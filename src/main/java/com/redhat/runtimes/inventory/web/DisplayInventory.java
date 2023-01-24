package com.redhat.runtimes.inventory.web;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/api/runtimes-inventory/v1")
public class DisplayInventory {

  @GET
  @Path("/entry/{entry}/") // trailing slash is required by api
  @Produces(MediaType.APPLICATION_JSON)
  public String getRuntimeRecord(
    @PathParam("entry") UUID entry
  ) {
    return "{\"response\": \"ok\"}";
  }
}
