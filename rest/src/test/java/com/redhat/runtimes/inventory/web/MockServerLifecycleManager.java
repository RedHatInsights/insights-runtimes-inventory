/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.web;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import org.mockserver.integration.ClientAndServer;

// Derived from notifications-backend
public class MockServerLifecycleManager {

  private static final String LOG_LEVEL_KEY = "mockserver.logLevel";

  private static ClientAndServer mockServer;
  private static String mockServerUrl;

  public static void start() {
    if (System.getProperty(LOG_LEVEL_KEY) == null) {
      System.setProperty(LOG_LEVEL_KEY, "OFF");
      System.out.println(
          "MockServer log is disabled. Use '-D"
              + LOG_LEVEL_KEY
              + "=WARN|INFO|DEBUG|TRACE' to enable it.");
    }
    mockServer = startClientAndServer();
    mockServerUrl = "http://localhost:" + mockServer.getPort();
  }

  public static String getMockServerUrl() {
    return mockServerUrl;
  }

  public static ClientAndServer getClient() {
    return mockServer;
  }

  public static void stop() {
    if (mockServer != null) {
      mockServer.stop();
    }
  }
}
