/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth;

import static com.redhat.runtimes.inventory.models.Constants.X_RH_IDENTITY_HEADER;

import com.redhat.runtimes.inventory.auth.principal.ConsolePrincipal;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.Set;

// Derived from notifications-backend

/**
 * Implements Jakarta EE JSR-375 (Security API) HttpAuthenticationMechanism for the Insights
 * x-rh-identity header and RBAC
 */
@ApplicationScoped
public class ConsoleAuthMechanism implements HttpAuthenticationMechanism {

  @Override
  public Uni<SecurityIdentity> authenticate(
      RoutingContext routingContext, IdentityProviderManager identityProviderManager) {
    String xRhIdentityHeaderValue = routingContext.request().getHeader(X_RH_IDENTITY_HEADER);
    String path = routingContext.normalizedPath();

    if (xRhIdentityHeaderValue == null) { // Access that did not go through 3Scale
      boolean good = false;

      // We block access unless the openapi file is requested.
      if (path.startsWith("/api/runtimes-inventory-service") && path.endsWith("openapi.json")) {
        good = true;
      }

      if (path.startsWith("/openapi.json")
          || path.startsWith("/health")
          || path.startsWith("/metrics")) {
        good = true;
      }

      if (!good) {
        return Uni.createFrom()
            .failure(new AuthenticationFailedException("No " + X_RH_IDENTITY_HEADER + " provided"));
      } else {
        return Uni.createFrom()
            .item(
                QuarkusSecurityIdentity.builder()
                    // Set a dummy principal, but add no roles.
                    .setPrincipal(ConsolePrincipal.noIdentity())
                    .build());
      }
    }

    ConsoleAuthenticationRequest authReq = new ConsoleAuthenticationRequest(xRhIdentityHeaderValue);
    return identityProviderManager.authenticate(authReq);
  }

  @Override
  public Uni<ChallengeData> getChallenge(RoutingContext routingContext) {
    return Uni.createFrom().nullItem();
  }

  @Override
  public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
    return Collections.singleton(ConsoleAuthenticationRequest.class);
  }

  @Override
  public HttpCredentialTransport getCredentialTransport() {
    return null;
  }
}
