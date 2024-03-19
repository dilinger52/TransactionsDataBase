package org.profinef.payload.response;

import java.util.List;

public class JwtResponse {
  private String accessToken;
  private String tokenType = "Bearer";
  private int id;
  private String username;
  private List<String> roles;

  public JwtResponse(String accessToken, int id, String username, List<String> roles) {
    this.accessToken = accessToken;
    this.id = id;
    this.username = username;
    this.roles = roles;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public List<String> getRoles() {
    return roles;
  }
}
