/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static com.redhat.runtimes.inventory.events.TestUtils.readFromResources;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ArchiveAnnouncementParserTest {

  @Test
  public void simpleParse() throws IOException {
    var json = readFromResources("incoming_kafka1.json");
    var parser = new ArchiveAnnouncementParser();

    var announce = parser.fromJsonString(json);
    assertEquals("12345", announce.getOrgId());
  }
}
