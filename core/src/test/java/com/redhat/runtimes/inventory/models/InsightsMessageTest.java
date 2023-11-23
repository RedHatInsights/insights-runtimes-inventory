/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

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
            + " -Djava.net.preferIPv4Stack=*****, -Djboss.modules.system.pkgs=*****,"
            + " -Djava.awt.headless=*****, -Dorg.jboss.boot.log.file=*****,"
            + " -Dsome.dumb.practice=*****, -Dsome.broken.practice=*****, -Dsome.nice.json=*****,"
            + " -Dsome.broken.json=*****,";

    assertEquals(sanitizedJvmArgs, InsightsMessage.sanitizeJavaParameters(unsanitizedJvmArgs));
  }
}
