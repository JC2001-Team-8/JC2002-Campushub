package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.common.DomainException;
import com.jc2001.campustrade.common.ErrorCode;
import com.jc2001.campustrade.dto.request.*;
import com.jc2001.campustrade.dto.response.*;
import com.jc2001.campustrade.entity.UserEntity;
import com.jc2001.campustrade.repository.UserRepository;
import com.jc2001.campustrade.security.AuthenticatedUser;
import com.jc2001.campustrade.security.JwtTokenProvider;
import com.jc2001.campustrade.service.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtTokenProvider tokens;

    public AuthServiceImpl(UserRepository users, PasswordEncoder passwords, JwtTokenProvider tokens) {
        this.users = users; this.passwords = passwords; this.tokens = tokens;
    }

    @Override @Transactional
    public CurrentUserResponse registerUser(RegisterRequest body) {
        validatePassword(body.getPassword());
        UserEntity user;
        try {
            user = users.create(body.getUsername().trim(), blankToNull(body.getEmail()),
                    passwords.encode(body.getPassword()), body.getNickname().trim());
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(ErrorCode.AUTH_USERNAME_TAKEN);
        }
        // Frozen OpenAPI contract returns CurrentUserResponse and explicitly requires a separate login.
        return currentResponse(user);
    }

    @Override @Transactional
    public AuthTokenResponse login(LoginRequest body) {
        UserEntity user = users.findByUsername(body.getUsername())
                .filter(u -> passwords.matches(body.getPassword(), u.getPasswordHash()))
                .orElseThrow(() -> new DomainException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        users.recordLogin(user.getId());
        return tokenResponse(user);
    }

    @Override public AuthTokenResponse refreshToken(RefreshTokenRequest body) {
        throw new DomainException(ErrorCode.COMMON_NOT_IMPLEMENTED);
    }

    @Override public Void logout() {
        // The API is stateless and the frozen contract makes server-side blacklist optional.
        return null;
    }

    @Override public CurrentUserResponse getCurrentUser() {
        return currentResponse(authenticatedUser());
    }

    @Override @Transactional
    public Void changePassword(ChangePasswordRequest body) {
        UserEntity user = authenticatedUser();
        if (!passwords.matches(body.getOldPassword(), user.getPasswordHash())) {
            throw new DomainException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        validatePassword(body.getNewPassword());
        if (!users.changePassword(user.getId(), user.getVersion(), passwords.encode(body.getNewPassword()))) {
            throw new DomainException(ErrorCode.CONFLICT);
        }
        return null;
    }

    private void validatePassword(String value) {
        if (value == null || value.length() < 8 || value.length() > 64
                || !value.matches(".*[A-Za-z].*") || !value.matches(".*[0-9].*")) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "密码至少 8 位，且需包含字母和数字");
        }
    }

    private AuthenticatedUser principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser)) throw new DomainException(ErrorCode.UNAUTHORIZED);
        return (AuthenticatedUser) auth.getPrincipal();
    }

    private UserEntity authenticatedUser() {
        return users.findById(principal().getUserId()).orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND));
    }

    private CurrentUserResponse currentResponse(UserEntity user) {
        CurrentUserResponse dto = new CurrentUserResponse();
        dto.setId(user.getId()); dto.setUsername(user.getUsername()); dto.setEmail(user.getEmail()); dto.setNickname(user.getNickname());
        dto.setAvatarUrl(user.getAvatarUrl()); dto.setRole(UserRole.valueOf(user.getRole()));
        dto.setStatus(UserStatus.valueOf(user.getStatus())); dto.setRestrictedUntil(user.getRestrictedUntil());
        dto.setVersion(user.getVersion()); dto.setCreatedAt(user.getCreatedAt()); dto.setTradeZones(users.findTradeZones(user.getId()));
        return dto;
    }

    private AuthTokenResponse tokenResponse(UserEntity user) {
        String jwt = tokens.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getStatus());
        AuthTokenResponse dto = new AuthTokenResponse(); dto.setAccessToken(jwt); dto.setTokenType("Bearer");
        dto.setExpiresIn(tokens.getExpirationSeconds()); dto.setExpiresAt(tokens.getExpiresAt(jwt));
        UserProfile profile = new UserProfile(); profile.setId(user.getId()); profile.setNickname(user.getNickname());
        profile.setAvatarUrl(user.getAvatarUrl()); profile.setCreatedAt(user.getCreatedAt()); dto.setUser(profile);
        return dto;
    }

    private String blankToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
