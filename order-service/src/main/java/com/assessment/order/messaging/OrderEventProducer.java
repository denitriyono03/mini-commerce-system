package com.assessment.order.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TOPIC = "order-placed-events";

    public void publishOrderEvent(Long productId, Integer quantity) {
        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("productId", productId);
            eventPayload.put("quantity", quantity);
            
            String message = objectMapper.writeValueAsString(eventPayload);
            
            kafkaTemplate.send(TOPIC, productId.toString(), message);
            log.info("Published order event for product {}: {}", productId, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order event", e);
        }
    }
}