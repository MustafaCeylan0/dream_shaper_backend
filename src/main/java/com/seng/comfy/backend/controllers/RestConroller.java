package com.seng.comfy.backend.controllers;


import com.seng.comfy.backend.service.ComfyUiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// Plus any additional imports for your comfyUiService and other components

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
@RestController
public class RestConroller {

    private ComfyUiService comfyUiService;

    @Autowired
    public void RestController(ComfyUiService comfyUiService) {
        this.comfyUiService = comfyUiService;
    }

    public RestConroller(ComfyUiService comfyUiService) {
        this.comfyUiService = comfyUiService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> queuePrompt(@RequestParam String prompt) {
        try {
            String filename = comfyUiService.queuePrompt(prompt); // Assume this returns the filename of the generated image
            System.out.println("Filename is : " + filename);
            String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/images/")
                    .path(filename)
                    .toUriString();

            return ResponseEntity.ok("Image generated successfully. Download at: " + downloadUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error queuing prompt: " + e.getMessage());
        }
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> downloadImage(@PathVariable String filename) throws MalformedURLException {
        String imagesDirPath = "src/main/resources/static/images/";
        Path filePath = Paths.get(imagesDirPath).resolve(filename).normalize();

        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
