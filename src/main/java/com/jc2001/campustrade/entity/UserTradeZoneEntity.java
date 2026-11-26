package com.jc2001.campustrade.entity;

/** Persistence model for a user's selected trade zone. */
public class UserTradeZoneEntity {
    private Long id, userId, tradeZoneId, createdBy, updatedBy;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public Long getTradeZoneId(){return tradeZoneId;} public void setTradeZoneId(Long v){tradeZoneId=v;}
    public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;}
    public Long getUpdatedBy(){return updatedBy;} public void setUpdatedBy(Long v){updatedBy=v;}
}
