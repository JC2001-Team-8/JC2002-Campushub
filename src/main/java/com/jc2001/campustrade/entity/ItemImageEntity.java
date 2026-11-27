package com.jc2001.campustrade.entity;

/** Persistence row model for an item image. */
public class ItemImageEntity {
    private Long id, itemId; private String imageUrl, contentType; private Integer sortOrder, fileSize;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
    public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public Integer getFileSize(){return fileSize;} public void setFileSize(Integer v){fileSize=v;}
}
