/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static com.redhat.runtimes.inventory.events.Utils.readBytesFromResources;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class EventConsumerTest {

  @Test
  public void testSimpleUnzip() throws IOException {
    var buffy = readBytesFromResources("1J6DOEu9ni-000029.gz");
    var json = EventConsumer.unzipJson(buffy);
    System.out.println(json);
  }
}
