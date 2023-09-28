/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static com.redhat.runtimes.inventory.events.Utils.*;

import com.redhat.runtimes.inventory.models.EapInstance;
import com.redhat.runtimes.inventory.models.InsightsMessage;
import com.redhat.runtimes.inventory.models.JvmInstance;
import com.redhat.runtimes.inventory.models.UpdateInstance;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.zip.GZIPInputStream;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
public class EventConsumer {
  public static final String INGRESS_CHANNEL = "ingress";
  public static final String EGG_CHANNEL = "egg";
  public static final String REJECTED_COUNTER_NAME = "input.rejected";
  public static final String PROCESSING_ERROR_COUNTER_NAME = "input.processing.error";
  public static final String PROCESSING_EXCEPTION_COUNTER_NAME = "input.processing.exception";
  public static final String DUPLICATE_COUNTER_NAME = "input.duplicate";
  public static final String CONSUMED_TIMER_NAME = "input.consumed";

  static final String VALID_CONTENT_TYPE =
      "application/vnd.redhat.runtimes-java-general.analytics+tgz";

  private static final String EVENT_TYPE_NOT_FOUND_MSG =
      "No event type found for [bundleName=%s, applicationName=%s, eventTypeName=%s]";

  @Inject MeterRegistry registry;

  @Inject EntityManager entityManager;

  private static HttpClient httpClient;

  private ArchiveAnnouncementParser jsonParser = new ArchiveAnnouncementParser();

  private Counter rejectedCounter;
  private Counter processingErrorCounter;
  private Counter duplicateCounter;
  private Counter processingExceptionCounter;

  @PostConstruct
  public void init() {
    rejectedCounter = registry.counter(REJECTED_COUNTER_NAME);
    processingErrorCounter = registry.counter(PROCESSING_ERROR_COUNTER_NAME);
    processingExceptionCounter = registry.counter(PROCESSING_EXCEPTION_COUNTER_NAME);
    duplicateCounter = registry.counter(DUPLICATE_COUNTER_NAME);
  }

  @Incoming(INGRESS_CHANNEL)
  @Blocking
  @ActivateRequestContext
  @Transactional
  public CompletionStage<Void> processMainFlow(Message<String> message) {
    // This timer will have dynamic tag values based on the action parsed from the received message.
    Timer.Sample consumedTimer = Timer.start(registry);
    var payload = message.getPayload();

    // Needs to be visible in the catch block
    JvmInstance inst = null;
    try {
      Log.debugf("Processing received Kafka message %s", payload);

      // Parse JSON using Jackson
      var announce = jsonParser.fromJsonString(payload);
      if (announce.getContentType().equals(VALID_CONTENT_TYPE)) {
        Log.infof("Processing our Kafka message %s", payload);

        // Get data back from S3
        Log.debugf("Processed message URL: %s", announce.getUrl());
        var archiveJson = getJsonFromS3(announce.getUrl());
        Log.debugf("Retrieved from S3: %s", archiveJson);

        InsightsMessage msg = instanceOf(announce, archiveJson);
        // This should be a true pattern match on type
        if (msg instanceof JvmInstance) {
          inst = (JvmInstance) msg;
        } else if (msg instanceof EapInstance) {
          inst = (EapInstance) msg;
        } else if (msg instanceof UpdateInstance update) {
          var linkingHash = update.getLinkingHash();
          var maybeInst = getInstanceFromHash(linkingHash);
          if (maybeInst.isPresent()) {
            inst = maybeInst.get();
            var newJars = update.getUpdates();
            inst.getJarHashes().addAll(newJars);
          } else {
            throw new IllegalStateException(
                "Update message seen for non-existent hash: " + linkingHash);
          }
        } else {
          // Can't happen, but just in case
          throw new IllegalStateException(
              "Message seen that is neither a new instance or an update");
        }
      }

      if (inst != null) {
        Log.debugf("About to persist: %s", inst);
        entityManager.persist(inst);
      }
    } catch (Throwable t) {
      processingExceptionCounter.increment();
      Log.errorf(t, "Could not process the payload: %s", inst);
    } finally {
      // FIXME Might need tags
      consumedTimer.stop(registry.timer(CONSUMED_TIMER_NAME));
    }

    return message.ack();
  }

  @Incoming(EGG_CHANNEL)
  @Blocking
  @ActivateRequestContext
  @Transactional
  public CompletionStage<Void> processEggFlow(Message<String> message) {
    // This timer will have dynamic tag values based on the action parsed from the received message.
    Timer.Sample consumedTimer = Timer.start(registry);
    var payload = message.getPayload();

    // Needs to be visible in the catch block
    JvmInstance inst = null;
    try {
      Log.debugf("Processing received Kafka message %s", payload);

      // Parse JSON using Jackson
      var announce = jsonParser.fromJsonString(payload);
      // FIXME
      if (VALID_CONTENT_TYPE.equals(announce.getContentType()) || announce.isRuntimes()) {
        Log.infof("Processing our Kafka message from egg %s", payload);

        var url = announce.getUrl();
        // FIXME I think that we'll need to do more JSON spelunking to get the S3 URL
        if (url != null) {
          // Get data back from S3
          Log.debugf("Processed message URL: %s", url);
          var archiveJson = getJsonFromS3(announce.getUrl());
          Log.debugf("Retrieved from S3: %s", archiveJson);

          var msg = instanceOf(announce, archiveJson);
          // The egg topic does not deliver update events, so this
          if (msg instanceof JvmInstance) {
            inst = (JvmInstance) msg;
            Log.infof("About to persist (from egg): %s", inst);
            entityManager.persist(inst);
          }
        }
      }
    } catch (Throwable t) {
      processingExceptionCounter.increment();
      Log.errorf(t, "Could not process the payload: %s", inst);
    } finally {
      // FIXME Might need tags
      consumedTimer.stop(registry.timer(CONSUMED_TIMER_NAME));
    }

    return message.ack();
  }

  Optional<JvmInstance> getInstanceFromHash(String linkingHash) {
    List<JvmInstance> instances =
        entityManager
            .createQuery("SELECT ri from JvmInstance ri where ri.linkingHash = ?1")
            .setParameter(1, linkingHash)
            .getResultList();
    if (instances.size() > 1) {
      throw new IllegalStateException(
          "Multiple instances found matching linking hash: " + linkingHash);
    } else if (instances.size() == 0) {
      return Optional.empty();
    }
    return Optional.of(instances.get(0));
  }

  /****************************************************************************
   *                           Utility Methods
   ***************************************************************************/

  public static String unzipJson(byte[] buffy) {
    try (var bais = new ByteArrayInputStream(buffy);
        var gunzip = new GZIPInputStream(bais)) {
      return new String(gunzip.readAllBytes());
    } catch (IOException e) {
      Log.error("Error in Unzipping archive: ", e);
      throw new RuntimeException(e);
    }
  }

  static void setHttpClient(HttpClient httpClient) {
    EventConsumer.httpClient = httpClient;
  }

  static String getJsonFromS3(String urlStr) {
    try {
      var uri = new URL(urlStr).toURI();
      var requestBuilder = HttpRequest.newBuilder().uri(uri);
      var request = requestBuilder.GET().build();
      Log.debugf("Issuing a HTTP POST request to %s", request);

      if (httpClient == null) {
        httpClient = HttpClient.newBuilder().build();
      }
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
      Log.debugf("S3 HTTP Client status: %s", response.statusCode());

      return unzipJson(response.body());
    } catch (URISyntaxException | IOException | InterruptedException e) {
      Log.error("Error in HTTP send: ", e);
      throw new RuntimeException(e);
    }
  }
}
