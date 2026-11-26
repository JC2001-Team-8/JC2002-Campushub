package com.jc2001.campustrade.entity;

/** Read model for the trade_zones reference table. */
public class TradeZoneEntity {
    private Long id, parentZoneId;
    private String code, name, zoneType;
    private Integer sortOrder;
    private Boolean active;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getParentZoneId(){return parentZoneId;} public void setParentZoneId(Long v){parentZoneId=v;}
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getZoneType(){return zoneType;} public void setZoneType(String v){zoneType=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public Boolean getActive(){return active;} public void setActive(Boolean v){active=v;}
}
