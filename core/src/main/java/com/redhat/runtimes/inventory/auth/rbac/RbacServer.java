/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.rbac;

import static com.redhat.runtimes.inventory.models.Constants.X_RH_IDENTITY_HEADER;

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/rbac/v1")
@RegisterRestClient(configKey = "rbac-authentication")
// @RegisterProvider(RbacRestClientRequestFilter.class)
// @RegisterProvider(RbacClientResponseFilter.class)
public interface RbacServer {

  @GET
  @Path("/access/") // trailing slash is required by api
  @Consumes("application/json")
  @Produces("application/json")
  @CacheResult(cacheName = "rbac-cache")
  Uni<RbacRaw> getRbacInfo(
      @QueryParam("application") String application,
      @HeaderParam(X_RH_IDENTITY_HEADER) String rhIdentity);

  @GET
  @Path("/groups/{groupID}/") // trailing slash is required by api
  @Produces("application/json")
  Response getGroup(
      @PathParam("groupID") UUID groupId, @HeaderParam(X_RH_IDENTITY_HEADER) String rhIdentity);
}
