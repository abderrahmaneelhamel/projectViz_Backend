package com.javatechie.Service;


import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    public Charge chargeCreditCard(double amount, String token) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        Map<String, Object> params = new HashMap<>();
        params.put("amount", (int) (amount * 100));
        params.put("currency", "usd");
        params.put("source", token);

        return Charge.create(params);
    }
}
