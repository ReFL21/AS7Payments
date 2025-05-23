package com.example.Payment.Service.Business;

import com.example.Payment.Service.Domain.PaymentRequest;
import com.example.Payment.Service.Events.OrderCreateEvent;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePaymentImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private CreatePaymentImpl svc;

    private static final String TEST_API_KEY = "sk_test_ABC123";

    @BeforeEach
    void setUp() {
        svc = new CreatePaymentImpl(rabbitTemplate, TEST_API_KEY);
        svc.init();  // @PostConstruct equivalent
    }

    @Test
    void init_shouldSetStripeApiKey() {
        assertEquals(TEST_API_KEY, Stripe.apiKey,
                "init() must configure Stripe.apiKey");
    }

    @Test
    void createPaymentIntent_buildsParamMapAndReturnsIntent() throws StripeException {
        // arrange
        PaymentRequest req = new PaymentRequest();
        req.setAmount(2_500L);
        req.setCurrency("eur");
        req.setUserId(99L);
        req.setProductIds(List.of("A", "B"));

        PaymentIntent fake = mock(PaymentIntent.class);

        // stub the static call to the Map<String,Object> overload
        try (MockedStatic<PaymentIntent> stripeStatic = mockStatic(PaymentIntent.class)) {
            stripeStatic.when(() ->
                    PaymentIntent.create(anyMap())
            ).thenReturn(fake);

            // act
            PaymentIntent result = svc.createPaymentIntent(req);

            // assert
            assertSame(fake, result);

            // verify the map contents
            stripeStatic.verify(() ->
                    PaymentIntent.create(argThat((Map<String,Object> m) ->
                            m.get("amount").equals(2_500L) &&
                                    m.get("currency").equals("eur") &&
                                    m.get("payment_method_types") instanceof List &&
                                    ((List<?>)m.get("payment_method_types")).contains("card") &&
                                    m.get("metadata") instanceof Map &&
                                    ((Map<?,?>)m.get("metadata")).get("userId").equals("99") &&
                                    ((Map<?,?>)m.get("metadata")).get("productIds").equals("A,B")
                    ))
            );
        }
    }

    @Test
    void createPaymentIntent_stripeExceptionIsWrapped() throws StripeException {
        PaymentRequest req = new PaymentRequest();

        try (MockedStatic<PaymentIntent> stripeStatic = mockStatic(PaymentIntent.class)) {
            stripeStatic.when(() ->
                    PaymentIntent.create(anyMap())
            ).thenThrow(new StripeException("msg", "err", "req_id", 400, null) {});

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> svc.createPaymentIntent(req));
            assertTrue(ex.getMessage().contains("Failed to create PaymentIntent"));
            assertTrue(ex.getCause() instanceof StripeException);
        }
    }

    @Test
    void confirmPaymentIntent_onSuccessPublishesEventAndReturnsStatus() throws StripeException {
        String pid = "pi_123";
        PaymentIntent retrieved = mock(PaymentIntent.class);
        PaymentIntent confirmed  = mock(PaymentIntent.class);

        // stub status, metadata, amount
        when(confirmed.getStatus()).thenReturn("succeeded");
        when(confirmed.getMetadata()).thenReturn(Map.of(
                "userId", "77",
                "productIds", "X,Y,Z"
        ));
        when(confirmed.getAmount()).thenReturn(555L);

        try (MockedStatic<PaymentIntent> stripeStatic = mockStatic(PaymentIntent.class)) {
            stripeStatic.when(() -> PaymentIntent.retrieve(pid)).thenReturn(retrieved);
            // confirm(Map<String,Object>) overload
            when(retrieved.confirm(anyMap())).thenReturn(confirmed);

            // act
            String status = svc.confirmPaymentIntent(pid);

            // assert return
            assertEquals("succeeded", status);

            // capture event
            @SuppressWarnings("unchecked")
            ArgumentCaptor<OrderCreateEvent> evtCap =
                    ArgumentCaptor.forClass(OrderCreateEvent.class);

            verify(rabbitTemplate)
                    .convertAndSend(
                            eq("order.exchange"),  // from @Value default
                            eq("order.create"),    // from @Value default
                            evtCap.capture()
                    );

            OrderCreateEvent evt = evtCap.getValue();
//            assertTrue(evt.isSuccessful());
//            assertNotNull(evt.getTimestamp());
            assertEquals(77L, evt.getUserId());
            assertEquals(555L, evt.getPrice());
            assertEquals(List.of("X","Y","Z"), evt.getProductIds());
        }
    }

    @Test
    void confirmPaymentIntent_stripeExceptionIsWrapped() throws StripeException {
        String pid = "pi_fail";

        try (MockedStatic<PaymentIntent> stripeStatic = mockStatic(PaymentIntent.class)) {
            stripeStatic.when(() -> PaymentIntent.retrieve(pid))
                    .thenThrow(new StripeException("msg", "err", "req_123", 500, null) {});

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> svc.confirmPaymentIntent(pid));
            assertTrue(ex.getMessage().contains("Failed to confirm PaymentIntent"));
            assertTrue(ex.getCause() instanceof StripeException);
        }
    }
}
