/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.web;

import static com.redhat.runtimes.inventory.models.Constants.X_RH_IDENTITY_HEADER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.runtimes.inventory.models.JarHash;
import com.redhat.runtimes.inventory.models.JvmInstance;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/runtimes-inventory-service/v1")
public class DisplayInventory {
  public static final String PROCESSING_ERROR_COUNTER_NAME = "input.processing.error";

  @Inject MeterRegistry registry;

  @Inject EntityManager entityManager;

  private Counter processingErrorCounter;

  @PostConstruct
  public void init() {
    processingErrorCounter = registry.counter(PROCESSING_ERROR_COUNTER_NAME);
  }

  /**
   * Given a RH identity header and a hostname, return all the associated JVM instance IDs
   *
   * @param hostname associated with the JVM instance
   * @param rhIdentity
   * @return JSON String containing a list of JVM instance IDs
   */
  @GET
  @Path("/instance-ids/") // trailing slash is required by api
  @Produces(MediaType.APPLICATION_JSON)
  public String getJvmInstanceIdRecords(
      @QueryParam("hostname") String hostname,
      @HeaderParam(X_RH_IDENTITY_HEADER) String rhIdentity) {
    // X_RH header is just B64 encoded - decode for the org ID
    String rhIdJson = new String(Base64.getDecoder().decode(rhIdentity));
    Log.debugf("X_RH_IDENTITY_HEADER: %s", rhIdJson);
    String orgId = "";
    try {
      orgId = extractOrgId(rhIdJson);
    } catch (Exception e) {
      processingErrorCounter.increment();
      return "{\"response\": \"[error]\"}";
    }
    // Retrieve from DB
    TypedQuery<UUID> query =
        entityManager.createQuery(
            """
              SELECT i.id
              FROM JvmInstance i
              WHERE i.orgId = :orgId and i.hostname = :hostname
              ORDER BY i.created desc
            """,
            UUID.class);
    query.setParameter("orgId", orgId);
    query.setParameter("hostname", hostname);
    List<UUID> results = query.getResultList();
    return mapResultListToJson(results);
  }

  /**
   * Given a RH identity header and a JVM instance id, return the associated JVM instance
   *
   * @param jvmInstanceId id of the JVM instance
   * @param rhIdentity
   * @return JSON String containing the specified JVM instance
   */
  @GET
  @Path("/instance/") // trailing slash is required by api
  @Produces(MediaType.APPLICATION_JSON)
  public String getJvmInstanceRecord(
      @QueryParam("jvmInstanceId") String jvmInstanceId,
      @HeaderParam(X_RH_IDENTITY_HEADER) String rhIdentity) {
    // X_RH header is just B64 encoded - decode for the org ID
    String rhIdJson = new String(Base64.getDecoder().decode(rhIdentity));
    Log.debugf("X_RH_IDENTITY_HEADER: %s", rhIdJson);
    String orgId = "";
    try {
      orgId = extractOrgId(rhIdJson);
    } catch (Exception e) {
      processingErrorCounter.increment();
      return """
      {"response": "[error]"}""";
    }
    // Retrieve from DB
    TypedQuery<JvmInstance> query =
        entityManager.createQuery(
            """
              SELECT i
              FROM JvmInstance i
              WHERE i.orgId = :orgId AND i.id = :id
              ORDER BY i.created desc
            """,
            JvmInstance.class);
    query.setParameter("id", UUID.fromString(jvmInstanceId));
    query.setParameter("orgId", orgId);
    JvmInstance result;
    try {
      result = query.getSingleResult();
    } catch (NoResultException e) {
      return "{\"response\": \"[]\"}";
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    try {
      Map<String, JvmInstance> map = Map.of("response", result);
      return mapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      Log.error("JSON Exception", e);
      processingErrorCounter.increment();
      return "{\"response\": \"[error]\"}";
    }
  }

  /**
   * Given a RH identity header and a hostname, return all the associated JVM instances
   *
   * @param hostname associated with the JVM Instance
   * @param rhIdentity
   * @return JSON String containing a list of JVM instances
   */
  @GET
  @Path("/instances/")
  @Produces(MediaType.APPLICATION_JSON)
  public String getAllJvmInstanceRecords(
      @QueryParam("hostname") String hostname,
      @HeaderParam(X_RH_IDENTITY_HEADER) String rhIdentity) {
    // X_RH header is just B64 encoded - decode for the org ID
    String rhIdJson = new String(Base64.getDecoder().decode(rhIdentity));
    Log.debugf("X_RH_IDENTITY_HEADER: %s", rhIdJson);
    String orgId = "";
    try {
      orgId = extractOrgId(rhIdJson);
    } catch (Exception e) {
      processingErrorCounter.increment();
      return "{\"response\": \"[error]\"}";
    }
    // Retrieve from DB
    TypedQuery<JvmInstance> query =
        entityManager.createQuery(
            """
              SELECT i
              FROM JvmInstance i
              WHERE i.orgId = :orgId AND i.hostname = :hostname
              ORDER BY i.created desc
            """,
            JvmInstance.class);
    query.setParameter("orgId", orgId);
    query.setParameter("hostname", hostname);
    List<JvmInstance> results = query.getResultList();
    return mapResultListToJson(results);
  }

  /**
   * Given the id of a JVM instance, return all ids of associated jar hashes
   *
   * @param jvmInstanceId identifier of the JVM instance
   * @return JSON String containing a list of jar hash ids associated with a JVM instance
   */
  @GET
  @Path("/jarhash-ids/")
  @Produces(MediaType.APPLICATION_JSON)
  public String getJarHashIdRecords(@QueryParam("jvmInstanceId") String jvmInstanceId) {
    Query query =
        entityManager.createNativeQuery(
            """
              SELECT j.jar_hash_id
              FROM jvm_instance_jar_hash j
              WHERE j.jvm_instance_id = :instanceId
            """,
            UUID.class);
    query.setParameter("instanceId", UUID.fromString(jvmInstanceId));
    var results = query.getResultList();
    return mapResultListToJson(results);
  }

  /**
   * Given the id of a jar hash, return the associated jar hash
   *
   * @param jarHashId identifier of the jar hash
   * @return JSON String containing the specified jar hash
   */
  @GET
  @Path("/jarhash/")
  @Produces(MediaType.APPLICATION_JSON)
  public String getJarHashRecord(@QueryParam("jarHashId") String jarHashId) {
    TypedQuery<JarHash> query =
        entityManager.createQuery(
            """
              SELECT new com.redhat.runtimes.inventory.models.JarHash(
                j.id, j.name, j.groupId, j.vendor, j.version,
                j.sha1Checksum, j.sha256Checksum, j.sha512Checksum)
              FROM JarHash j
              WHERE j.id = :jarHashId
            """,
            JarHash.class);
    query.setParameter("jarHashId", UUID.fromString(jarHashId));
    JarHash result;
    try {
      result = query.getSingleResult();
    } catch (NoResultException e) {
      return "{\"response\": \"[]\"}";
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    try {
      Map<String, JarHash> map = Map.of("response", result);
      return mapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      Log.error("JSON Exception", e);
      processingErrorCounter.increment();
      return "{\"response\": \"[error]\"}";
    }
  }

  /**
   * Given a JVM instance identifier, return all associated jar hashes
   *
   * @param jvmInstanceId identifier of the JVM instance
   * @return JSON String containing all jar hashes associated with a particular JVM instance
   */
  @GET
  @Path("/jarhashes/")
  @Produces(MediaType.APPLICATION_JSON)
  public String getAllJarHashRecords(@QueryParam("jvmInstanceId") String jvmInstanceId) {
    Query query =
        entityManager.createNativeQuery(
            """
              SELECT
                jh.id, jh.name, jh.group_id, jh.vendor, jh.version,
                jh.sha1Checksum, jh.sha256Checksum, jh.sha512Checksum
              FROM jvm_instance_jar_hash jt
              RIGHT JOIN jar_hash jh
              ON jt.jar_hash_id = jh.id
              WHERE jt.jvm_instance_id = :instanceId
            """,
            JarHash.class);
    query.setParameter("instanceId", UUID.fromString(jvmInstanceId));
    var results = query.getResultList();
    return mapResultListToJson(results);
  }

  private String mapResultListToJson(List<?> resultList) {
    if (resultList.size() == 0) {
      return "{\"response\": \"[]\"}";
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    try {
      Map<String, List<?>> map = Map.of("response", resultList);
      return mapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      Log.error("JSON Exception", e);
      processingErrorCounter.increment();
      return "{\"response\": \"[error]\"}";
    }
  }

  @SuppressWarnings("unchecked")
  static String extractOrgId(String rhIdJson) {
    TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
    String out = "";

    var mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    try {
      var o = mapper.readValue(rhIdJson, typeRef);
      var identity = (Map<String, Object>) o.get("identity");
      out = String.valueOf(identity.get("org_id"));
    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling incoming JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }

    return out;
  }
}
