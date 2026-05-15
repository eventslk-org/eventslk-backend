package com.event_registration.lk.outbox;

import com.event_registration.lk.entity.OutboxEvent;
import com.event_registration.lk.kafka.NotificationProducer;
import com.event_registration.lk.kafka.UserSignupEvent;
import com.event_registration.lk.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
public class OutboxPoller {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxRepository;
    private final NotificationProducer notificationProducer;
    private final ObjectMapper objectMapper;

    public OutboxPoller(OutboxEventRepository outboxRepository,
                        NotificationProducer notificationProducer,
                        ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.notificationProducer = notificationProducer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pending = outboxRepository.findByStatusOrderByCreatedAt(
                OutboxEvent.Status.PENDING, PageRequest.of(0, BATCH_SIZE));

        if (pending.isEmpty()) return;

        log.debug("[outbox] processing {} pending events", pending.size());

        for (OutboxEvent event : pending) {
            try {
                publish(event);
                event.setStatus(OutboxEvent.Status.PROCESSED);
                outboxRepository.save(event);
            } catch (Exception e) {
                // Leave as PENDING so the next tick retries. Kafka broker may be down.
                log.warn("[outbox] failed to publish event id={} type={}, will retry: {}",
                        event.getId(), event.getType(), e.getMessage());
            }
        }
    }

    private void publish(OutboxEvent event) throws Exception {
        switch (event.getType()) {
            case "UserSignupEvent" -> {
                UserSignupEvent payload = objectMapper.readValue(event.getPayload(), UserSignupEvent.class);
                notificationProducer.publishUserSignup(payload);
            }
            default -> throw new IllegalStateException("Unknown outbox event type: " + event.getType());
        }
    }
}
