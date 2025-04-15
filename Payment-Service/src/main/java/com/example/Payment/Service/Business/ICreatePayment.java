package com.example.Payment.Service.Business;

import com.stripe.model.PaymentIntent;

public interface ICreatePayment {
    PaymentIntent createPaymentIntent(Long amount, String currency);
}
