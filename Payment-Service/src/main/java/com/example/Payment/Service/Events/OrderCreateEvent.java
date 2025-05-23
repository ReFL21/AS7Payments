package com.example.Payment.Service.Events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateEvent {
    private boolean success;
    private Instant date;
    private Long userId;
    private Long price;
    private List<String> productIds;

}
