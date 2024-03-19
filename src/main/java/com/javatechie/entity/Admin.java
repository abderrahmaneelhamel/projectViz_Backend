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
public class Admin extends User {

    public Admin(Long id, String name, String email, String password, Role role){
        super(id,name,email,password,role);
    }
}