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
    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }

    sanitizeInstance(inst);
    return inst;
  }

  static void mapJvmInstanceValues(
      JvmInstance inst, Map<String, Object> o, Map<String, Object> basic) {

    try {
      // if (basic == null) {
      //   var updatedJars = (Map<String, Object>) o.get("updated-jars");
      //   if (updatedJars != null) {
      //     return updatedInstanceOf(updatedJars);
      //   }
      //   throw new RuntimeException(
      //       "Error in unmarshalling JSON - does not contain a basic or updated-jars tag");
      // }
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

  static UpdateInstance updatedInstanceOf(Map<String, Object> updatedJars) {
    var linkingHash = (String) updatedJars.get("idHash");
    var jarsJson = (List<Map<String, Object>>) updatedJars.get("jars");

    var jars = new ArrayList<JarHash>();
    jarsJson.forEach(j -> jars.add(jarHashOf((Map<String, Object>) j)));
    return new UpdateInstance(linkingHash, jars);
  }

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

    sanitizeInstance(inst);
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
      // arcRep.forEach(j -> archives.add(jarHashOf(inst, (Map<String, Object>) j)));
      arcRep.forEach(j -> archives.add(jarHashOf((Map<String, Object>) j)));
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
      Map<String, String> subsystems = new HashMap<String, String>();
      Map<String, Object> subsystemRep = (Map<String, Object>) configRep.get("subsystem");
      for (Map.Entry<String, Object> entry : subsystemRep.entrySet()) {
        subsystems.put(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
      }
      config.setSubsystems(subsystems);

      // Config Deployments parsing
      Map<String, String> deployments = new HashMap<String, String>();
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
          Instant.ofEpochMilli(Long.valueOf(String.valueOf(basic.get("jvm.report_time"))))
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

  // Clean given fields of data we don't want to persist
  public static void sanitizeInstance(InsightsMessage msg) {
    JvmInstance inst = null;

    if (msg instanceof JvmInstance) {
      inst = (JvmInstance) msg;
    } else if (msg instanceof EapInstance) {
      inst = (EapInstance) msg;
    } else {
      // We don't care, ignore and move on.
      return;
    }

    if (inst == null) {
      Log.debugf("Not sanitizing a Null Instance...");
      return;
    }

    inst.setJvmArgs(sanitizeJavaParameters(inst.getJvmArgs()));
    inst.setJavaCommand(sanitizeJavaParameters(inst.getJavaCommand()));
  }

  // This will sanitize strings that contain java style parameters
  // of the type -Dxxxxx=yyyyy by substituting the yyyyy part.
  public static String sanitizeJavaParameters(String in) {
    StringBuilder out = new StringBuilder();
    String redacted = "=*****"; // What to replace sanitized content with

    for (String token : tokenizeString(in)) {
      // We only care about -Dxxxxx=yyyyy params
      if (token.startsWith("-D") && token.contains("=")) {
        String[] parts = token.split("=", 2);
        out.append(parts[0]);
        out.append(redacted);
        // We might be parsing json
        // if so, preserve the list comma or list closing bracket
        if (token.endsWith(",")) {
          out.append(',');
        }
        if (token.endsWith("]")) {
          out.append(']');
        }
      } else {
        out.append(token);
      }
      out.append(" ");
    }
    // Remove the last added space
    out.deleteCharAt(out.length() - 1);
    return out.toString();
  }

  // This tokenizes a string, but with some special rules
  // It tokenizes based on spaces, but it will interpret quotes
  // that start in the middle of a string, after an '='
  // This is important because some of the data we want to preserve might
  // look like -Dxxxxx="this is all one token"
  // This is also aware of escape sequences
  public static String[] tokenizeString(String in) {
    ArrayList<String> tokens = new ArrayList<String>();
    StringBuilder word = new StringBuilder();
    Character currentQuote = null;
    boolean escaping = false;
    boolean afterEquals = false;
    // Order is important here. Rearrange at your own risk.
    for (char c : in.toCharArray()) {
      // If we're not escaping, start escaping and continue
      if (c == '\\' && !escaping) {
        escaping = true;
        word.append(c);
        continue;
      }

      // If we're escaping, always just add to the word and continue
      if (escaping) {
        escaping = false;
        word.append(c);
        continue;
      }

      // If we see an '=', remember that and continue
      if (c == '=') {
        afterEquals = true;
        word.append(c);
        continue;
      }

      // If we're not in a quote and we hit a space, save the word and continue
      if (currentQuote == null && c == ' ') {
        tokens.add(word.toString());
        word = new StringBuilder();
        continue;
      }

      // If we see a quote...
      if (c == '\'' || c == '"') {
        // If we are quoting...
        if (currentQuote != null) {
          // stop quoting if we're at the matching quote
          if (c == currentQuote) {
            currentQuote = null;
          }
        } else {
          // So we're not quoting...
          // If we're at a new word or after an equals, start quoting
          if (afterEquals || word.length() == 0) {
            currentQuote = c;
          }
        }
      }

      // Otherwise, just add the char
      afterEquals = false;
      word.append(c);
    }
    // Add the last word for the end of string
    tokens.add(word.toString());
    return tokens.toArray(new String[0]);
  }
}
