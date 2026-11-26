package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.common.*;
import com.jc2001.campustrade.dto.request.*;
import com.jc2001.campustrade.dto.response.*;
import com.jc2001.campustrade.entity.UserEntity;
import com.jc2001.campustrade.repository.UserRepository;
import com.jc2001.campustrade.service.IdempotencyService;
import com.jc2001.campustrade.service.ItemService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemServiceImpl implements ItemService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final IdempotencyService idempotency;
    private static final String SELECT = "SELECT i.*, c.code category_code,c.name category_name,c.sort_order category_sort, "
            + "ic.code condition_code,ic.name condition_name,ic.sort_order condition_sort,z.code zone_code,z.name zone_name, "
            + "u.nickname seller_nickname,u.avatar_url seller_avatar, (SELECT image_url FROM item_images im WHERE im.item_id=i.id ORDER BY sort_order,id LIMIT 1) cover "
            + "FROM items i JOIN categories c ON c.id=i.category_id JOIN item_conditions ic ON ic.id=i.condition_id "
            + "JOIN trade_zones z ON z.id=i.trade_zone_id JOIN users u ON u.id=i.seller_id ";

    public ItemServiceImpl(JdbcTemplate jdbc, UserRepository users, IdempotencyService idempotency) {
        this.jdbc=jdbc; this.users=users; this.idempotency=idempotency;
    }

    @Override public com.jc2001.campustrade.common.PageResult<ItemSummary> list(Integer page,Integer size,List<String> sort,Long categoryId,Long conditionId,
            Long zoneId,String keyword,DecimalString min,DecimalString max,SwapPreference preference,Long sellerId,
            Long currentUser,List<ItemStatus> statuses) {
        int p=page==null?0:page, n=size==null?20:size;
        if(p<0||n<1||n>100) throw new DomainException(ErrorCode.VALIDATION_ERROR);
        String order="i.published_at DESC,i.id DESC";
        if(sort!=null&&!sort.isEmpty()) {
            List<String> terms=new ArrayList<>();
            for(String s:sort){String[] a=s.split(","); String field=a[0];
                if(!Arrays.asList("published_at","estimated_price").contains(field)) throw new DomainException(ErrorCode.COMMON_INVALID_SORT_FIELD);
                String direction=a.length>1?a[1].toUpperCase(Locale.ROOT):"ASC";
                if(!direction.equals("ASC")&&!direction.equals("DESC")) throw new DomainException(ErrorCode.VALIDATION_ERROR);
                terms.add("i."+field+" "+direction);
            } order=String.join(",",terms);
        }
        StringBuilder where=new StringBuilder(" WHERE i.deleted_at IS NULL "); List<Object> args=new ArrayList<>();
        boolean mine=currentUser!=null;
        if(mine){where.append(" AND i.seller_id=?");args.add(currentUser);if(statuses!=null&&!statuses.isEmpty()){where.append(" AND i.status::text IN (");for(int i=0;i<statuses.size();i++){if(i>0)where.append(',');where.append('?');args.add(statuses.get(i).name());}where.append(')');}}
        else where.append(" AND i.status='IN_SALE'");
        if(categoryId!=null){where.append(" AND i.category_id=?");args.add(categoryId);} if(conditionId!=null){where.append(" AND i.condition_id=?");args.add(conditionId);}
        if(zoneId!=null){where.append(" AND i.trade_zone_id=?");args.add(zoneId);} if(keyword!=null&&!keyword.trim().isEmpty()){where.append(" AND lower(i.title) LIKE ?");args.add("%"+keyword.trim().toLowerCase(Locale.ROOT)+"%");}
        if(min!=null){where.append(" AND i.estimated_price>=?::numeric");args.add(min.getValue());} if(max!=null){where.append(" AND i.estimated_price<=?::numeric");args.add(max.getValue());}
        if(preference!=null){where.append(" AND i.swap_preference=?::swap_preference");args.add(preference.name());} if(sellerId!=null){where.append(" AND i.seller_id=?");args.add(sellerId);}
        Long total=jdbc.queryForObject("SELECT count(*) FROM items i"+where,Long.class,args.toArray());
        args.add(n);args.add(p*n);
        List<ItemSummary> rows=jdbc.query(SELECT+where+" ORDER BY "+order+" LIMIT ? OFFSET ?", summaryMapper(),args.toArray());
        return com.jc2001.campustrade.common.PageResult.of(p,n,total==null?0:total,rows);
    }

    @Override @Transactional public ItemDetail create(Long userId,String key,CreateItemRequest request) {
        return idempotency.execute(userId,key,"createItem",request,ItemDetail.class,()->createOnce(userId,request));
    }
    private ItemDetail createOnce(Long userId,CreateItemRequest r){
        UserEntity user=users.findById(userId).orElseThrow(()->new DomainException(ErrorCode.USER_NOT_FOUND));
        if("RESTRICTED".equals(user.getStatus())) throw new DomainException(ErrorCode.USER_RESTRICTED_CANNOT_WRITE);
        if(!users.findActiveZoneIds(Collections.singletonList(r.getTradeZoneId())).contains(r.getTradeZoneId())) throw new DomainException(ErrorCode.TRADE_ZONE_NOT_FOUND);
        if(!users.findTradeZones(userId).stream().anyMatch(z->z.getId().equals(r.getTradeZoneId()))) throw new DomainException(ErrorCode.ITEM_TRADE_ZONE_NOT_SELECTED);
        if(r.getImageUrls()==null||r.getImageUrls().isEmpty()||r.getImageUrls().size()>9) throw new DomainException(ErrorCode.VALIDATION_ERROR);
        validateImageUrls(r.getImageUrls());
        Long id=jdbc.queryForObject("INSERT INTO items(seller_id,category_id,condition_id,trade_zone_id,title,description,estimated_price,status,swap_preference,published_at,created_by) VALUES(?,?,?,?,?,?,?::numeric,'IN_SALE',?::swap_preference,now(),?) RETURNING id",Long.class,userId,r.getCategoryId(),r.getConditionId(),r.getTradeZoneId(),r.getTitle(),r.getDescription(),r.getEstimatedPrice().getValue(),r.getSwapPreference()==null?SwapPreference.NOT_ACCEPTING.name():r.getSwapPreference().name(),userId);
        for(int i=0;i<r.getImageUrls().size();i++) jdbc.update("INSERT INTO item_images(item_id,image_url,sort_order,created_by) VALUES(?,?,?,?)",id,r.getImageUrls().get(i),i,userId);
        return detail(id,userId);
    }
    @Override public ItemDetail detail(Long id,Long viewerId){return jdbc.query(SELECT+" WHERE i.id=? AND i.deleted_at IS NULL", rs->{if(!rs.next())throw new DomainException(ErrorCode.ITEM_NOT_FOUND);return detailFrom(rs);},id);}
    @Override @Transactional public ItemDetail update(Long uid,Long id,UpdateItemRequest r){
        ensureWritable(uid); Map<String,Object> item=ownedItem(uid,id); String status=(String)item.get("status"); int ver=r.getVersion()==null?-1:r.getVersion();
        if(Arrays.asList("RESERVED","SWAPPING").contains(status)&&(r.getTitle()!=null||r.getCategoryId()!=null||r.getConditionId()!=null||r.getEstimatedPrice()!=null||r.getTradeZoneId()!=null)) throw new DomainException(ErrorCode.ITEM_LOCKED_BY_OCCUPATION);
        if(Arrays.asList("SOLD","BANNED").contains(status))throw new DomainException(ErrorCode.ITEM_STATUS_NOT_EDITABLE);
        if(ver<0)throw new DomainException(ErrorCode.VALIDATION_ERROR);
        int updated=jdbc.update("UPDATE items SET title=COALESCE(?,title),description=?,category_id=COALESCE(?,category_id),condition_id=COALESCE(?,condition_id),trade_zone_id=COALESCE(?,trade_zone_id),estimated_price=COALESCE(?::numeric,estimated_price),version=version+1,updated_by=? WHERE id=? AND version=?",r.getTitle(),r.getDescription(),r.getCategoryId(),r.getConditionId(),r.getTradeZoneId(),r.getEstimatedPrice()==null?null:r.getEstimatedPrice().getValue(),uid,id,ver);
        if(updated==0)throw new DomainException(ErrorCode.CONFLICT);
        if(r.getImageUrls()!=null){if(r.getImageUrls().isEmpty()||r.getImageUrls().size()>9)throw new DomainException(ErrorCode.VALIDATION_ERROR);validateImageUrls(r.getImageUrls());jdbc.update("DELETE FROM item_images WHERE item_id=?",id);for(int i=0;i<r.getImageUrls().size();i++)jdbc.update("INSERT INTO item_images(item_id,image_url,sort_order,created_by) VALUES(?,?,?,?)",id,r.getImageUrls().get(i),i,uid);}
        return detail(id,uid);
    }
    @Override @Transactional public ItemDetail shelf(Long uid,Long id){ensureWritable(uid);ownedItem(uid,id);int n=jdbc.update("UPDATE items SET status='OFF_SHELF',version=version+1 WHERE id=? AND seller_id=? AND status='IN_SALE' AND NOT EXISTS(SELECT 1 FROM item_occupations o WHERE o.item_id=items.id AND o.released_at IS NULL)",id,uid);if(n==0)throw new DomainException(ErrorCode.ITEM_LOCKED_BY_OCCUPATION);return detail(id,uid);}
    @Override @Transactional public ItemDetail relist(Long uid,Long id){ensureWritable(uid);Map<String,Object> i=ownedItem(uid,id);if("BANNED".equals(i.get("status")))throw new DomainException(ErrorCode.ITEM_BANNED_CANNOT_RELIST);int n=jdbc.update("UPDATE items SET status='IN_SALE',published_at=now(),version=version+1 WHERE id=? AND seller_id=? AND status='OFF_SHELF' AND NOT EXISTS(SELECT 1 FROM item_occupations o WHERE o.item_id=items.id AND o.released_at IS NULL)",id,uid);if(n==0)throw new DomainException(ErrorCode.ITEM_LOCKED_BY_OCCUPATION);return detail(id,uid);}
    @Override @Transactional public ItemDetail preference(Long uid,Long id,UpdateSwapPreferenceRequest r){ensureWritable(uid);Map<String,Object> i=ownedItem(uid,id);String s=(String)i.get("status");if(!"IN_SALE".equals(s)&&r.getSwapPreference()==SwapPreference.ACCEPTING)throw new DomainException(ErrorCode.ITEM_STATUS_NOT_SWAPPABLE);int version=r.getVersion()==null?(Integer)i.get("version"):r.getVersion();int n=jdbc.update("UPDATE items SET swap_preference=?::swap_preference,version=version+1 WHERE id=? AND seller_id=? AND version=?",r.getSwapPreference().name(),id,uid,version);if(n==0)throw new DomainException(ErrorCode.CONFLICT);return detail(id,uid);}
    @Override public Map<String,Object> occupation(Long uid,Long id,boolean admin){Map<String,Object> i=item(id);if(!admin&&!Objects.equals(uid,i.get("seller_id")))throw new DomainException(ErrorCode.FORBIDDEN);List<Map<String,Object>> rows=jdbc.queryForList("SELECT ref_type,ref_id,occupied_at FROM item_occupations WHERE item_id=? AND released_at IS NULL",id);Map<String,Object> occ=null;if(!rows.isEmpty()){Map<String,Object> row=rows.get(0);occ=new LinkedHashMap<>();occ.put("refType",row.get("ref_type"));occ.put("refId",row.get("ref_id"));occ.put("occupiedAt",row.get("occupied_at"));}Map<String,Object> out=new LinkedHashMap<>();out.put("itemId",id);out.put("itemStatus",i.get("status"));out.put("occupation",occ);return out;}

    private Map<String,Object> item(Long id){try{return jdbc.queryForMap("SELECT * FROM items WHERE id=? AND deleted_at IS NULL",id);}catch(org.springframework.dao.EmptyResultDataAccessException ex){throw new DomainException(ErrorCode.ITEM_NOT_FOUND);}}
    private Map<String,Object> ownedItem(Long uid,Long id){Map<String,Object> i=item(id);if(!Objects.equals(uid,((Number)i.get("seller_id")).longValue()))throw new DomainException(ErrorCode.FORBIDDEN);return i;}
    private void ensureWritable(Long uid){UserEntity u=users.findById(uid).orElseThrow(()->new DomainException(ErrorCode.USER_NOT_FOUND));if("RESTRICTED".equals(u.getStatus()))throw new DomainException(ErrorCode.USER_RESTRICTED_CANNOT_WRITE);if("ADMIN".equals(u.getRole()))throw new DomainException(ErrorCode.FORBIDDEN);}
    private void validateImageUrls(List<String> urls){for(String url:urls)if(url==null||url.length()>255||!url.startsWith("/static/uploads/")||url.contains("..")||url.contains("\\"))throw new DomainException(ErrorCode.VALIDATION_ERROR);}
    private RowMapper<ItemSummary> summaryMapper(){return (rs,n)->{ItemSummary x=new ItemSummary();x.setId(rs.getLong("id"));x.setTitle(rs.getString("title"));x.setCoverImageUrl(rs.getString("cover"));x.setEstimatedPrice(DecimalString.of(rs.getBigDecimal("estimated_price")));x.setCurrency("CNY");x.setStatus(ItemStatus.valueOf(rs.getString("status")));x.setSwapPreference(SwapPreference.valueOf(rs.getString("swap_preference")));x.setCategoryId(rs.getLong("category_id"));x.setCategoryName(rs.getString("category_name"));x.setConditionId(rs.getLong("condition_id"));x.setConditionName(rs.getString("condition_name"));x.setTradeZoneId(rs.getLong("trade_zone_id"));x.setTradeZoneName(rs.getString("zone_name"));x.setSellerId(rs.getLong("seller_id"));x.setSellerNickname(rs.getString("seller_nickname"));Timestamp t=rs.getTimestamp("published_at");if(t!=null)x.setPublishedAt(t.toInstant());return x;};}
    private ItemDetail detailFrom(ResultSet rs)throws SQLException{ItemDetail d=new ItemDetail();d.setId(rs.getLong("id"));d.setTitle(rs.getString("title"));d.setDescription(rs.getString("description"));d.setEstimatedPrice(DecimalString.of(rs.getBigDecimal("estimated_price")));d.setCurrency("CNY");d.setPriceDisclaimer("用户自填估价，仅供参考，非平台定价");d.setStatus(ItemStatus.valueOf(rs.getString("status")));d.setSwapPreference(SwapPreference.valueOf(rs.getString("swap_preference")));d.setSwapDisplayStatus("SWAPPING".equals(rs.getString("status"))?"交换中":"SOLD".equals(rs.getString("status"))?"已交换":SwapPreference.ACCEPTING.name().equals(rs.getString("swap_preference"))?"可交换":"暂不交换");d.setVersion(rs.getInt("version"));Timestamp p=rs.getTimestamp("published_at"),u=rs.getTimestamp("updated_at");if(p!=null)d.setPublishedAt(p.toInstant());if(u!=null)d.setUpdatedAt(u.toInstant());
        Category c=new Category();c.setId(rs.getLong("category_id"));c.setCode(rs.getString("category_code"));c.setName(rs.getString("category_name"));c.setSortOrder(rs.getInt("category_sort"));d.setCategory(c);
        ItemCondition ic=new ItemCondition();ic.setId(rs.getLong("condition_id"));ic.setCode(rs.getString("condition_code"));ic.setName(rs.getString("condition_name"));ic.setSortOrder(rs.getInt("condition_sort"));d.setCondition(ic);
        TradeZone z=new TradeZone();z.setId(rs.getLong("trade_zone_id"));z.setCode(rs.getString("zone_code"));z.setName(rs.getString("zone_name"));d.setTradeZone(z);
        UserProfile seller=new UserProfile();seller.setId(rs.getLong("seller_id"));seller.setNickname(rs.getString("seller_nickname"));seller.setAvatarUrl(rs.getString("seller_avatar"));d.setSeller(seller);
        d.setImages(jdbc.query("SELECT * FROM item_images WHERE item_id=? ORDER BY sort_order,id",(r,n)->{ItemImage im=new ItemImage();im.setId(r.getLong("id"));im.setImageUrl(r.getString("image_url"));im.setSortOrder(r.getInt("sort_order"));im.setContentType(r.getString("content_type"));im.setFileSize((Integer)r.getObject("file_size"));return im;},rs.getLong("id")));if("BANNED".equals(rs.getString("status")))d.setBannedReason(rs.getString("banned_reason"));return d;}
}
