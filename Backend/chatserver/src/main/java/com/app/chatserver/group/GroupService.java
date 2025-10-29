package com.app.chatserver.group;

import com.app.chatserver.model.Group;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupService {

    private final JdbcTemplate jdbc;

    public GroupService(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    /**
     * ✅ LOGIC MỚI: Tạo nhóm VÀ tự động thêm creator làm admin
     */
    @Transactional
    public Group createGroup(String name, int creatorId, String avatar) {
        
        // 1. Insert vào bảng `groups`
        String sqlGroup = """
            INSERT INTO `groups` (name, creator_id, avatar, created_at)
            VALUES (?, ?, ?, ?)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sqlGroup, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setInt(2, creatorId);
            ps.setString(3, avatar);
            ps.setTimestamp(4, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long newGroupId = null;
        if (keyHolder.getKey() != null) {
             newGroupId = keyHolder.getKey().longValue();
        }
        if (newGroupId == null) {
            throw new RuntimeException("Tạo nhóm thất bại, không lấy được ID.");
        }

        // 2. Tự động thêm creator vào `group_members` với vai trò 'admin'
        String sqlMember = """
            INSERT INTO group_members (group_id, member_id, role)
            VALUES (?, ?, 'admin')
            """;
        
        jdbc.update(sqlMember, newGroupId, creatorId);
        
        System.out.println("[GroupService] Đã tạo nhóm " + newGroupId + " và thêm admin " + creatorId);
        
        Group group = new Group();
        group.setId(newGroupId.intValue());
        group.setName(name);
        group.setCreatorId(creatorId);
        group.setAvatar(avatar);
        group.setCreatedAt(now);
        return group;
    }

    /**
     * ✅ LOGIC MỚI: Lấy nhóm theo user ID
     */
    public List<Group> getGroupsByUserId(int userId) {
        String sql = """
            SELECT g.* FROM `groups` g
            JOIN group_members gm ON g.id = gm.group_id
            WHERE gm.member_id = ?
            ORDER BY g.name ASC
            """;
        
        return jdbc.query(sql, new BeanPropertyRowMapper<>(Group.class), userId);
    }
    
    /**
     * ✅ LOGIC MỚI: Xóa nhóm (chỉ creator)
     */
    @Transactional
    public boolean deleteGroup(int groupId, int requesterId) {
        // 1. Kiểm tra xem người yêu cầu có phải là người tạo nhóm không
        String checkOwnerSql = "SELECT COUNT(*) FROM `groups` WHERE id = ? AND creator_id = ?";
        
        Integer count = jdbc.queryForObject(checkOwnerSql, Integer.class, groupId, requesterId);
        
        if (count == null || count == 0) {
            // Không phải chủ sở hữu hoặc nhóm không tồn tại
            System.err.println("[GroupService] User " + requesterId + " không có quyền xóa nhóm " + groupId);
            return false;
        }
        
        // 2. Xóa (Schema DB đã có ON DELETE CASCADE cho group_members và messages)
        String deleteSql = "DELETE FROM `groups` WHERE id = ?";
        int rowsAffected = jdbc.update(deleteSql, groupId);
        
        System.out.println("[GroupService] User " + requesterId + " đã xóa nhóm " + groupId);
        return rowsAffected > 0;
    }
}
