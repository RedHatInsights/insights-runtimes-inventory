package com.redhat.runtimes.inventory.events;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ArchiveAnnouncementParserTest {

  public static String readFromResources(String fName) throws IOException {
    try (final InputStream is = ArchiveAnnouncementParserTest.class.getClassLoader().getResourceAsStream(fName);
         final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      for (;;) {
        int nread = is.read(buffer);
        if (nread <= 0) {
          break;
        }
        baos.write(buffer, 0, nread);
      }
      return new String(baos.toByteArray());
    }
  }

  @Test
  public void simpleParse() throws IOException {
    var json = readFromResources("incoming_kafka1.json");
    var parser = new ArchiveAnnouncementParser();

    var announce = parser.fromJsonString(json);
    assertEquals("12345", announce.getOrgId());
  }

}
