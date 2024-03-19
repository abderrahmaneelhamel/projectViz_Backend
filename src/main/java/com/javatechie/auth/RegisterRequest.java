package com.javatechie.auth;

import com.javatechie.auth.user.Role;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class RegisterRequest {

  // Admin & BlogAuthor
  public RegisterRequest(String fullName, String email, String password, Role role) {
    this.fullName = fullName;
    this.email = email;
    this.password = password;
    this.role = role;
  }

  // Client
  public RegisterRequest(String fullName, String email, String password, Role role, String address, String phone) {
    this.fullName = fullName;
    this.email = email;
    this.password = password;
    this.role = role;
    this.address = address;
    this.phone = phone;
  }

  private String fullName;
  private String email;
  private String password;
  private Role role;
  private String address;
  private String phone;
}
