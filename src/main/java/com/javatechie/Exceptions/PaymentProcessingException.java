package com.javatechie.Exceptions;

import com.stripe.exception.StripeException;

public class PaymentProcessingException  extends RuntimeException {

    public PaymentProcessingException(String message, StripeException e) {
        super(message);
    }
}