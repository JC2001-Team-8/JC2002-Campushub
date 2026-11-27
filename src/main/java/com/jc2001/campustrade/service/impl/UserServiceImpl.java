package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.common.DomainException;
import com.jc2001.campustrade.common.ErrorCode;
import com.jc2001.campustrade.dto.request.NotificationSettings;
import com.jc2001.campustrade.dto.request.UpdateProfileRequest;
import com.jc2001.campustrade.dto.request.UserTradeZonesRequest;
import com.jc2001.campustrade.dto.response.CurrentUserResponse;
import com.jc2001.campustrade.dto.response.UserProfile;
import com.jc2001.campustrade.entity.UserEntity;
import com.jc2001.campustrade.repository.UserRepository;
import com.jc2001.campustrade.security.AuthenticatedUser;
import com.jc2001.campustrade.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    public UserServiceImpl(UserRepository users) { this.users = users; }

    @Override @Transactional
    public CurrentUserResponse updateMyProfile(UpdateProfileRequest body) {
        UserEntity user = currentUser();
        if ("RESTRICTED".equals(user.getStatus())) {
            throw new DomainException(ErrorCode.USER_RESTRICTED_CANNOT_WRITE,
                    ErrorCode.USER_RESTRICTED_CANNOT_WRITE.getDefaultMessage(),
                    Collections.singletonMap("restrictedUntil", user.getRestrictedUntil()));
        }
        String nickname = body.isNicknamePresent() ? body.getNickname() : user.getNickname();
        String avatar = body.isAvatarUrlPresent() ? body.getAvatarUrl() : user.getAvatarUrl();
        if (nickname == null || nickname.trim().isEmpty() || nickname.length() > 20) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR);
        }
        if (!users.updateProfile(user.getId(), user.getVersion(), nickname, avatar)) {
            throw new DomainException(ErrorCode.CONFLICT);
        }
        return currentResponse(users.findById(user.getId()).orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND)));
    }

    @Override @Transactional
    public CurrentUserResponse setMyTradeZones(UserTradeZonesRequest body) {
        UserEntity user = currentUser();
        List<Long> ids = body.getTradeZoneIds();
        if (ids == null || ids.isEmpty() || ids.size() > 5 || new HashSet<>(ids).size() != ids.size()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR);
        }
        List<Long> activeIds = users.findActiveZoneIds(ids);
        if (activeIds.size() != ids.size()) throw new DomainException(ErrorCode.TRADE_ZONE_NOT_FOUND);
        if (!users.bumpVersion(user.getId(), user.getVersion())) throw new DomainException(ErrorCode.CONFLICT);
        users.replaceTradeZones(user.getId(), ids);
        return currentResponse(users.findById(user.getId()).orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND)));
    }

    @Override public NotificationSettings getMyNotificationSettings() {
        UserEntity user = currentUser();
        return settings(user);
    }

    @Override @Transactional
    public NotificationSettings updateMyNotificationSettings(NotificationSettings body) {
        UserEntity user = currentUser();
        if (body.getEmailEnabled() != null && body.getEmailEnabled()) {
            // Email delivery is explicitly not enabled in this iteration.
            body.setEmailEnabled(false);
        }
        if (!users.updateNotificationSettings(user.getId(), body.getInAppEnabled())) {
            throw new DomainException(ErrorCode.USER_NOT_FOUND);
        }
        return settings(users.findById(user.getId()).orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND)));
    }

    @Override public UserProfile getPublicProfile(Long userId) {
        UserEntity user = users.findById(userId).orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND));
        UserProfile profile = new UserProfile();
        profile.setId(user.getId()); profile.setNickname(user.getNickname()); profile.setAvatarUrl(user.getAvatarUrl());
        profile.setCreatedAt(user.getCreatedAt());
        return profile;
    }

    private NotificationSettings settings(UserEntity user) {
        NotificationSettings s = new NotificationSettings();
        s.setInAppEnabled(user.getInAppEnabled()); s.setEmailEnabled(Boolean.FALSE);
        return s;
    }

    private CurrentUserResponse currentResponse(UserEntity user) {
        CurrentUserResponse dto = new CurrentUserResponse();
        dto.setId(user.getId()); dto.setUsername(user.getUsername()); dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname()); dto.setAvatarUrl(user.getAvatarUrl());
        dto.setRole(com.jc2001.campustrade.dto.response.UserRole.valueOf(user.getRole()));
        dto.setStatus(com.jc2001.campustrade.dto.response.UserStatus.valueOf(user.getStatus()));
        dto.setRestrictedUntil(user.getRestrictedUntil()); dto.setVersion(user.getVersion()); dto.setCreatedAt(user.getCreatedAt());
        dto.setTradeZones(users.findTradeZones(user.getId()));
        return dto;
    }

    private UserEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser)) throw new DomainException(ErrorCode.UNAUTHORIZED);
        Long id = ((AuthenticatedUser) auth.getPrincipal()).getUserId();
        return users.findById(id).orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND));
    }
}
