package com.jc2001.campustrade.entity;

import java.time.Instant;

/** Idempotency ledger row used by write endpoints that require request replay. */
public class IdempotencyRecordEntity {
    private Long id;
    private Long userId;
    private String idempotencyKey;
    private String endpoint;
    private String requestHash;
    private String status;
    private Integer responseStatus;
    private String responseBody;
    private Instant expiresAt;
    public Long getId() { return id; }
    public void setId(Long value) { id=value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId=value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { idempotencyKey=value; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String value) { endpoint=value; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String value) { requestHash=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status=value; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer value) { responseStatus=value; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String value) { responseBody=value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { expiresAt=value; }
}
