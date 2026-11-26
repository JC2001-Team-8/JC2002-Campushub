package com.jc2001.campustrade.repository;

import com.jc2001.campustrade.entity.UserEntity;
import com.jc2001.campustrade.dto.response.TradeZone;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;
    private final UserTradeZoneRepository tradeZones;
    private static final RowMapper<UserEntity> USER = UserRepository::mapUser;

    public UserRepository(JdbcTemplate jdbc, UserTradeZoneRepository tradeZones) { this.jdbc = jdbc; this.tradeZones=tradeZones; }

    public Optional<UserEntity> findByUsername(String username) {
        return one("SELECT * FROM users WHERE username = ?", username);
    }

    public Optional<UserEntity> findById(Long id) {
        return one("SELECT * FROM users WHERE id = ?", id);
    }

    private Optional<UserEntity> one(String sql, Object arg) {
        try { return Optional.ofNullable(jdbc.queryForObject(sql, USER, arg)); }
        catch (EmptyResultDataAccessException ex) { return Optional.empty(); }
    }

    public UserEntity create(String username, String email, String passwordHash, String nickname) {
        Long id = jdbc.queryForObject("INSERT INTO users (username, email, password_hash, nickname) "
                + "VALUES (?, ?, ?, ?) RETURNING id", Long.class, username, email, passwordHash, nickname);
        return findById(id).orElseThrow(IllegalStateException::new);
    }

    public boolean updateProfile(Long id, Integer version, String nickname, String avatarUrl) {
        return jdbc.update("UPDATE users SET nickname = ?, avatar_url = ?, version = version + 1 WHERE id = ? AND version = ?",
                nickname, avatarUrl, id, version) == 1;
    }

    public boolean changePassword(Long id, Integer version, String passwordHash) {
        return jdbc.update("UPDATE users SET password_hash = ?, version = version + 1 WHERE id = ? AND version = ?",
                passwordHash, id, version) == 1;
    }

    public boolean updateNotificationSettings(Long id, Boolean inAppEnabled) {
        return jdbc.update("UPDATE users SET in_app_enabled = ?, version = version + 1 WHERE id = ?", inAppEnabled, id) == 1;
    }

    public boolean bumpVersion(Long id, Integer version) {
        return jdbc.update("UPDATE users SET version = version + 1 WHERE id = ? AND version = ?", id, version) == 1;
    }

    public void recordLogin(Long id) {
        jdbc.update("UPDATE users SET last_login_at = now() WHERE id = ?", id);
    }

    public java.util.List<TradeZone> findTradeZones(Long userId) { return tradeZones.findByUserId(userId); }
    public void replaceTradeZones(Long userId, java.util.List<Long> ids) { tradeZones.replace(userId, ids); }
    public java.util.List<Long> findActiveZoneIds(java.util.List<Long> ids) { return tradeZones.findActiveIds(ids); }

    private static UserEntity mapUser(ResultSet rs, int row) throws SQLException {
        UserEntity u = new UserEntity();
        u.setId(rs.getLong("id")); u.setUsername(rs.getString("username")); u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash")); u.setNickname(rs.getString("nickname"));
        u.setAvatarUrl(rs.getString("avatar_url")); u.setRole(rs.getString("role")); u.setStatus(rs.getString("status"));
        Timestamp restricted = rs.getTimestamp("restricted_until"); if (restricted != null) u.setRestrictedUntil(restricted.toInstant());
        Timestamp login = rs.getTimestamp("last_login_at"); if (login != null) u.setLastLoginAt(login.toInstant());
        u.setVersion(rs.getInt("version")); Timestamp created = rs.getTimestamp("created_at");
        if (created != null) u.setCreatedAt(created.toInstant());
        u.setInAppEnabled(rs.getBoolean("in_app_enabled")); u.setEmailEnabled(rs.getBoolean("email_enabled"));
        return u;
    }
}
