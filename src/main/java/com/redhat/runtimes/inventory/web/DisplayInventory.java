package com.redhat.runtimes.inventory.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

import static com.redhat.runtimes.inventory.events.EventConsumer.X_RH_IDENTITY_HEADER;

@Path("/api/runtimes-inventory/v1")
public class DisplayInventory {

  @Inject
  MeterRegistry registry;

  private Counter processingErrorCounter;

  @GET
  @Path("/entry/{entry}/") // trailing slash is required by api
  @Produces(MediaType.APPLICATION_JSON)
  public String getRuntimeRecord(
    @PathParam("entry") UUID entry,
    @HeaderParam(X_RH_IDENTITY_HEADER) String rhIdentity
  ) {
    // Test to see what's in rhIdentity
    return "{\"response\": \""+ rhIdentity +"\"}";
  }
}
