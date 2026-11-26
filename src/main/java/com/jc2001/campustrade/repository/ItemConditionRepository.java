package com.jc2001.campustrade.repository;

import com.jc2001.campustrade.dto.response.ItemCondition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class ItemConditionRepository {
    private final JdbcTemplate jdbc;
    public ItemConditionRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public List<ItemCondition> findActive(){return jdbc.query("SELECT id,code,name,sort_order FROM item_conditions WHERE is_active=true ORDER BY sort_order,id",(rs,i)->{
        ItemCondition c=new ItemCondition();c.setId(rs.getLong("id"));c.setCode(rs.getString("code"));c.setName(rs.getString("name"));c.setSortOrder(rs.getInt("sort_order"));return c;
    });}
}
