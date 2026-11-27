package com.jc2001.campustrade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jc2001.campustrade.common.DomainException;
import com.jc2001.campustrade.common.ErrorCode;
import com.jc2001.campustrade.entity.IdempotencyRecordEntity;
import com.jc2001.campustrade.repository.IdempotencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.function.Supplier;

/** Shared transactional idempotency ledger for required write endpoints. */
@Service
public class IdempotencyService {
    private final IdempotencyRepository repository;
    private final ObjectMapper mapper;
    public IdempotencyService(IdempotencyRepository repository, ObjectMapper mapper) { this.repository=repository; this.mapper=mapper; }

    @Transactional
    public <T> T execute(Long userId, String key, String endpoint, Object request, Class<T> responseType, Supplier<T> action) {
        if (key == null || key.length() < 8 || key.length() > 64) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key 必须为 8–64 个字符",
                    Collections.singletonMap("field", "Idempotency-Key"));
        }
        String requestHash = hash(request);
        if (!repository.claim(userId, key, endpoint, requestHash)) {
            IdempotencyRecordEntity previous = repository.find(userId, key)
                    .orElseThrow(() -> new DomainException(ErrorCode.CONFLICT));
            if (!endpoint.equals(previous.getEndpoint()) || !requestHash.equals(previous.getRequestHash())) {
                throw new DomainException(ErrorCode.COMMON_IDEMPOTENCY_KEY_REUSED);
            }
            if (!"COMPLETED".equals(previous.getStatus()) || previous.getResponseBody() == null) {
                throw new DomainException(ErrorCode.CONFLICT);
            }
            try { return mapper.readValue(previous.getResponseBody(), responseType); }
            catch (Exception ex) { throw new IllegalStateException("幂等响应快照无法反序列化", ex); }
        }
        T result = action.get();
        try { repository.complete(userId, key, mapper.writeValueAsString(result)); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("无法记录幂等响应", ex); }
        return result;
    }

    private String hash(Object value) {
        try {
            byte[] bytes = mapper.writeValueAsBytes(value);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("请求摘要生成失败", ex);
        }
    }
}
