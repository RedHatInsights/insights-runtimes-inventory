/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.runtimes.inventory.models.InsightsMessage;
import com.redhat.runtimes.inventory.models.JarHash;
import com.redhat.runtimes.inventory.models.JvmInstance;
import com.redhat.runtimes.inventory.models.UpdateInstance;
import io.quarkus.logging.Log;
import java.time.ZoneOffset;
import java.util.*;

final class Utils {
  private Utils() {}

  static InsightsMessage jvmInstanceOf(ArchiveAnnouncement announce, String json) {
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
        var updatedJars = (Map<String, Object>) o.get("updated-jars");
        if (updatedJars != null) {
          return updatedInstanceOf(updatedJars);
        }
        throw new RuntimeException(
            "Error in unmarshalling JSON - does not contain a basic or updated-jars tag");
      }

      mapJvmInstanceValues(inst, o, basic);
      inst.setJarHashes(jarHashesOf(inst, (Map<String, Object>) o.get("jars")));
    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }

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

      // FIXME Add heap min
      inst.setHeapMax((int) Double.parseDouble(String.valueOf(basic.get("jvm.heap.max"))));
      inst.setLaunchTime(Long.parseLong(String.valueOf(basic.get("jvm.report_time"))));

      inst.setOsArch(String.valueOf(basic.get("system.arch")));
      inst.setProcessors(Integer.parseInt(String.valueOf(basic.get("system.cores.logical"))));
      inst.setHostname(String.valueOf(basic.get("system.hostname")));

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
    jarsJson.forEach(j -> jars.add(jarHashOf(null, j)));
    return new UpdateInstance(linkingHash, jars);
  }

  static Set<JarHash> jarHashesOf(JvmInstance inst, Map<String, Object> jarsRep) {
    if (jarsRep == null) {
      return Set.of();
    }
    var jars = (List<Object>) jarsRep.get("jars");
    if (jars == null) {
      return Set.of();
    }
    var out = new HashSet<JarHash>();
    jars.forEach(j -> out.add(jarHashOf(inst, (Map<String, Object>) j)));

    return out;
  }

  static JarHash jarHashOf(JvmInstance inst, Map<String, Object> jarJson) {
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
}
