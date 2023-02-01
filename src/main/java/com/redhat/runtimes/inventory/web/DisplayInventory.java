package com.redhat.runtimes.inventory.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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
    // Can we decode the org ID from the X_RH header?

    // Retrieve from DB
    var query = entityManager.createQuery("select new com.redhat.runtimes.inventory.models.RuntimesInstance from public.runtimes_instance");
    var results = query.getResultList();

    // Test to see what's in rhIdentity
    return "{\"response\": \""+ rhIdentity +"\"}";
  }
}
