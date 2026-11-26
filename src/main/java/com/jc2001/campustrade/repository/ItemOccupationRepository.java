package com.jc2001.campustrade.repository;

import com.jc2001.campustrade.entity.ItemOccupationEntity;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ItemOccupationRepository {
    private final JdbcTemplate jdbc;
    public ItemOccupationRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public List<ItemOccupationEntity> findActiveByItemId(Long itemId){return jdbc.query("SELECT * FROM item_occupations WHERE item_id=? AND released_at IS NULL",(r,n)->{ItemOccupationEntity x=new ItemOccupationEntity();x.setId(r.getLong("id"));x.setItemId(r.getLong("item_id"));x.setReferenceType(r.getString("ref_type"));x.setReferenceId(r.getLong("ref_id"));if(r.getTimestamp("occupied_at")!=null)x.setOccupiedAt(r.getTimestamp("occupied_at").toInstant());return x;},itemId);}
}
