package com.event_registration.lk.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationProducer {

    private static final String TOPIC_USER_SIGNUP        = "eventslk.user.signup";
    private static final String TOPIC_BOOKING_CONFIRMED  = "eventslk.booking.confirmed";
    private static final String TOPIC_BOOKING_CANCELLED  = "eventslk.booking.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserSignup(UserSignupEvent event) {
        kafkaTemplate.send(TOPIC_USER_SIGNUP, event);
        log.info("[kafka] published UserSignupEvent for {}", event.getEmail());
    }

    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        kafkaTemplate.send(TOPIC_BOOKING_CONFIRMED, event);
        log.info("[kafka] published BookingConfirmedEvent for {} booking={}", event.getEmail(), event.getBookingId());
    }

    public void publishBookingCancelled(BookingCancelledEvent event) {
        kafkaTemplate.send(TOPIC_BOOKING_CANCELLED, event);
        log.info("[kafka] published BookingCancelledEvent for {} booking={}", event.getEmail(), event.getBookingId());
    }
}
