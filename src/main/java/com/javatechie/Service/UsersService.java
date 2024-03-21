package com.javatechie.Service;

import com.javatechie.Exceptions.PaymentFailedException;
import com.javatechie.Exceptions.PaymentProcessingException;
import com.javatechie.auth.user.User;
import com.javatechie.auth.user.UserRepository;
import com.javatechie.entity.Plan;
import com.javatechie.repository.PlanRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import org.springframework.stereotype.Service;

@Service
public class UsersService {

    UserRepository userRepository;
    PlanRepository planRepository;
    StripeService stripeService;

    public UsersService(UserRepository userRepository,PlanRepository planRepository,StripeService stripeService){
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.stripeService = stripeService;
    }



    public Boolean updatePlan(Integer id, long planId, String cardToken) {
        User user = userRepository.findById(id).orElse(null);
        Plan plan = planRepository.findById(planId).orElse(null);

        if (user != null && plan != null) {
            try {
                Charge charge = stripeService.chargeCreditCard(plan.getPrice(), cardToken);
                if (charge.getPaid()) {
                    user.setPlan(plan);
                    userRepository.save(user);
                    return true;
                } else {
                    throw new PaymentFailedException("Payment failed. Please try again.");
                }
            } catch (StripeException e) {
                throw new PaymentProcessingException("Error processing payment.", e);
            } catch (PaymentFailedException e) {
                throw new RuntimeException(e);
            }
        } else {
            return false;
        }
    }
}
