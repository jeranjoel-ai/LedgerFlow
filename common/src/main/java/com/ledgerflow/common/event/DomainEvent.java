package com.ledgerflow.common.event;

import java.time.Instant;

/**
 * Base contract for every domain event published to Kafka in LedgerFlow.
 *
 * <p>Every event is versioned and carries an {@code eventId} (for consumer idempotency via a {@code
 * processed_event} table) and an {@code occurredAt} timestamp. Concrete events are Java records
 * implementing this interface, e.g. {@code PaymentCreatedEvent}.
 */
public interface DomainEvent {

  String eventId();

  Instant occurredAt();

  String type();
}
