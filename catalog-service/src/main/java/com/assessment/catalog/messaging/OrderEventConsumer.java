package com.assessment.catalog.messaging;

import com.assessment.catalog.service.ProductService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ProductService productService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-placed-events", groupId = "catalog-group")
    public void consumeOrderEvent(String message) {
        try {
            log.info("Received Order Event: {}", message);
            JsonNode jsonNode = objectMapper.readTree(message);
            Long productId = jsonNode.get("productId").asLong();
            Integer quantity = jsonNode.get("quantity").asInt();

            productService.reduceStock(productId, quantity);
            log.info("Successfully reduced stock for product ID: {}", productId);
            
        } catch (Exception e) {
            log.error("Failed to process order event", e);
        }
    }
}