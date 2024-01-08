package com.seng.comfy.backend.controllers;

import com.seng.comfy.backend.entity.UserAuth;
import com.seng.comfy.backend.entity.UserProfile;
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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

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
        // Check if the email is already in use
        if (userAuthRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email is already in use");
        }

        // Check if the username is already in use
        if(userAuthRepository.findByUsername(user.getUsername()).isPresent()){
            return ResponseEntity.badRequest().body("Username is already in use");
        }

        // Encode the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Create a new UserProfile
        UserProfile userProfile = new UserProfile();
        user.setUserProfile(userProfile); // Associate UserProfile with UserAuth
        userProfile.setUser(user); // Associate UserAuth with UserProfile

        // Save the UserAuth entity, which should also save UserProfile due to CascadeType.ALL
        UserAuth newUser = userAuthRepository.save(user);

        // Clear the password before returning the response
        newUser.setPassword(null);

        // Return the response
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }




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
