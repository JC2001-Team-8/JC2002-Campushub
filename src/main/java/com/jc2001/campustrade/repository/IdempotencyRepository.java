package com.jc2001.campustrade.repository;

import com.jc2001.campustrade.entity.IdempotencyRecordEntity;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class IdempotencyRepository {
    private final JdbcTemplate jdbc;
    public IdempotencyRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public boolean claim(Long userId, String key, String endpoint, String requestHash) {
        jdbc.update("DELETE FROM idempotency_records WHERE user_id = ? AND idempotency_key = ? AND expires_at <= now()", userId, key);
        return jdbc.query("INSERT INTO idempotency_records (user_id, idempotency_key, endpoint, request_hash, status, created_by, updated_by) "
                        + "VALUES (?, ?, ?, ?, 'IN_PROGRESS'::idempotency_status, ?, ?) "
                        + "ON CONFLICT (user_id, idempotency_key) DO NOTHING RETURNING id",
                (rs, row) -> rs.getLong(1), userId, key, endpoint, requestHash, userId, userId).size() == 1;
    }

    public Optional<IdempotencyRecordEntity> find(Long userId, String key) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("SELECT * FROM idempotency_records WHERE user_id = ? AND idempotency_key = ?",
                    (rs, row) -> {
                        IdempotencyRecordEntity e=new IdempotencyRecordEntity(); e.setId(rs.getLong("id")); e.setUserId(rs.getLong("user_id"));
                        e.setIdempotencyKey(rs.getString("idempotency_key")); e.setEndpoint(rs.getString("endpoint")); e.setRequestHash(rs.getString("request_hash"));
                        e.setStatus(rs.getString("status")); e.setResponseStatus((Integer) rs.getObject("response_status")); e.setResponseBody(rs.getString("response_body"));
                        Timestamp expiry=rs.getTimestamp("expires_at"); if(expiry!=null)e.setExpiresAt(expiry.toInstant()); return e;
                    }, userId, key));
        } catch (EmptyResultDataAccessException ex) { return Optional.empty(); }
    }

    public void complete(Long userId, String key, String body) {
        jdbc.update("UPDATE idempotency_records SET status = 'COMPLETED'::idempotency_status, response_status = 200, "
                + "response_body = ?::jsonb, version = version + 1, updated_by = ? "
                + "WHERE user_id = ? AND idempotency_key = ? AND status = 'IN_PROGRESS'::idempotency_status",
                body, userId, userId, key);
    }
}
