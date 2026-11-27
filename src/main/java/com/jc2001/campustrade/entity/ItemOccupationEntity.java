package com.jc2001.campustrade.entity;

import java.time.Instant;

/** Persistence row model for the exclusive order/swap occupation ledger. */
public class ItemOccupationEntity {
    private Long id, itemId, referenceId; private String referenceType, releaseReason; private Instant occupiedAt, releasedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    public Long getReferenceId(){return referenceId;} public void setReferenceId(Long v){referenceId=v;}
    public String getReferenceType(){return referenceType;} public void setReferenceType(String v){referenceType=v;}
    public String getReleaseReason(){return releaseReason;} public void setReleaseReason(String v){releaseReason=v;}
    public Instant getOccupiedAt(){return occupiedAt;} public void setOccupiedAt(Instant v){occupiedAt=v;}
    public Instant getReleasedAt(){return releasedAt;} public void setReleasedAt(Instant v){releasedAt=v;}
}
