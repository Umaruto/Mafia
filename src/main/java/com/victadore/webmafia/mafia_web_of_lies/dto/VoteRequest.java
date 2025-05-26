package com.victadore.webmafia.mafia_web_of_lies.dto;

public class VoteRequest {
    private String voterUsername;
    private String targetUsername;  // Can be null or "SKIP"
    private boolean skip;           // New field to explicitly indicate skip

    // Getters and setters
    public String getVoterUsername() {
        return voterUsername;
    }

    public void setVoterUsername(String voterUsername) {
        this.voterUsername = voterUsername;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }
}