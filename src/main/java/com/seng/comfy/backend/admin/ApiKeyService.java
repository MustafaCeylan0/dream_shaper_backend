package com.seng.comfy.backend.admin;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class ApiKeyService {

    // This could be replaced with a database lookup
    private final Map<String, String> validApiKeys = Collections.singletonMap("4F7E0E31825F589FF70FC046E42577911EEFC5F6024970DB1D5E516F001A4846", "admin");

    public boolean isValidKey(String key) {
        return validApiKeys.containsKey(key) && validApiKeys.get(key).equals("admin");
    }
}