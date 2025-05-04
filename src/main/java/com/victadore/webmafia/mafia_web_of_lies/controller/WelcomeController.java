package com.victadore.webmafia.mafia_web_of_lies.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WelcomeController {

    /**
     * This controller ensures that the Thymeleaf template is returned
     * for all routes that don't match API endpoints.
     * This allows for client-side routing in the SPA.
     */
    @GetMapping(value = {"", "/", "/lobby", "/game/**"})
    public String home() {
        return "index";
    }
}
