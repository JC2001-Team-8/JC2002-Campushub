package com.jc2001.campustrade.repository;

import com.jc2001.campustrade.dto.response.PickupPoint;
import com.jc2001.campustrade.dto.response.PickupPointType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class PickupPointRepository {
    private final JdbcTemplate jdbc;
    public PickupPointRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public List<PickupPoint> findActiveByZone(Long zoneId,String type){
        String sql="SELECT id,code,name,point_type,trade_zone_id,description,sort_order FROM pickup_points WHERE trade_zone_id=? AND is_active=true";
        if(type!=null)sql+=" AND point_type=?::pickup_point_type"; sql+=" ORDER BY sort_order,id";
        return type==null?jdbc.query(sql,(rs,i)->map(rs),zoneId):jdbc.query(sql,(rs,i)->map(rs),zoneId,type);
    }
    private PickupPoint map(java.sql.ResultSet rs)throws java.sql.SQLException{
        PickupPoint p=new PickupPoint();p.setId(rs.getLong("id"));p.setCode(rs.getString("code"));p.setName(rs.getString("name"));
        p.setPointType(PickupPointType.valueOf(rs.getString("point_type")));p.setTradeZoneId(rs.getLong("trade_zone_id"));
        p.setDescription(rs.getString("description"));p.setSortOrder(rs.getInt("sort_order"));return p;
    }
}
