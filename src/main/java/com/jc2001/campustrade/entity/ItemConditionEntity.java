package com.jc2001.campustrade.entity;

/** Read model for the item_conditions reference table. */
public class ItemConditionEntity {
    private Long id;
    private String code, name;
    private Integer sortOrder;
    private Boolean active;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public Boolean getActive(){return active;} public void setActive(Boolean v){active=v;}
}
