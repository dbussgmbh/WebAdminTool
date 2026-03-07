package com.example.app.repository;

import com.example.app.data.AppUser;
import com.example.app.data.Role;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AppUserRowMapper implements RowMapper<AppUser> {
    @Override
    public AppUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        AppUser user = new AppUser();
        user.setId(rs.getLong("ID"));
        user.setUsername(rs.getString("USERNAME"));
        user.setPasswordHash(rs.getString("PASSWORD_HASH"));
        user.setRole(Role.valueOf(rs.getString("ROLE")));
        user.setEnabled(rs.getInt("ENABLED") == 1);
        if (rs.getTimestamp("CREATED_AT") != null) user.setCreatedAt(rs.getTimestamp("CREATED_AT").toLocalDateTime());
        if (rs.getTimestamp("UPDATED_AT") != null) user.setUpdatedAt(rs.getTimestamp("UPDATED_AT").toLocalDateTime());
        return user;
    }
}
