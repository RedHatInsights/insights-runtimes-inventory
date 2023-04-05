package events;

import com.redhat.runtimes.inventory.events.ArchiveAnnouncementParser;
import org.junit.jupiter.api.Test;

import static events.Utils.readFromResources;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class ArchiveAnnouncementParserTest {

  @Test
  public void simpleParse() throws IOException {
    var json = readFromResources("incoming_kafka1.json");
    var parser = new ArchiveAnnouncementParser();

    var announce = parser.fromJsonString(json);
    assertEquals("12345", announce.getOrgId());
  }

}
