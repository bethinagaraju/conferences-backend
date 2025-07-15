package com.zn.dto;

public class LoginResponseDTO {
    private String token;
    private String tokenType = "Bearer";
    private AdminResponseDTO admin;

    public LoginResponseDTO() {}

    public LoginResponseDTO(String token, AdminResponseDTO admin) {
        this.token = token;
        this.admin = admin;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public AdminResponseDTO getAdmin() {
        return admin;
    }

    public void setAdmin(AdminResponseDTO admin) {
        this.admin = admin;
    }
}
