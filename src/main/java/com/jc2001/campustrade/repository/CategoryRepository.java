package com.jc2001.campustrade.repository;

import com.jc2001.campustrade.dto.response.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class CategoryRepository {
    private final JdbcTemplate jdbc;
    public CategoryRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public List<Category> findActive(){return jdbc.query("SELECT id,code,name,sort_order FROM categories WHERE is_active=true ORDER BY sort_order,id",(rs,i)->{
        Category c=new Category();c.setId(rs.getLong("id"));c.setCode(rs.getString("code"));c.setName(rs.getString("name"));c.setSortOrder(rs.getInt("sort_order"));return c;
    });}
}
