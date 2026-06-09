package com.assessment.order.model.dto;

import java.math.BigDecimal;

public interface TopSpenderReport {
    String getCustomerId();
    BigDecimal getTotalSpent();
    Integer getRankPosition();
}