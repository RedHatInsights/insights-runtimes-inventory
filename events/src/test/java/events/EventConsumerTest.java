package events;

import com.redhat.runtimes.inventory.events.EventConsumer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static events.Utils.readBytesFromResources;

public class EventConsumerTest {

  @Test
  public void testSimpleUnzip() throws IOException {
    var buffy = readBytesFromResources("1J6DOEu9ni-000029.gz");
    var json = EventConsumer.unzipJson(buffy);
    System.out.println(json);
  }

}
