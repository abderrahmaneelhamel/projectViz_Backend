package com.javatechie.auth;

import com.javatechie.auth.user.Role;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class RegisterRequest {

  public RegisterRequest(String fullName, String email, String password) {
    this.fullName = fullName;
    this.email = email;
    this.password = password;
  }


  private String fullName;
  private String email;
  private String password;
}
