package com.victadore.webmafia.mafia_web_of_lies.config;

import com.victadore.webmafia.mafia_web_of_lies.service.GameLogicService;
import com.victadore.webmafia.mafia_web_of_lies.service.PhaseTimerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class ServiceConfiguration {
    
    @Autowired
    private GameLogicService gameLogicService;
    
    @Autowired
    private PhaseTimerService phaseTimerService;
    
    @PostConstruct
    public void setupDependencies() {
        // Set up the circular dependency after both beans are created
        gameLogicService.setPhaseTimerService(phaseTimerService);
    }
} 