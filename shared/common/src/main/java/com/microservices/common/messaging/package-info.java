/**
 * Shared event contracts and PostgreSQL transactional outbox infrastructure.
 * Each participating service owns its database, outbox_events table, and migrations.
 * Import MessagingConfiguration and enable scheduling in the service. Its JdbcTemplate
 * and transaction manager must use the same datasource as its business writes.
 * Enqueue inside the business transaction; the relay publishes in separate transactions.
 * Register additional contracts in EventTopics and EventCodec. Consumers deduplicate
 * using eventId because an acknowledged send can be retried after a database failure.
 */
package com.microservices.common.messaging;
