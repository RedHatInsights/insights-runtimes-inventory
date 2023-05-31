/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.web;

import static com.redhat.runtimes.inventory.models.Constants.X_RH_IDENTITY_HEADER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.runtimes.inventory.models.RuntimesInstance;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Base64;
import java.util.Map;

@Path("/api/runtimes-inventory/v1")
public class DisplayInventory {

  @Inject MeterRegistry registry;

  @Inject EntityManager entityManager;

  private Counter processingErrorCounter;

  @GET
  @Path("/instance/") // trailing slash is required by api
  @Produces(MediaType.APPLICATION_JSON)
  public String getRuntimeRecord(
      @QueryParam("hostname") String hostname,
      @HeaderParam(X_RH_IDENTITY_HEADER) String rhIdentity) {
    // X_RH header is just B64 encoded - decode for the org ID
    var rhIdJson = new String(Base64.getDecoder().decode(rhIdentity));
    Log.infof("X_RH_IDENTITY_HEADER: %s", rhIdJson);
    var orgId = extractOrgId(rhIdJson);

    // Retrieve from DB
    var query =
        entityManager.createQuery(
            """
    select new com.redhat.runtimes.inventory.models.RuntimesInstance(
      i.id, i.accountId, i.orgId, i.hostname, i.launchTime, i.vendor, i.versionString,
      i.version, i.majorVersion, i.osArch, i.processors, i.heapMax, i.details, i.created
    ) from com.redhat.runtimes.inventory.models.RuntimesInstance i
    where i.orgId = :orgId and i.hostname = :hostname
    order by i.created desc
    """,
            RuntimesInstance.class);
    query.setParameter("orgId", orgId);
    query.setParameter("hostname", hostname);
    var results = query.getResultList();
    Log.infof("Found %s rows when looking for %s in org %s", results.size(), hostname, orgId);
    var out = results.size() == 0 ? "[not found]" : results.get(0);

    // FIXME Temp - need proper marshalling
    return "{\"response\": \"" + out + "\"}";
  }

  String extractOrgId(String rhIdJson) {
    TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
    String out = "";

    var mapper = new ObjectMapper();
    try {
      var o = mapper.readValue(rhIdJson, typeRef);
      var identity = (Map<String, Object>) o.get("identity");
      out = String.valueOf(identity.get("org_id"));
    } catch (JsonProcessingException | ClassCastException | NumberFormatException e) {
      Log.error("Error in unmarshalling JSON", e);
      throw new RuntimeException("Error in unmarshalling JSON", e);
    }

    return out;
  }
}
