/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

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
import io.quarkus.logging.Log;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

public final class Utils {

  private Utils() {}

  @SuppressWarnings("unchecked")
  public static InsightsMessage instanceOf(ArchiveAnnouncement announce, String json) {
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

      // Is this an Eap Instance?
      var eap = (Map<String, Object>) o.get("eap");
      if (eap != null) {
        return eapInstanceOf(announce, json);
      }
      return jvmInstanceOf(announce, json);
    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }
  }
  /****************************************************************************
   *                             JVM Methods
   ***************************************************************************/
  @SuppressWarnings("unchecked")
  public static JvmInstance jvmInstanceOf(ArchiveAnnouncement announce, String json) {
    var inst = new JvmInstance();
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
        throw new RuntimeException(
            "Error in unmarshalling JSON - does not contain a basic or updated-jars tag");
      }

      mapJvmInstanceValues(inst, o, basic);
      inst.setJarHashes(jarHashesOf((Map<String, Object>) o.get("jars")));
      // Set workload
      var details = (Map<String, Object>) o.get("details");
      if (details != null) {
        var workload = details.get("workloadType");
        if (workload != null) {
          inst.setWorkload(String.valueOf(workload));
        }
        if (details.containsKey("is_ocp")) {
          inst.setOcp(Boolean.parseBoolean(details.get("is_ocp").toString()));
        } else {
          // FIXME This is a workaround - for the first release of the agent in Cryostat
          // we didn't have the is_ocp field. So we'll have to assume that if this payload
          // came from an agent and the field's not there, it's from OCP.
          // This should be fixed in the next release of the agent in Cryostat.
          inst.setOcp(true);
        }
      }
    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }

    inst.sanitize();
    return inst;
  }

  static void mapJvmInstanceValues(
      JvmInstance inst, Map<String, Object> o, Map<String, Object> basic) {

    try {
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

      inst.setHeapMin((int) Double.parseDouble(String.valueOf(basic.get("jvm.heap.min"))));
      inst.setHeapMax((int) Double.parseDouble(String.valueOf(basic.get("jvm.heap.max"))));
      inst.setLaunchTime(Long.parseLong(String.valueOf(basic.get("jvm.report_time"))));

      inst.setOsArch(String.valueOf(basic.get("system.arch")));
      inst.setProcessors(Integer.parseInt(String.valueOf(basic.get("system.cores.logical"))));
      inst.setHostname(String.valueOf(basic.get("system.hostname")));

      inst.setJavaClassPath(String.valueOf(basic.get("java.class.path")));
      inst.setJavaClassVersion(String.valueOf(basic.get("java.class.version")));
      inst.setJavaCommand(String.valueOf(basic.get("java.command")));
      inst.setJavaHome(String.valueOf(basic.get("java.home")));
      inst.setJavaLibraryPath(String.valueOf(basic.get("java.library.path")));
      inst.setJavaVendor(String.valueOf(basic.get("java.vendor")));
      inst.setJavaSpecificationVendor(String.valueOf(basic.get("java.specification.vendor")));
      inst.setJavaVendorVersion(String.valueOf(basic.get("java.vendor.version")));
      inst.setJavaVmName(String.valueOf(basic.get("java.vm.name")));
      inst.setJavaVmVendor(String.valueOf(basic.get("java.vm.vendor")));
      inst.setJvmHeapGcDetails(String.valueOf(basic.get("jvm.heap.gc.details")));
      inst.setJvmPid(String.valueOf(basic.get("jvm.pid")));
      inst.setJvmReportTime(String.valueOf(basic.get("jvm.report_time")));
      inst.setJvmPackages(String.valueOf(basic.get("jvm.packages")));
      inst.setJvmArgs(String.valueOf(basic.get("jvm.args")));
      inst.setSystemOsName(String.valueOf(basic.get("system.os.name")));
      inst.setSystemOsVersion(String.valueOf(basic.get("system.os.version")));

      inst.setDetails(basic);
    } catch (ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }
  }

  @SuppressWarnings("unchecked")
  static UpdateInstance updatedInstanceOf(Map<String, Object> updatedJars) {
    var linkingHash = (String) updatedJars.get("idHash");
    var jarsJson = (List<Map<String, Object>>) updatedJars.get("jars");

    var jars = new ArrayList<JarHash>();
    jarsJson.forEach(j -> jars.add(jarHashOf((Map<String, Object>) j)));
    return new UpdateInstance(linkingHash, jars);
  }

  @SuppressWarnings("unchecked")
  static Set<JarHash> jarHashesOf(Map<String, Object> jarsRep) {
    if (jarsRep == null) {
      return Set.of();
    }
    var jars = (List<Object>) jarsRep.get("jars");
    if (jars == null) {
      return Set.of();
    }
    var out = new HashSet<JarHash>();
    jars.forEach(j -> out.add(jarHashOf((Map<String, Object>) j)));

    return out;
  }

  @SuppressWarnings("unchecked")
  public static JarHash jarHashOf(Map<String, Object> jarJson) {
    var out = new JarHash();
    out.setName((String) jarJson.getOrDefault("name", ""));
    out.setVersion((String) jarJson.getOrDefault("version", ""));

    var attrs = (Map<String, String>) jarJson.getOrDefault("attributes", Map.of());
    out.setGroupId(attrs.getOrDefault("groupId", ""));
    out.setVendor(attrs.getOrDefault("Implementation-Vendor", ""));
    out.setSha1Checksum(attrs.getOrDefault("sha1Checksum", ""));
    out.setSha256Checksum(attrs.getOrDefault("sha256Checksum", ""));
    out.setSha512Checksum(attrs.getOrDefault("sha512Checksum", ""));

    return out;
  }

  /****************************************************************************
   *                             EAP Methods
   ***************************************************************************/
  @SuppressWarnings("unchecked")
  public static EapInstance eapInstanceOf(ArchiveAnnouncement announce, String json) {
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
      var basic = (Map<String, Object>) o.get("basic");
      mapJvmInstanceValues(inst, o, basic);

      // Map our 'basic' fields
      inst.setAppClientException(String.valueOf(basic.get("app.client.exception")));
      inst.setAppName(String.valueOf(basic.get("app.name")));
      inst.setAppTransportCertHttps(String.valueOf(basic.get("app.transport.cert.https")));
      inst.setAppTransportTypeFile(String.valueOf(basic.get("app.transport.type.file")));
      inst.setAppTransportTypeHttps(String.valueOf(basic.get("app.transport.type.https")));
      inst.setAppUserDir(String.valueOf(basic.get("app.user.dir")));
      inst.setAppUserName(String.valueOf(basic.get("app.user.name")));
      inst.setWorkload("EAP");
      if (basic.containsKey("is_ocp")) {
        inst.setOcp(Boolean.parseBoolean(String.valueOf(basic.get("is_ocp"))));
      } else {
        inst.setOcp(false);
      }

      // Jar hashes...
      inst.setJarHashes(jarHashesOf((Map<String, Object>) o.get("jars")));

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

      inst.setModules(jarHashesOf((Map<String, Object>) eapRep.get("eap-modules")));
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

    inst.sanitize();
    return inst;
  }

  @SuppressWarnings("unchecked")
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
      } else {
        var archives = new HashSet<JarHash>();
        arcRep.forEach(j -> archives.add(jarHashOf((Map<String, Object>) j)));
        dep.setArchives(archives);
      }
      out.add(dep);
    }

    return out;
  }

  @SuppressWarnings("unchecked")
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
      extension.setSubsystems(subsystems);
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
      Map<String, String> subsystems = new HashMap<>();
      Map<String, Object> subsystemRep = (Map<String, Object>) configRep.get("subsystem");
      for (Map.Entry<String, Object> entry : subsystemRep.entrySet()) {
        subsystems.put(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
      }
      config.setSubsystems(subsystems);

      // Config Deployments parsing
      Map<String, String> deployments = new HashMap<>();
      Map<String, Object> deploymentRep = (Map<String, Object>) configRep.get("deployment");
      if (deploymentRep != null) {
        for (Map.Entry<String, Object> entry : deploymentRep.entrySet()) {
          deployments.put(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
        }
      }
      config.setDeployments(deployments);

    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }

    return config;
  }

  /****************************************************************************
   *                             Utility Methods
   ***************************************************************************/
  // Given a message, should we process it and persist it?
  @SuppressWarnings("unchecked")
  public static boolean shouldProcessMessage(String json, Clock clock, boolean isEgg) {
    TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};

    var mapper = new ObjectMapper();
    try {
      var o = mapper.readValue(json, typeRef);
      var basic = (Map<String, Object>) o.get("basic");
      // This might be an update. If so, things are fine
      if (basic == null) {
        var updatedJars = (Map<String, Object>) o.get("updated-jars");
        // TODO We're ignoring updates from egg files
        //      This is because of lack of date/time context in them
        //      Maybe we shouldn't be? Maybe they shouldn't be sending them?
        if (updatedJars == null || isEgg) {
          return false;
        }
        return true;
      }

      // We should check the timestamp for statements from the last 24 hours.
      LocalDate yesterday = LocalDate.now(clock).minusDays(1);
      LocalDate messageTime =
          Instant.ofEpochMilli(Long.parseLong(String.valueOf(basic.get("jvm.report_time"))))
              .atZone(ZoneId.systemDefault())
              .toLocalDate();
      if (messageTime.isBefore(yesterday)) {
        Log.infof(
            "Ignoring message because its date [%s] is older than our cutoff [%s]",
            messageTime, yesterday);
        return false;
      }
    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }
    return true;
  }
}
