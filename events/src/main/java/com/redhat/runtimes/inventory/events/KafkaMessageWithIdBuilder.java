/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static com.redhat.runtimes.inventory.events.KafkaMessageDeduplicator.MESSAGE_ID_HEADER;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import java.util.UUID;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Message;

public class KafkaMessageWithIdBuilder {

  public static Message build(String payload) {
    byte[] messageId = UUID.randomUUID().toString().getBytes(UTF_8);
    OutgoingKafkaRecordMetadata metadata =
        OutgoingKafkaRecordMetadata.builder()
            .withHeaders(new RecordHeaders().add(MESSAGE_ID_HEADER, messageId))
            .build();
    return Message.of(payload).addMetadata(metadata);
  }
}
