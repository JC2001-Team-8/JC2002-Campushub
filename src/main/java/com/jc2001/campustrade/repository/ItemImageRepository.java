package com.jc2001.campustrade.repository;

import com.jc2001.campustrade.entity.ItemImageEntity;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ItemImageRepository {
    private final JdbcTemplate jdbc;
    public ItemImageRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public List<ItemImageEntity> findByItemId(Long itemId){return jdbc.query("SELECT * FROM item_images WHERE item_id=? ORDER BY sort_order,id",(r,n)->{ItemImageEntity x=new ItemImageEntity();x.setId(r.getLong("id"));x.setItemId(r.getLong("item_id"));x.setImageUrl(r.getString("image_url"));x.setSortOrder(r.getInt("sort_order"));x.setFileSize((Integer)r.getObject("file_size"));x.setContentType(r.getString("content_type"));return x;},itemId);}
    public void replace(Long itemId,List<String> imageUrls,Long actorId){jdbc.update("DELETE FROM item_images WHERE item_id=?",itemId);for(int i=0;i<imageUrls.size();i++)jdbc.update("INSERT INTO item_images(item_id,image_url,sort_order,created_by,updated_by) VALUES(?,?,?,?,?)",itemId,imageUrls.get(i),i,actorId,actorId);}
}
