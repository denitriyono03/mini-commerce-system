package com.assessment.order.controller;

import com.assessment.order.model.dto.OrderRequest;
import com.assessment.order.model.dto.TopSpenderReport;
import com.assessment.order.model.entity.Order;
import com.assessment.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(request));
    }

    @GetMapping("/reports/top-spenders")
    public ResponseEntity<List<TopSpenderReport>> getTopSpenders() {
        return ResponseEntity.ok(orderService.getTopSpendersReport());
    }
}