package com.jc2001.campustrade.repository;

import com.jc2001.campustrade.dto.response.TradeZone;
import com.jc2001.campustrade.dto.response.ZoneType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Collections;
import java.util.List;

@Repository
public class UserTradeZoneRepository {
    private final JdbcTemplate jdbc;
    public UserTradeZoneRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public List<TradeZone> findByUserId(Long userId){return jdbc.query("SELECT z.id,z.code,z.name,z.zone_type,z.parent_zone_id,z.sort_order,z.is_active "
            +"FROM user_trade_zones uz JOIN trade_zones z ON z.id=uz.trade_zone_id WHERE uz.user_id=? ORDER BY z.sort_order,z.id",(rs,row)->{
        TradeZone z=new TradeZone();z.setId(rs.getLong("id"));z.setCode(rs.getString("code"));z.setName(rs.getString("name"));
        z.setZoneType(ZoneType.valueOf(rs.getString("zone_type")));long parent=rs.getLong("parent_zone_id");if(!rs.wasNull())z.setParentZoneId(parent);
        z.setSortOrder(rs.getInt("sort_order"));z.setActive(rs.getBoolean("is_active"));return z;
    },userId);}
    public List<Long> findActiveIds(List<Long> ids){
        if(ids.isEmpty())return Collections.emptyList();String marks=String.join(",",Collections.nCopies(ids.size(),"?"));
        return jdbc.query("SELECT id FROM trade_zones WHERE is_active=true AND id IN ("+marks+")",(rs,row)->rs.getLong(1),ids.toArray());
    }
    public void replace(Long userId,List<Long> ids){
        jdbc.update("DELETE FROM user_trade_zones WHERE user_id=?",userId);
        for(Long id:ids)jdbc.update("INSERT INTO user_trade_zones(user_id,trade_zone_id,created_by,updated_by) VALUES(?,?,?,?)",userId,id,userId,userId);
    }
}
