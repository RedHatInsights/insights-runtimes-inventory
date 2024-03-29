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
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.hibernate.Session;

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

  @Inject Session session;

  private static HttpClient httpClient;

  private ArchiveAnnouncementParser jsonParser = new ArchiveAnnouncementParser();

  private Counter rejectedCounter;
  private Counter processingErrorCounter;
  private Counter duplicateCounter;
  private Counter processingExceptionCounter;

  private static Clock clock = Clock.systemDefaultZone();

  @PostConstruct
  public void init() {
    rejectedCounter = registry.counter(REJECTED_COUNTER_NAME);
    processingErrorCounter = registry.counter(PROCESSING_ERROR_COUNTER_NAME);
    processingExceptionCounter = registry.counter(PROCESSING_EXCEPTION_COUNTER_NAME);
    duplicateCounter = registry.counter(DUPLICATE_COUNTER_NAME);
    new ProcessorMetrics().bindTo(registry);
    new JvmMemoryMetrics().bindTo(registry);
  }

  @Incoming(INGRESS_CHANNEL)
  @Blocking
  @ActivateRequestContext
  @Transactional
  public CompletionStage<Void> processMainFlow(Message<String> message) {
    // This timer will have dynamic tag values based on the action parsed from the received message.
    Timer.Sample consumedTimer = Timer.start(registry);
    var payload = message.getPayload();

    try {
      Log.debugf("Processing received Kafka message %s", payload);

      // Parse JSON using Jackson
      var announce = jsonParser.fromJsonString(payload);
      if (announce.getContentType().equals(VALID_CONTENT_TYPE)) {

        // Get data back from S3
        Log.infof("Processed message URL: %s", announce.getUrl());
        var archiveJson = getJsonFromS3(announce.getUrl());
        Log.debugf("Retrieved from S3: %s", archiveJson);
        if (shouldProcessMessage(archiveJson, clock, false)) {
          processMessage(announce, archiveJson);
        }
      }

    } catch (Throwable t) {
      processingExceptionCounter.increment();
      Log.errorf(t, "Could not process the payload");
      Log.debugf(t, "payload: %s", payload);
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

    try {
      Log.debugf("Processing received Kafka message from egg %s", payload);

      // Parse JSON using Jackson
      var announce = jsonParser.fromJsonString(payload);
      if (VALID_CONTENT_TYPE.equals(announce.getContentType()) || announce.isRuntimes()) {
        var url = announce.getUrl();
        if (url != null) {
          // Get data back from S3
          Log.infof("Processed message URL: %s", url);
          var jsonFiles = getJsonsFromArchiveStream(getInputStreamFromS3(announce.getUrl()));
          Log.debugf("Found [%s] files in the S3 archive.", jsonFiles.size());
          for (String json : jsonFiles) {
            if (shouldProcessMessage(json, clock, true)) {
              processMessage(announce, json);
            }
          }
        }
      }
    } catch (Throwable t) {
      processingExceptionCounter.increment();
      Log.errorf(t, "Could not process the egg payload.");
      Log.debugf(t, "payload: %s", payload);
    } finally {
      // FIXME Might need tags
      consumedTimer.stop(registry.timer(CONSUMED_TIMER_NAME));
    }

    return message.ack();
  }

  @Transactional
  public void processMessage(ArchiveAnnouncement announce, String json) {
    // Needs to be visible in the catch block
    JvmInstance inst = null;
    try {
      InsightsMessage msg = instanceOf(announce, json);

      if (msg instanceof EapInstance) {
        inst = (EapInstance) msg;
      } else if (msg instanceof JvmInstance) {
        inst = (JvmInstance) msg;
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
        throw new IllegalStateException("Message seen that is neither a new instance or an update");
      }

      if (inst != null) {
        Log.debugf("About to persist: %s", inst);
        entityManager.persist(inst);
        entityManager.flush();
        session.evict(inst);
      }
    } catch (Throwable t) {
      processingExceptionCounter.increment();
      Log.errorf(t, "Could not process and/or persist the object.");
      Log.debugf(t, "The object: %s", inst);
    }
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

  public static List<String> getJsonsFromArchiveStream(InputStream archiveStream) {
    // The egg file comes in as a String, but it is actually a gzipped tarfile
    // So we will turn it into a stream, 'uncompress' the stream, then walk
    // the archive for files we care about.
    String insightsDataPath = "/data/var/tmp/insights-runtimes/uploads/";
    List<String> jsonFiles = new ArrayList<String>();

    try {
      GzipCompressorInputStream gzis = new GzipCompressorInputStream(archiveStream);
      TarArchiveInputStream tarInput = new TarArchiveInputStream(gzis);

      ArchiveEntry entry;
      while ((entry = tarInput.getNextEntry()) != null) {
        String entryName = entry.getName();

        // Skip any file not in our relevant path
        if (!entryName.contains(insightsDataPath)) {
          continue;
        }

        // Read in the file stream and turn it into a string for processing
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = tarInput.read(buffer)) != -1) {
          baos.write(buffer, 0, bytesRead);
        }
        String json = new String(baos.toByteArray());
        if (json == null || json.isEmpty()) {
          continue;
        }

        jsonFiles.add(json);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return jsonFiles;
  }

  static void setHttpClient(HttpClient httpClient) {
    EventConsumer.httpClient = httpClient;
  }

  static void setClock(Clock clock) {
    EventConsumer.clock = clock;
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

  static InputStream getInputStreamFromS3(String urlStr) {
    try {
      var uri = new URL(urlStr).toURI();
      var requestBuilder = HttpRequest.newBuilder().uri(uri);
      var request = requestBuilder.GET().build();
      Log.debugf("Issuing a HTTP POST request to %s", request);

      if (httpClient == null) {
        httpClient = HttpClient.newBuilder().build();
      }
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
      Log.debugf("S3 HTTP Client status: %s", response.statusCode());

      return response.body();
    } catch (URISyntaxException | IOException | InterruptedException e) {
      Log.error("Error in HTTP send: ", e);
      throw new RuntimeException(e);
    }
  }
}
