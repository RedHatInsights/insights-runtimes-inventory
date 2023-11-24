/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import static com.redhat.runtimes.inventory.models.InsightsMessage.REDACTED_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class InsightsMessageTest {

  @Test
  public void testSanitizeJavaParameters() {
    String unsanitizedJvmArgs =
        "[-D[Standalone], -verbose:gc, -Xloggc:/opt/jboss-eap-7.4.0/standalone/log/gc.log,"
            + " -Djava.net.preferIPv4Stack=true, -Djboss.modules.system.pkgs=org.jboss.byteman,"
            + " -Djava.awt.headless=true,"
            + " -Dorg.jboss.boot.log.file=/opt/jboss-eap-7.4.0/standalone/log/server.log,"
            + " -Dsome.dumb.practice=\"Man I hope \\\" ' this = works\","
            + " -Dsome.broken.practice=\"Man I hope ' '{ this still = works\","
            + " -Dsome.nice.json=\"{\"a\":\"b\"}\","
            + " -Dsome.broken.json=\"{\"a\":\"b\"'{\",";
    String sanitizedJvmArgs =
        "[-D[Standalone], -verbose:gc, -Xloggc:/opt/jboss-eap-7.4.0/standalone/log/gc.log,"
            + " -Djava.net.preferIPv4Stack"
            + REDACTED_VALUE
            + ", -Djboss.modules.system.pkgs"
            + REDACTED_VALUE
            + ","
            + " -Djava.awt.headless"
            + REDACTED_VALUE
            + ", -Dorg.jboss.boot.log.file"
            + REDACTED_VALUE
            + ","
            + " -Dsome.dumb.practice"
            + REDACTED_VALUE
            + ", -Dsome.broken.practice"
            + REDACTED_VALUE
            + ", -Dsome.nice.json"
            + REDACTED_VALUE
            + ","
            + " -Dsome.broken.json"
            + REDACTED_VALUE
            + ",";

    assertEquals(sanitizedJvmArgs, InsightsMessage.sanitizeJavaParameters(unsanitizedJvmArgs));
  }

  // This test is to ensure that we don't have any impact when we sanitize the JVM args on both
  // client and server side
  @Test
  public void testDoubleSanitizeJavaParametersIsANoop() {
    String sanitizedJvmArgs =
        "[-D[Standalone], -verbose:gc, -Xloggc:/opt/jboss-eap-7.4.0/standalone/log/gc.log,"
            + " -Djava.net.preferIPv4Stack"
            + REDACTED_VALUE
            + ", -Djboss.modules.system.pkgs"
            + REDACTED_VALUE
            + ","
            + " -Djava.awt.headless"
            + REDACTED_VALUE
            + ", -Dorg.jboss.boot.log.file"
            + REDACTED_VALUE
            + ","
            + " -Dsome.dumb.practice"
            + REDACTED_VALUE
            + ", -Dsome.broken.practice"
            + REDACTED_VALUE
            + ", -Dsome.nice.json"
            + REDACTED_VALUE
            + ","
            + " -Dsome.broken.json"
            + REDACTED_VALUE
            + ",";

    assertEquals(sanitizedJvmArgs, InsightsMessage.sanitizeJavaParameters(sanitizedJvmArgs));
  }
}
