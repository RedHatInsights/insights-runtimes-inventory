/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class TestUtils {
  private TestUtils() {}

  public static String readFromResources(String fName) throws IOException {
    return new String(readBytesFromResources(fName));
  }

  public static byte[] readBytesFromResources(String fName) throws IOException {
    try (final InputStream is =
            ArchiveAnnouncementParserTest.class.getClassLoader().getResourceAsStream(fName);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      for (; ; ) {
        int nread = is.read(buffer);
        if (nread <= 0) {
          break;
        }
        baos.write(buffer, 0, nread);
      }
      return baos.toByteArray();
    }
  }
}
