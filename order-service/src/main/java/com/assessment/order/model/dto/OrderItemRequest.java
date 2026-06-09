package com.assessment.order.model.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class OrderItemRequest {
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
}