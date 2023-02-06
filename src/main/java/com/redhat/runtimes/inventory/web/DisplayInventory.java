/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.web;

import static com.redhat.runtimes.inventory.events.EventConsumer.X_RH_IDENTITY_HEADER;

import com.redhat.runtimes.inventory.models.RuntimesInstance;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.logging.Log;
import java.util.Base64;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

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

    // Retrieve from DB
    var query =
        entityManager.createQuery(
            """
    select new com.redhat.runtimes.inventory.models.RuntimesInstance(
      i.id, i.accountId, i.orgId, i.hostname, i.vendor, i.versionString,
      i.version, i.majorVersion, i.osArch, i.processors, i.heapMax
    ) from com.redhat.runtimes.inventory.models.RuntimesInstance i
    """,
            RuntimesInstance.class);
    var results = query.getResultList();
    Log.infof("Found %s rows when looking for %s", results.size(), hostname);

    // Test to see what's in rhIdentity
    return "{\"response\": \"" + rhIdentity + "\"}";
  }
}
