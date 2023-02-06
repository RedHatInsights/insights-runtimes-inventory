package com.redhat.runtimes.inventory.web;

import com.redhat.runtimes.inventory.models.RuntimesInstance;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.logging.Log;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Base64;
import java.util.UUID;

import static com.redhat.runtimes.inventory.events.EventConsumer.X_RH_IDENTITY_HEADER;

@Path("/api/runtimes-inventory/v1")
public class DisplayInventory {

  @Inject
  MeterRegistry registry;

  @Inject
  EntityManager entityManager;

  private Counter processingErrorCounter;

  @GET
  @Path("/instance/") // trailing slash is required by api
  @Produces(MediaType.APPLICATION_JSON)
  public String getRuntimeRecord(
    @QueryParam("hostname") String hostname,
    @HeaderParam(X_RH_IDENTITY_HEADER) String rhIdentity
  ) {
    // X_RH header is just B64 encoded - decode for the org ID
    var rhIdJson = new String(Base64.getDecoder().decode(rhIdentity));
    Log.infof("X_RH_IDENTITY_HEADER: %s", rhIdJson);

    // Retrieve from DB
    var query = entityManager.createQuery("""
    select new com.redhat.runtimes.inventory.models.RuntimesInstance(
      i.id, i.account_id, i.org_id, i.hostname, i.vendor, i.version_string,
      i.version, i.major_version, i.os_arch, i.processors, i.heapMax
    ) from runtimes_instance i
    """, RuntimesInstance.class);
    var results = query.getResultList();
    Log.infof("Found %s rows when looking for %s", results.size(), hostname);

    // Test to see what's in rhIdentity
    return "{\"response\": \""+ rhIdentity +"\"}";
  }
}
