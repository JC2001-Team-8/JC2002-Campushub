package com.jc2001.campustrade.entity;

/** Read model for the pickup_points reference table. */
public class PickupPointEntity {
    private Long id, tradeZoneId;
    private String code, name, pointType, description;
    private Integer sortOrder;
    private Boolean active;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getTradeZoneId(){return tradeZoneId;} public void setTradeZoneId(Long v){tradeZoneId=v;}
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getPointType(){return pointType;} public void setPointType(String v){pointType=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public Boolean getActive(){return active;} public void setActive(Boolean v){active=v;}
}
