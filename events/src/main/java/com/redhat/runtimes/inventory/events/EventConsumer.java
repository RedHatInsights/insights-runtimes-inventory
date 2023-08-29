/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static com.redhat.runtimes.inventory.events.Utils.*;
import static org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy.PRE_PROCESSING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.runtimes.inventory.models.EapConfiguration;
import com.redhat.runtimes.inventory.models.EapDeployment;
import com.redhat.runtimes.inventory.models.EapExtension;
import com.redhat.runtimes.inventory.models.EapInstance;
import com.redhat.runtimes.inventory.models.InsightsMessage;
import com.redhat.runtimes.inventory.models.JarHash;
import com.redhat.runtimes.inventory.models.JvmInstance;
import com.redhat.runtimes.inventory.models.NameVersionPair;
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
  public static final String INGRESS_CHANNEL = "ingress";
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
  @Acknowledgment(PRE_PROCESSING)
  @Blocking
  @ActivateRequestContext
  @Transactional
  public CompletionStage<Void> process(Message<String> message) {
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

        var msg = jvmInstanceOf(announce, archiveJson);
        // This should be a true pattern match on type
        if (msg instanceof JvmInstance) {
          inst = (JvmInstance) msg;
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
   *                         Runtimes Methods
   ***************************************************************************/

  /****************************************************************************
   *                             EAP Methods
   ***************************************************************************/

  static InsightsMessage eapInstanceOf(ArchiveAnnouncement announce, String json) {
    var inst = new EapInstance();
    inst.setRaw(json);
    // Announce fields first
    inst.setAccountId(announce.getAccountId());
    inst.setOrgId(announce.getOrgId());
    inst.setCreated(announce.getTimestamp().atZone(ZoneOffset.UTC));

    TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};

    var mapper = new ObjectMapper();
    try {
      var o = mapper.readValue(json, typeRef);
      mapJvmInstanceValues(inst, o, (Map<String, Object>) o.get("basic"));
      inst.setJarHashes(jarHashesOf(inst, (Map<String, Object>) o.get("jars")));

      var eapRep = (Map<String, Object>) o.get("eap");
      if (eapRep == null) {
        throw new RuntimeException(
            "Error in unmarshalling JSON - is an EapInstance without an eap definition.");
      }
      inst.setEapVersion(String.valueOf(eapRep.get("eap-version")));
      var eapRepInstall = (Map<String, Object>) eapRep.get("eap-installation");
      if (eapRepInstall != null) {
        // TODO: I don't like this [boolean of string of] stuff.
        //       Figure out a better way to do that.
        inst.setEapXp(Boolean.valueOf(String.valueOf(eapRepInstall.get("eap-xp"))));
        inst.setEapYamlExtension(
            Boolean.valueOf(String.valueOf(eapRepInstall.get("yaml-extension"))));
        inst.setEapBootableJar(Boolean.valueOf(String.valueOf(eapRepInstall.get("bootable-jar"))));
        inst.setEapUseGit(Boolean.valueOf(String.valueOf(eapRepInstall.get("use-git"))));
      }

      var modRep = (Map<String, Object>) eapRep.get("eap-modules");
      inst.setModules(jarHashesOf(inst, modRep));
      var configRep = (Map<String, Object>) eapRep.get("eap-configuration");
      inst.setConfiguration(eapConfigurationOf(inst, configRep));
      var eapDepRep = (Map<String, Object>) eapRep.get("eap-deployments");
      var depRep = (List<Map<String, Object>>) eapDepRep.get("deployments");
      inst.setDeployments(eapDeploymentsOf(inst, depRep));

      // System.out.println(mapper.writeValueAsString(inst));
    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }

    return inst;
  }

  static Set<EapDeployment> eapDeploymentsOf(EapInstance inst, List<Map<String, Object>> depRep) {
    if (depRep == null) {
      return Set.of();
    }
    var out = new HashSet<EapDeployment>();
    for (var deployment : depRep) {
      EapDeployment dep = new EapDeployment();
      dep.setEapInstance(inst);
      dep.setName(String.valueOf(deployment.get("name")));
      var arcRep = (List<Object>) deployment.get("archives");
      if (arcRep == null) {
        dep.setArchives(Set.of());
      }
      var archives = new HashSet<JarHash>();
      arcRep.forEach(j -> archives.add(jarHashOf(inst, (Map<String, Object>) j)));
      dep.setArchives(archives);
      out.add(dep);
    }

    return out;
  }

  static EapConfiguration eapConfigurationOf(EapInstance inst, Map<String, Object> eapConfigRep) {
    if (eapConfigRep == null) {
      throw new RuntimeException(
          "Error in unmarshalling JSON - is an EapInstance without an eap-configuration.");
    }
    var mapper = new ObjectMapper();
    var config = new EapConfiguration();
    config.setEapInstance(inst);
    config.setVersion(String.valueOf(eapConfigRep.get("version")));

    var configRep = (Map<String, Object>) eapConfigRep.get("configuration");
    config.setLaunchType(String.valueOf(configRep.get("launch-type")));
    config.setName(String.valueOf(configRep.get("name")));
    config.setOrganization(String.valueOf(configRep.get("organization")));
    config.setProcessType(String.valueOf(configRep.get("process-type")));
    config.setProductName(String.valueOf(configRep.get("product-name")));
    config.setProductVersion(String.valueOf(configRep.get("product-version")));
    config.setProfileName(String.valueOf(configRep.get("profile-name")));
    config.setReleaseCodename(String.valueOf(configRep.get("release-codename")));
    config.setReleaseVersion(String.valueOf(configRep.get("release-version")));
    config.setRunningMode(String.valueOf(configRep.get("running-mode")));
    config.setRuntimeConfigurationState(
        String.valueOf(configRep.get("runtime-configuration-state")));
    config.setServerState(String.valueOf(configRep.get("server-state")));
    config.setSuspendState(String.valueOf(configRep.get("suspend-state")));

    // Extension Parsing
    Set<EapExtension> extensions = new HashSet<EapExtension>();
    var extensionsRep = (Map<String, Map<String, Object>>) configRep.get("extension");
    for (Map<String, Object> extRep : extensionsRep.values()) {
      // Looks like:
      // { "module"    : "...",
      //   "subsystem" : { ... } }
      EapExtension extension = new EapExtension();
      extension.setModule(String.valueOf(extRep.get("module")));
      Set<NameVersionPair> subsystems = new HashSet<NameVersionPair>();
      var subRep = (Map<String, Map<String, Integer>>) extRep.get("subsystem");
      for (Map.Entry<String, Map<String, Integer>> subEntry : subRep.entrySet()) {
        // Looks like:
        // { "sub_name"  : { ... },
        //   "sub_name2" : { ... },
        //   ... }
        NameVersionPair subsystem = new NameVersionPair();
        subsystem.setName(subEntry.getKey());
        Map<String, Integer> versions = subEntry.getValue();
        // Looks like:
        // { "management-{major,minor,micro}-version" : <num> }
        String version = String.valueOf(versions.get("management-major-version"));
        version += "." + String.valueOf(versions.get("management-minor-version"));
        version += "." + String.valueOf(versions.get("management-micro-version"));
        subsystem.setVersion(version);
        subsystems.add(subsystem);
      }
      extensions.add(extension);
    }
    config.setExtensions(extensions);

    // JSON Dumps begin here
    try {
      config.setSocketBindingGroups(
          mapper.writeValueAsString(configRep.get("socket-binding-group")));
      config.setPaths(mapper.writeValueAsString(configRep.get("path")));
      config.setInterfaces(mapper.writeValueAsString(configRep.get("interface")));
      config.setCoreServices(mapper.writeValueAsString(configRep.get("core-service")));

      // Subsystem parsing
      Map<String, String> subsystems = new HashMap<String, String>();
      Map<String, Object> subsystemRep = (Map<String, Object>) configRep.get("subsystem");
      for (Map.Entry<String, Object> entry : subsystemRep.entrySet()) {
        subsystems.put(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
      }
      config.setSubsystems(subsystems);

      // Config Deployments parsing
      Map<String, String> deployments = new HashMap<String, String>();
      Map<String, Object> deploymentRep = (Map<String, Object>) configRep.get("deployment");
      for (Map.Entry<String, Object> entry : deploymentRep.entrySet()) {
        deployments.put(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
      }
      config.setDeployments(deployments);

    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }

    return config;
  }

  /****************************************************************************
   *                           Utility Methods
   ***************************************************************************/

  static String unzipJson(byte[] buffy) {
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
