package org.profinef.payload.request;

import jakarta.validation.constraints.NotBlank;
import org.profinef.payload.response.JwtResponse;

public class ChangePasswordRequest {
    @NotBlank
    private String oldPassword;
    @NotBlank
    private String newPassword;
    @NotBlank
    private JwtResponse user;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public JwtResponse getUser() {
        return user;
    }

    public void setUser(JwtResponse user) {
        this.user = user;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
