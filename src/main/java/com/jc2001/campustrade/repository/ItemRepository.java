package com.jc2001.campustrade.repository;

import com.jc2001.campustrade.entity.ItemEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ItemRepository {
    private final JdbcTemplate jdbc;
    public ItemRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private static final RowMapper<ItemEntity> MAPPER=ItemRepository::map;
    public ItemEntity findById(Long id){try{return jdbc.queryForObject("SELECT * FROM items WHERE id=? AND deleted_at IS NULL",MAPPER,id);}catch(EmptyResultDataAccessException e){return null;}}
    @Transactional public boolean occupyForOrder(Long itemId,Long orderId,Long actorId){return occupy(itemId,orderId,"ORDER",actorId);}
    @Transactional public boolean occupyForSwap(Long itemId,Long proposalId,Long actorId){return occupy(itemId,proposalId,"SWAP",actorId);}
    private boolean occupy(Long itemId,Long refId,String type,Long actorId){
        String target="ORDER".equals(type)?"RESERVED":"SWAPPING";
        if(jdbc.update("UPDATE items SET status=?::item_status,version=version+1,updated_by=? WHERE id=? AND status='IN_SALE' AND deleted_at IS NULL",target,actorId,itemId)!=1)return false;
        jdbc.update("INSERT INTO item_occupations(item_id,ref_type,ref_id,created_by,updated_by) VALUES (?,?::occupation_type,?,?,?)",itemId,type,refId,actorId,actorId);
        return true;
    }
    @Transactional public boolean releaseOccupation(Long itemId,String type,Long refId,String reason,Long actorId){
        int n=jdbc.update("UPDATE item_occupations SET released_at=now(),release_reason=?,version=version+1,updated_by=? WHERE item_id=? AND ref_type=?::occupation_type AND ref_id=? AND released_at IS NULL",reason,actorId,itemId,type,refId);
        if(n==0)return false;
        jdbc.update("UPDATE items SET status='IN_SALE',version=version+1,updated_by=? WHERE id=? AND status IN ('RESERVED','SWAPPING')",actorId,itemId);
        return true;
    }
    private static ItemEntity map(ResultSet r,int row)throws SQLException{ItemEntity x=new ItemEntity();x.setId(r.getLong("id"));x.setSellerId(r.getLong("seller_id"));x.setCategoryId(r.getLong("category_id"));x.setConditionId(r.getLong("condition_id"));x.setTradeZoneId(r.getLong("trade_zone_id"));x.setTitle(r.getString("title"));x.setDescription(r.getString("description"));x.setStatus(r.getString("status"));x.setSwapPreference(r.getString("swap_preference"));x.setEstimatedPrice(r.getBigDecimal("estimated_price"));x.setVersion(r.getInt("version"));if(r.getTimestamp("published_at")!=null)x.setPublishedAt(r.getTimestamp("published_at").toInstant());return x;}
}
