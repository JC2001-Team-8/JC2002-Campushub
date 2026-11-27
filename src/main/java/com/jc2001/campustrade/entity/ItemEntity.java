package com.jc2001.campustrade.entity;

import java.math.BigDecimal;
import java.time.Instant;

/** Persistence row model for an item. */
public class ItemEntity {
    private Long id, sellerId, categoryId, conditionId, tradeZoneId;
    private String title, description, status, swapPreference;
    private BigDecimal estimatedPrice;
    private Integer version;
    private Instant publishedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getSellerId(){return sellerId;} public void setSellerId(Long v){sellerId=v;}
    public Long getCategoryId(){return categoryId;} public void setCategoryId(Long v){categoryId=v;}
    public Long getConditionId(){return conditionId;} public void setConditionId(Long v){conditionId=v;}
    public Long getTradeZoneId(){return tradeZoneId;} public void setTradeZoneId(Long v){tradeZoneId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getSwapPreference(){return swapPreference;} public void setSwapPreference(String v){swapPreference=v;}
    public BigDecimal getEstimatedPrice(){return estimatedPrice;} public void setEstimatedPrice(BigDecimal v){estimatedPrice=v;}
    public Integer getVersion(){return version;} public void setVersion(Integer v){version=v;}
    public Instant getPublishedAt(){return publishedAt;} public void setPublishedAt(Instant v){publishedAt=v;}
}
