package org.profinef.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.profinef.entity.LoggedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.profinef.entity.ERole;
import org.profinef.entity.Role;
import org.profinef.entity.User;
import org.profinef.payload.request.LoginRequest;
import org.profinef.payload.request.SignupRequest;
import org.profinef.payload.response.JwtResponse;
import org.profinef.payload.response.MessageResponse;
import org.profinef.repository.RoleRepository;
import org.profinef.repository.UserRepository;
import org.profinef.security.jwt.JwtUtils;
import org.profinef.security.services.UserDetailsImpl;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  JwtUtils jwtUtils;
  static Set<LoggedUser> loggedUsers = new HashSet<>();

  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtUtils.generateJwtToken(authentication);
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    List<String> roles = userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());
    updateLoggedCounter(userDetails);
    return ResponseEntity.ok(new JwtResponse(jwt,
                         userDetails.getId(), 
                         userDetails.getUsername(),
                         roles));
  }

  static void updateLoggedCounter(UserDetailsImpl userDetails) {
    loggedUsers.add(new LoggedUser(userDetails.getId(), 0));
    for (LoggedUser loggedUser : loggedUsers) {
      if (loggedUser.getUserId() == userDetails.getId()) {
        loggedUser.setCounter(loggedUser.getCounter() + 1);
      }
    }
  }

  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    if (userRepository.existsByLogin(signUpRequest.getUsername())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Error: Username is already taken!"));
    }

    // Create new user's account
    User user = new User(signUpRequest.getUsername(),
               encoder.encode(signUpRequest.getPassword()));

    Set<String> strRoles = signUpRequest.getRole();
    Set<Role> roles = new HashSet<>();

    if (strRoles == null) {
      Role userRole = roleRepository.findByName(ERole.ROLE_MANAGER)
          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
        case "admin":
          Role adminRole = roleRepository.findByName(ERole.ROLE_SUPERADMIN)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(adminRole);

          break;
        case "mod":
          Role modRole = roleRepository.findByName(ERole.ROLE_ADMIN)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(modRole);

          break;
        default:
          Role userRole = roleRepository.findByName(ERole.ROLE_MANAGER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(userRole);
        }
      });
    }

    user.setRoles(roles);
    userRepository.save(user);

    return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
  }

  @PostMapping("/logout/{id}")
  public void logout(@PathVariable int id) {
    for (LoggedUser loggedUser : loggedUsers) {
      if (loggedUser.getUserId() == id) {
        loggedUser.setCounter(loggedUser.getCounter());
      }
      if (loggedUser.getCounter() <= 1) {
        loggedUsers.remove(loggedUser);
      }
    }
  }

  @GetMapping("/")
  public Set<LoggedUser> countLogged() {
     return loggedUsers;
  }
}
