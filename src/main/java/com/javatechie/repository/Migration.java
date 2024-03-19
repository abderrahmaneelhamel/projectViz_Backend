package com.javatechie.repository;


import com.javatechie.entity.Plan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Migration {

    @Autowired
    private PlanRepository planRepository;

    public void populatePlans(){
      int count =  planRepository.findAll().size();
      if(count == 0) {
          planRepository.save(new Plan(1L, "Basic", 15, 0));
          planRepository.save(new Plan(2L, "Pro", 100, 49));
          planRepository.save(new Plan(3L, "Premium", -1, 199));
      }
    }
}
