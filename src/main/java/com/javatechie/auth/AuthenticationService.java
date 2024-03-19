package com.javatechie.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatechie.auth.token.Token;
import com.javatechie.auth.token.TokenRepository;
import com.javatechie.auth.token.TokenType;
import com.javatechie.auth.user.User;
import com.javatechie.auth.user.UserRepository;
import com.javatechie.config.JwtService;
import com.javatechie.entity.Admin;
import com.javatechie.entity.Client;
import com.javatechie.entity.Plan;
import com.javatechie.repository.AdminRepository;
import com.javatechie.repository.ClientRepository;
import com.javatechie.repository.Migration;
import com.javatechie.repository.PlanRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
  private final UserRepository userRepository;
  private final TokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final AdminRepository adminRepository;
  private final ClientRepository clientRepository;
  private final PlanRepository planRepository;
  private final Migration migration;

  public AuthenticationResponse register(RegisterRequest request){
    migration.populatePlans();
    System.out.println(request);
    var user = User.builder()
        .name(request.getFullName())
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(request.getRole())
        .build();
    var savedUser = userRepository.save(user);
    var jwtToken = jwtService.generateToken(savedUser,savedUser);
    switch (request.getRole()) {
      case ADMIN:
        Admin admin = new Admin(0L, request.getFullName() ,request.getEmail(),passwordEncoder.encode(request.getPassword()),request.getRole());
        adminRepository.save(admin);
        break;
      case CLIENT:
        Plan plan = planRepository.findById(1L).orElse(null);
        Client client = new Client(0L, request.getFullName() ,request.getEmail(),passwordEncoder.encode(request.getPassword()),request.getRole(),plan);
        clientRepository.save(client);
        break;
    }
    var refreshToken = jwtService.generateRefreshToken(savedUser);
    saveUserToken(savedUser, jwtToken);
    return AuthenticationResponse.builder()
        .accessToken(jwtToken)
        .refreshToken(refreshToken)
        .build();
  }

  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    migration.populatePlans();
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        )
    );
    var user = userRepository.findFirstByEmail(request.getEmail())
        .orElseThrow();
    var jwtToken = jwtService.generateToken(user,user);
    var refreshToken = jwtService.generateRefreshToken(user);
    revokeAllUserTokens(user);
    saveUserToken(user, jwtToken);
    return AuthenticationResponse.builder()
        .accessToken(jwtToken)
            .refreshToken(refreshToken)
        .build();
  }

  private void saveUserToken(User user, String jwtToken) {
    var token = Token.builder()
        .user(user)
        .token(jwtToken)
        .tokenType(TokenType.BEARER)
        .expired(false)
        .revoked(false)
        .build();
    tokenRepository.save(token);
  }

  private void revokeAllUserTokens(User user) {
    var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
    if (validUserTokens.isEmpty())
      return;
    validUserTokens.forEach(token -> {
      token.setExpired(true);
      token.setRevoked(true);
    });
    tokenRepository.saveAll(validUserTokens);
  }

  public void refreshToken(
          HttpServletRequest request,
          HttpServletResponse response
  ) throws IOException {
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    final String refreshToken;
    final String userEmail;
    if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
      return;
    }
    refreshToken = authHeader.substring(7);
    userEmail = jwtService.extractUsername(refreshToken);
    if (userEmail != null) {
      var user = this.userRepository.findFirstByEmail(userEmail)
              .orElseThrow();
      if (jwtService.isTokenValid(refreshToken, user)) {
        var accessToken = jwtService.generateToken(user,user);
        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);
        var authResponse = AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
      }
    }
  }


}
