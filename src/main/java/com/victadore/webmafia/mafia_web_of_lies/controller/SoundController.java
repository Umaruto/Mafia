package com.victadore.webmafia.mafia_web_of_lies.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/sounds")
public class SoundController {
    
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getSound(@PathVariable String filename) {
        try {
            // Check if file exists in static/sounds directory
            Resource resource = new ClassPathResource("static/sounds/" + filename);
            
            if (!resource.exists()) {
                // Return 404 instead of 500 for missing files
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type based on file extension
            String contentType = "audio/mpeg"; // Default to MP3
            if (filename.toLowerCase().endsWith(".wav")) {
                contentType = "audio/wav";
            } else if (filename.toLowerCase().endsWith(".ogg")) {
                contentType = "audio/ogg";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
            headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=3600"); // Cache for 1 hour
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            // Log the error but return 404 to client
            System.err.println("Error serving sound file " + filename + ": " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Sound controller is working. Available sounds should be placed in src/main/resources/static/sounds/");
    }
} 