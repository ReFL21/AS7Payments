package com.example.Payment.Service.Domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentRequest {
    @NotNull
    private long amount;
    @NotBlank
    private String currency;

    @NotNull
    private Long userId;

    @NotEmpty
    private List<String> productIds;
}
