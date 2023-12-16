package com.seng.comfy.backend.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

public class TxtToImgRequest {
    String jsonConfig;
    String positivePrompt;


    public TxtToImgRequest(File jsonConfig, String positivePrompt) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonConfig);

        // Navigate to the "6" -> "inputs" -> "text" path in the JSON structure
        ((ObjectNode) root.path("6").path("inputs")).put("text", positivePrompt);

        // Now root contains the modified JSON object
        this.jsonConfig = root.toString(); // Consider storing the JsonNode or its string representation instead
        this.positivePrompt = positivePrompt;

        // Optionally, write the modified JSON back to the file or keep it in memory
        // mapper.writeValue(jsonFile, root);

    }

    public String getJsonConfig() {
        return jsonConfig;
    }

    public void setJsonConfig(String jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    public String getPositivePrompt() {
        return positivePrompt;
    }

    public void setPositivePrompt(String positivePrompt) {
        this.positivePrompt = positivePrompt;
    }

}
