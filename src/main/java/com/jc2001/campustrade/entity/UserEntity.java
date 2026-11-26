package com.jc2001.campustrade.entity;

import java.time.Instant;

/** Persistence model for the users table. */
public class UserEntity {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String status;
    private Instant restrictedUntil;
    private Instant lastLoginAt;
    private Integer version;
    private Instant createdAt;
    private Boolean inAppEnabled;
    private Boolean emailEnabled;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public String getNickname() { return nickname; }
    public void setNickname(String value) { nickname = value; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String value) { avatarUrl = value; }
    public String getRole() { return role; }
    public void setRole(String value) { role = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Instant getRestrictedUntil() { return restrictedUntil; }
    public void setRestrictedUntil(Instant value) { restrictedUntil = value; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant value) { lastLoginAt = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { version = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public Boolean getInAppEnabled() { return inAppEnabled; }
    public void setInAppEnabled(Boolean value) { inAppEnabled = value; }
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean value) { emailEnabled = value; }
}
