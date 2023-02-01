package com.redhat.runtimes.inventory.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.runtimes.inventory.models.RuntimesInstance;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.zip.GZIPInputStream;

import static org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy.PRE_PROCESSING;

@ApplicationScoped
public class EventConsumer {
  private static final String INGRESS_CHANNEL = "ingress";
  private static final String REJECTED_COUNTER_NAME = "input.rejected";
  private static final String PROCESSING_ERROR_COUNTER_NAME = "input.processing.error";
  private static final String PROCESSING_EXCEPTION_COUNTER_NAME = "input.processing.exception";
  private static final String DUPLICATE_COUNTER_NAME = "input.duplicate";
  private static final String CONSUMED_TIMER_NAME = "input.consumed";

  public static final String X_RH_IDENTITY_HEADER = "x-rh-identity";

  private static final String EVENT_TYPE_NOT_FOUND_MSG = "No event type found for [bundleName=%s, applicationName=%s, eventTypeName=%s]";

  @Inject
  MeterRegistry registry;

  // TODO
  @Inject
  KafkaMessageDeduplicator kafkaMessageDeduplicator;

  @Inject
  EntityManager entityManager;

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
  public CompletionStage<Void> process(Message<String> message) {
    // This timer will have dynamic tag values based on the action parsed from the received message.
    Timer.Sample consumedTimer = Timer.start(registry);
    var payload = message.getPayload();
    Log.info("Processing received Kafka message: "+ payload);

    // Parse JSON using Jackson
    var announce = jsonParser.fromJsonString(payload);
    Log.debug("Processed message URL: "+ announce.getUrl());
//    Log.info("Processed Org ID: "+ announce.getOrgId());

    // Get data back from S3
    var archiveJson = getJsonFromS3(announce.getUrl());
    Log.info("Retrieved from S3: "+ archiveJson);

    var inst = runtimesInstance(announce, archiveJson);

    // TODO Do we need UUIDs?
    // Persist core data
    Log.info("About to persist: "+ inst);
    entityManager.persist(inst);

    // FIXME Might need tags
    consumedTimer.stop(registry.timer(CONSUMED_TIMER_NAME));
    return message.ack();
  }

  static RuntimesInstance runtimesInstance(ArchiveAnnouncement announce, String json) {
    // Find hostname - will use as a lookup key in DB
    var inst = new RuntimesInstance();
    inst.setAccountId(announce.getAccountId());
    inst.setOrgId(announce.getOrgId());

    TypeReference<Map<String,Object>> typeRef = new TypeReference<>() {};

    var mapper = new ObjectMapper();
    try {
      var o = mapper.readValue(json, typeRef);
      var basic = (Map<String, Object>)o.get("basic");
      inst.setHostname(String.valueOf(basic.get("hostname")));
      inst.setVendor(String.valueOf(basic.get("java.vm.specification.vendor")));
      inst.setVersionString(String.valueOf(basic.get("java.runtime.version")));
      inst.setVersion(String.valueOf(basic.get("java.version")));
      inst.setMajorVersion(Integer.parseInt(String.valueOf(basic.get("java.vm.specification.version"))));
      inst.setOsArch(String.valueOf(basic.get("os.arch")));

      // FIXME Hardcoded for now
      inst.setProcessors(12);
      inst.setHeapMax(8192);
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
      var requestBuilder =
        HttpRequest.newBuilder().uri(uri);
      var request = requestBuilder.GET().build();
      Log.info("Issuing a HTTP POST request to " + request);

      var client = HttpClient.newBuilder().build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      Log.info(
        "S3 HTTP Client: status="+ response.statusCode());

      return unzipJson(response.body());
    } catch (URISyntaxException | IOException | InterruptedException e) {
      Log.error("Error in HTTP send: ", e);
      throw new RuntimeException(e);
    }
  }

}
