package com.seng.comfy.backend.helper;

public class AuthenticationResponse {

    private final String jwt;

    public AuthenticationResponse(String jwt) {
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }

    // Setter is not needed since the JWT is final and set through the constructor
}