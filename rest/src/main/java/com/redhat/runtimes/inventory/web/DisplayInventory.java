/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.web;

import static com.redhat.runtimes.inventory.models.Constants.X_RH_IDENTITY_HEADER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.runtimes.inventory.models.JvmInstance;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Base64;
import java.util.Map;

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

  @GET
  @Path("/instance/") // trailing slash is required by api
  @Produces(MediaType.APPLICATION_JSON)
  public String getRuntimeRecord(
      @QueryParam("hostname") String hostname,
      @HeaderParam(X_RH_IDENTITY_HEADER) String rhIdentity) {
    // X_RH header is just B64 encoded - decode for the org ID
    var rhIdJson = new String(Base64.getDecoder().decode(rhIdentity));
    Log.debugf("X_RH_IDENTITY_HEADER: %s", rhIdJson);
    var orgId = "";
    try {
      orgId = extractOrgId(rhIdJson);
    } catch (Exception e) {
      processingErrorCounter.increment();
      return """
      {"response": "[error]"}""";
    }

    // Retrieve from DB
    var query =
        entityManager.createQuery(
            """
    select new com.redhat.runtimes.inventory.models.JvmInstance(
      i.id, i.accountId, i.orgId, i.hostname, i.launchTime, i.vendor, i.versionString,
      i.version, i.majorVersion, i.osArch, i.processors, i.heapMax, i.details, i.created
    ) from com.redhat.runtimes.inventory.models.JvmInstance i
    where i.orgId = :orgId and i.hostname = :hostname
    order by i.created desc""",
            JvmInstance.class);
    query.setParameter("orgId", orgId);
    query.setParameter("hostname", hostname);
    var results = query.getResultList();
    Log.debugf("Found %s rows when looking for %s in org %s", results.size(), hostname, orgId);
    if (results.size() == 0) {
      return """
      {"response": "[not found]"}""";
    }

    var mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    try {
      var map = Map.of("response", results.get(0));
      return mapper.writeValueAsString(map);

    } catch (JsonProcessingException e) {
      Log.error("JSON Exception", e);
      processingErrorCounter.increment();
      return """
      {"response": "[error]"}""";
    }
  }

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
