package com.javatechie.entity;

import com.javatechie.auth.user.Role;
import com.javatechie.auth.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Client extends User {

    public Client(Long id, String name, String email, String password, Role role, Plan plan){
        super(id,name,email,password,role);
        this.plan = plan;
    }

    @ManyToOne
    @JoinColumn(name = "plan_id", referencedColumnName = "id")
    private Plan plan;
}
