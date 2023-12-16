package com.seng.comfy.backend.controllers;

import com.seng.comfy.backend.entity.UserAuth;
import com.seng.comfy.backend.helper.AuthenticationRequest;
import com.seng.comfy.backend.helper.AuthenticationResponse;
import com.seng.comfy.backend.jwt.JwtUtil;
import com.seng.comfy.backend.repository.UserAuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserAuthRepository userAuthRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserAuth user) {
        if (userAuthRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email is already in use");
        }
        if(userAuthRepository.findByUsername(user.getUsername()).isPresent()){
            return ResponseEntity.badRequest().body("Username is already in use");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        UserAuth newUser = userAuthRepository.save(user);
        newUser.setPassword(null); // Don't return the password hash
        return ResponseEntity.ok(newUser);
    }


/* Old login endpoint

 @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserAuth user) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
        );
        if (authentication.isAuthenticated()) {
            return ResponseEntity.ok("User logged in successfully");
        } else {
            return ResponseEntity.badRequest().body("Invalid username or password");
        }
    }*/

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) {
        try {
            // Attempt to authenticate with the provided credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getUsername(),
                            authenticationRequest.getPassword()
                    )
            );
            // If authentication was successful, proceed to generate a JWT
            final String jwt = jwtUtil.generateToken(authenticationRequest.getUsername());
            return ResponseEntity.ok(new AuthenticationResponse(jwt));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/message")
    public String getMessage(){
        return "HERE IS YOUR MESSAGE";
    }
}
