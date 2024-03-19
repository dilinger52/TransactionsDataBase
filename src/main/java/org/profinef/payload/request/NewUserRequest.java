package org.profinef.payload.request;

import jakarta.validation.constraints.NotBlank;

public class NewUserRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
