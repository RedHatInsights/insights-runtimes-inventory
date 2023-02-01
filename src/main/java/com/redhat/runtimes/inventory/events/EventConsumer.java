package com.redhat.runtimes.inventory.events;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy.PRE_PROCESSING;

@ApplicationScoped
public class EventConsumer {
  private static final String INGRESS_CHANNEL = "ingress";
  private static final String REJECTED_COUNTER_NAME = "input.rejected";
  private static final String PROCESSING_ERROR_COUNTER_NAME = "input.processing.error";
  private static final String PROCESSING_EXCEPTION_COUNTER_NAME = "input.processing.exception";
  private static final String DUPLICATE_COUNTER_NAME = "input.duplicate";
  private static final String CONSUMED_TIMER_NAME = "input.consumed";

  public static final String X_RH_IDENTITY_HEADER = "x-rh-identity";

  private static final String EVENT_TYPE_NOT_FOUND_MSG = "No event type found for [bundleName=%s, applicationName=%s, eventTypeName=%s]";

  @Inject
  MeterRegistry registry;

  // TODO
  @Inject
  KafkaMessageDeduplicator kafkaMessageDeduplicator;

  @Inject
  ArchiveAnnouncementParser jsonParser;

  @Inject
  EntityManager entityManager;

  private Counter rejectedCounter;
  private Counter processingErrorCounter;
  private Counter duplicateCounter;
  private Counter processingExceptionCounter;

  @PostConstruct
  public void init() {
    rejectedCounter = registry.counter(REJECTED_COUNTER_NAME);
    processingErrorCounter = registry.counter(PROCESSING_ERROR_COUNTER_NAME);
    processingExceptionCounter = registry.counter(PROCESSING_EXCEPTION_COUNTER_NAME);
    duplicateCounter = registry.counter(DUPLICATE_COUNTER_NAME);
  }

  @Incoming(INGRESS_CHANNEL)
  @Acknowledgment(PRE_PROCESSING)
  @Blocking
  @ActivateRequestContext
  public CompletionStage<Void> process(Message<String> message) {
    // This timer will have dynamic tag values based on the action parsed from the received message.
    Timer.Sample consumedTimer = Timer.start(registry);
    String payload = message.getPayload();

    Log.info("Processing received message: "+ payload);

    // Parse JSON using Jackson

    // Find org_id & hostname - use as a lookup key in DB

    // TODO Do we need this?
    // Extract UUID
//    UUID messageId = kafkaMessageDeduplicator.findMessageId(

    // Persist core data
    // entityManager.

    // FIXME Might need tags
    consumedTimer.stop(registry.timer(CONSUMED_TIMER_NAME));
    return message.ack();
  }

}
