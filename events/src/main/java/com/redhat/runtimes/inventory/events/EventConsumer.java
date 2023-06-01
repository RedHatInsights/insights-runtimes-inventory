/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy.PRE_PROCESSING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.runtimes.inventory.models.InsightsMessage;
import com.redhat.runtimes.inventory.models.JarHash;
import com.redhat.runtimes.inventory.models.RuntimesInstance;
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
import java.time.ZoneOffset;
import java.util.*;
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

        var msg = runtimesInstanceOf(announce, archiveJson);
        // This should be a true pattern match on type
        if (msg instanceof RuntimesInstance) {
          inst = (RuntimesInstance) msg;
        } else if (msg instanceof UpdateInstance update) {
          var linkingHash = update.getLinkingHash();
          var maybeInst = getInstanceFromHash(linkingHash);
          if (maybeInst.isPresent()) {
            inst = maybeInst.get();
            var newJars = update.getUpdates();
            for (var jh : newJars) {
              jh.setInstance(inst);
            }
            inst.getJarHashes().addAll(newJars);
          } else {
            throw new IllegalStateException(
                "Update message seen for non-existant hash: " + linkingHash);
          }
        } else {
          // Can't happen, but just in case
          throw new IllegalStateException(
              "Message seen that is neither a new instance or an update");
        }
      }

      Log.infof("About to persist: %s", inst);
      entityManager.persist(inst);
    } catch (Throwable t) {
      processingExceptionCounter.increment();
      Log.errorf(t, "Could not process the payload: %s", inst);
    } finally {
      // FIXME Might need tags
      consumedTimer.stop(registry.timer(CONSUMED_TIMER_NAME));
    }

    return message.ack();
  }

  Optional<RuntimesInstance> getInstanceFromHash(String linkingHash) {
    List<RuntimesInstance> instances =
        entityManager
            .createQuery("SELECT ri from RuntimesInstance ri where ri.linkingHash = ?1")
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

  static InsightsMessage runtimesInstanceOf(ArchiveAnnouncement announce, String json) {
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
        var updatedJars = (Map<String, Object>) o.get("updated-jars");
        if (updatedJars != null) {
          return updatedInstanceOf(updatedJars);
        }
        throw new RuntimeException(
            "Error in unmarshalling JSON - does not contain a basic or updated-jars tag");
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

      inst.setJarHashes(jarHashesOf(inst, json));
    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }

    return inst;
  }

  static UpdateInstance updatedInstanceOf(Map<String, Object> updatedJars) {
    var linkingHash = (String) updatedJars.get("idHash");
    var jarsJson = (List<Map<String, Object>>) updatedJars.get("jars");

    var jars = new ArrayList<JarHash>();
    jarsJson.forEach(j -> jars.add(jarHashOf(null, j)));
    return new UpdateInstance(linkingHash, jars);
  }

  static Set<JarHash> jarHashesOf(RuntimesInstance inst, String json) {
    TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};

    var mapper = new ObjectMapper();
    try {
      var o = mapper.readValue(json, typeRef);
      var jarsRep = (Map<String, Object>) o.get("jars");
      if (jarsRep == null) {
        return Set.of();
      }
      var jars = (List<Object>) jarsRep.get("jars");
      if (jars == null) {
        return Set.of();
      }
      var out = new HashSet<JarHash>();
      jars.forEach(j -> out.add(jarHashOf(inst, (Map<String, Object>) j)));

      var eapRep = (Map<String, Object>) o.get("eap");
      if (eapRep != null) {
        // FIXME Do EAP-specific processing
        // Log.infof("EAP-specific processing required");
      }
      return out;
    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON for jars", e);
      throw new RuntimeException("Error in unmarshalling JSON for jars", e);
    }
  }

  static JarHash jarHashOf(RuntimesInstance inst, Map<String, Object> jarJson) {
    var out = new JarHash();
    out.setInstance(inst);
    out.setName((String) jarJson.getOrDefault("name", ""));
    out.setVersion((String) jarJson.getOrDefault("version", ""));

    var attrs = (Map<String, String>) jarJson.getOrDefault("attributes", Map.of());
    out.setGroupId(attrs.getOrDefault("group_id", ""));
    out.setVendor(attrs.getOrDefault("vendor", ""));
    out.setSha1Checksum(attrs.getOrDefault("sha1_checksum", ""));
    out.setSha256Checksum(attrs.getOrDefault("sha256_checksum", ""));
    out.setSha512Checksum(attrs.getOrDefault("sha512_checksum", ""));

    return out;
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
