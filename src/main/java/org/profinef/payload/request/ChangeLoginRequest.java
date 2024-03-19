package org.profinef.payload.request;

import jakarta.validation.constraints.NotBlank;
import org.profinef.payload.response.JwtResponse;

public class ChangeLoginRequest {
    @NotBlank
    private String username;
    @NotBlank
    private JwtResponse user;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public JwtResponse getUser() {
        return user;
    }

    public void setUser(JwtResponse user) {
        this.user = user;
    }
}
