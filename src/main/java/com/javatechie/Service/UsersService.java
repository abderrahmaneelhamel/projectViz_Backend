package com.javatechie.Service;

import com.javatechie.Exceptions.PaymentFailedException;
import com.javatechie.Exceptions.PaymentProcessingException;
import com.javatechie.entity.Client;
import com.javatechie.entity.Plan;
import com.javatechie.repository.ClientRepository;
import com.javatechie.repository.PlanRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import org.springframework.stereotype.Service;

@Service
public class UsersService {

    ClientRepository  clientRepository;
    PlanRepository planRepository;
    StripeService stripeService;

    public UsersService(ClientRepository clientRepository,PlanRepository planRepository,StripeService stripeService){
        this.clientRepository = clientRepository;
        this.planRepository = planRepository;
        this.stripeService = stripeService;
    }



    public Client updatePlan(Long id, long planId, String cardToken) {
        Client client = clientRepository.findById(id).orElse(null);
        Plan plan = planRepository.findById(planId).orElse(null);

        if (client != null && plan != null) {
            try {
                Charge charge = stripeService.chargeCreditCard(plan.getPrice(), cardToken);
                if (charge.getPaid()) {
                    client.setPlan(plan);
                    return clientRepository.save(client);
                } else {
                    throw new PaymentFailedException("Payment failed. Please try again.");
                }
            } catch (StripeException e) {
                throw new PaymentProcessingException("Error processing payment.", e);
            } catch (PaymentFailedException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }
}
