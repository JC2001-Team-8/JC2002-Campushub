package com.jc2001.campustrade.repository;

import com.jc2001.campustrade.dto.response.TradeZone;
import com.jc2001.campustrade.dto.response.ZoneType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class TradeZoneRepository {
    private final JdbcTemplate jdbc;
    public TradeZoneRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<TradeZone> findForPublic(String type, boolean includeInactive) {
        String sql="SELECT id,code,name,zone_type,parent_zone_id,sort_order,is_active FROM trade_zones";
        if(!includeInactive)sql+=" WHERE is_active=true";
        if(type!=null)sql+=(includeInactive?" WHERE ":" AND ")+"zone_type=?::zone_type";
        sql+=" ORDER BY sort_order,id";
        return type==null?jdbc.query(sql,(rs,i)->map(rs)):jdbc.query(sql,(rs,i)->map(rs),type);
    }
    public boolean exists(Long id) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM trade_zones WHERE id = ?)",Boolean.class,id)); }
    private TradeZone map(java.sql.ResultSet rs) throws java.sql.SQLException {
        TradeZone z=new TradeZone(); z.setId(rs.getLong("id")); z.setCode(rs.getString("code")); z.setName(rs.getString("name"));
        z.setZoneType(ZoneType.valueOf(rs.getString("zone_type"))); long p=rs.getLong("parent_zone_id"); if(!rs.wasNull())z.setParentZoneId(p);
        z.setSortOrder(rs.getInt("sort_order")); z.setActive(rs.getBoolean("is_active")); return z;
    }
}
