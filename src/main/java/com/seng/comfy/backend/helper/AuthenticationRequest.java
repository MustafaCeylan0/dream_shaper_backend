package com.seng.comfy.backend.helper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationRequest {

    private String username;
    private String password;

    // Constructors, getters, and setters are omitted for brevity

    // Default constructor for JSON parsing
    public AuthenticationRequest() {}


}