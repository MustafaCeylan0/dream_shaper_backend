package com.seng.comfy.backend.helper;

import lombok.Getter;

import java.util.Optional;

public class GenerateRequest {
    @Getter
    private String prompt;
    private Optional<String> negativePrompt = Optional.empty();
    private Optional<String> model = Optional.empty();
    private Optional<String> ratio = Optional.empty();
    private Optional<Integer> resolution = Optional.empty();


    public String getNegativePrompt() {
        return negativePrompt.orElse("worst quality, low quality, bad quality");
    }

    public String getModel() {
        return model.orElse("sd_xl_base_1.0.safetensors");
    }

    public String getRatio() {
        return ratio.orElse("1:1");
    }

    public Integer getResolution() {
        return resolution.orElse(512);
    }


}
