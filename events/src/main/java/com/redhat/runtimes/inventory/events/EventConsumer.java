/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy.PRE_PROCESSING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.runtimes.inventory.models.RuntimesInstance;
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
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.zip.GZIPInputStream;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
public class EventConsumer {
  private static final String INGRESS_CHANNEL = "ingress";
  private static final String REJECTED_COUNTER_NAME = "input.rejected";
  private static final String PROCESSING_ERROR_COUNTER_NAME = "input.processing.error";
  private static final String PROCESSING_EXCEPTION_COUNTER_NAME = "input.processing.exception";
  private static final String DUPLICATE_COUNTER_NAME = "input.duplicate";
  private static final String CONSUMED_TIMER_NAME = "input.consumed";

  static final String VALID_CONTENT_TYPE =
      "application/vnd.redhat.runtimes-java-general.analytics+tgz";

  private static final String EVENT_TYPE_NOT_FOUND_MSG =
      "No event type found for [bundleName=%s, applicationName=%s, eventTypeName=%s]";

  @Inject MeterRegistry registry;

  // TODO Remove?
  @Inject KafkaMessageDeduplicator kafkaMessageDeduplicator;

  @Inject EntityManager entityManager;

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
  @Acknowledgment(PRE_PROCESSING)
  @Blocking
  @ActivateRequestContext
  @Transactional
  public CompletionStage<Void> process(Message<String> message) {
    // This timer will have dynamic tag values based on the action parsed from the received message.
    Timer.Sample consumedTimer = Timer.start(registry);
    var payload = message.getPayload();

    RuntimesInstance inst = null;
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

        inst = runtimesInstance(announce, archiveJson);

        // Persist core data
        Log.infof("About to persist: %s", inst);
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

  static RuntimesInstance runtimesInstance(ArchiveAnnouncement announce, String json) {
    var inst = new RuntimesInstance();
    // Announce fields first
    inst.setAccountId(announce.getAccountId());
    inst.setOrgId(announce.getOrgId());
    inst.setCreated(announce.getTimestamp().atZone(ZoneOffset.UTC));

    TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};

    var mapper = new ObjectMapper();
    try {
      var o = mapper.readValue(json, typeRef);
      var basic = (Map<String, Object>) o.get("basic");
      if (basic == null) {
        throw new RuntimeException("Error in unmarshalling JSON - does not contain a basic tag");
      }
      inst.setLinkingHash((String) o.get("idHash"));

      inst.setVersionString(String.valueOf(basic.get("java.runtime.version")));
      inst.setVersion(String.valueOf(basic.get("java.version")));
      inst.setVendor(String.valueOf(basic.get("java.vm.specification.vendor")));

      var strVersion = String.valueOf(basic.get("java.vm.specification.version"));
      // Handle Java 8
      if (strVersion.startsWith("1.")) {
        strVersion = strVersion.substring(2);
      }
      inst.setMajorVersion(Integer.parseInt(strVersion));

      // FIXME Add heap min
      inst.setHeapMax((int) Double.parseDouble(String.valueOf(basic.get("jvm.heap.max"))));
      inst.setLaunchTime(Long.parseLong(String.valueOf(basic.get("jvm.report_time"))));

      inst.setOsArch(String.valueOf(basic.get("system.arch")));
      inst.setProcessors(Integer.parseInt(String.valueOf(basic.get("system.cores.logical"))));
      inst.setHostname(String.valueOf(basic.get("system.hostname")));

      inst.setDetails(basic);
    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }

    return inst;
  }

  static String unzipJson(byte[] buffy) {
    try (var bais = new ByteArrayInputStream(buffy);
        var gunzip = new GZIPInputStream(bais)) {
      return new String(gunzip.readAllBytes());
    } catch (IOException e) {
      Log.error("Error in Unzipping archive: ", e);
      throw new RuntimeException(e);
    }
  }

  static String getJsonFromS3(String urlStr) {
    try {
      var uri = new URL(urlStr).toURI();
      var requestBuilder = HttpRequest.newBuilder().uri(uri);
      var request = requestBuilder.GET().build();
      Log.debugf("Issuing a HTTP POST request to %s", request);

      var client = HttpClient.newBuilder().build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      Log.debugf("S3 HTTP Client status: %s", response.statusCode());

      return unzipJson(response.body());
    } catch (URISyntaxException | IOException | InterruptedException e) {
      Log.error("Error in HTTP send: ", e);
      throw new RuntimeException(e);
    }
  }
}
