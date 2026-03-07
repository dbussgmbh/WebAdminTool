package com.example.app.repository;

import com.example.app.data.AppUser;
import com.example.app.data.Role;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AppUserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AppUserRowMapper rowMapper = new AppUserRowMapper();

    public AppUserRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AppUser> findByUsernameIgnoreCase(String username) {
        String sql = "SELECT ID, USERNAME, PASSWORD_HASH, ROLE, ENABLED, CREATED_AT, UPDATED_AT FROM APP_USERS WHERE UPPER(USERNAME)=UPPER(:username)";
        List<AppUser> users = jdbcTemplate.query(sql, new MapSqlParameterSource("username", username), rowMapper);
        return users.stream().findFirst();
    }

    public Optional<AppUser> findById(Long id) {
        String sql = "SELECT ID, USERNAME, PASSWORD_HASH, ROLE, ENABLED, CREATED_AT, UPDATED_AT FROM APP_USERS WHERE ID=:id";
        List<AppUser> users = jdbcTemplate.query(sql, new MapSqlParameterSource("id", id), rowMapper);
        return users.stream().findFirst();
    }

    public boolean existsByUsernameIgnoreCase(String username) {
        String sql = "SELECT COUNT(*) FROM APP_USERS WHERE UPPER(USERNAME)=UPPER(:username)";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("username", username), Integer.class);
        return count != null && count > 0;
    }

    public List<AppUser> findAllOrdered() {
        String sql = "SELECT ID, USERNAME, PASSWORD_HASH, ROLE, ENABLED, CREATED_AT, UPDATED_AT FROM APP_USERS ORDER BY USERNAME";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<AppUser> searchByUsername(String term) {
        String sql = "SELECT ID, USERNAME, PASSWORD_HASH, ROLE, ENABLED, CREATED_AT, UPDATED_AT FROM APP_USERS WHERE UPPER(USERNAME) LIKE UPPER(:term) ORDER BY USERNAME";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("term", "%" + term + "%"), rowMapper);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM APP_USERS", new MapSqlParameterSource(), Long.class);
        return count == null ? 0L : count;
    }

    public AppUser insert(String username, String passwordHash, Role role, Boolean enabled) {
        String sql = "INSERT INTO APP_USERS (USERNAME, PASSWORD_HASH, ROLE, ENABLED) VALUES (:username,:passwordHash,:role,:enabled)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("passwordHash", passwordHash)
                .addValue("role", role.name())
                .addValue("enabled", Boolean.TRUE.equals(enabled) ? 1 : 0);

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"ID"});
        Number key = keyHolder.getKey();
        if (key != null) {
            return findById(key.longValue()).orElseThrow();
        }
        return findByUsernameIgnoreCase(username).orElseThrow();
    }

    public AppUser update(Long id, String username, Role role, Boolean enabled) {
        String sql = "UPDATE APP_USERS SET USERNAME=:username, ROLE=:role, ENABLED=:enabled WHERE ID=:id";
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("username", username)
                .addValue("role", role.name())
                .addValue("enabled", Boolean.TRUE.equals(enabled) ? 1 : 0));
        return findById(id).orElseThrow();
    }

    public void updatePassword(Long id, String passwordHash) {
        String sql = "UPDATE APP_USERS SET PASSWORD_HASH=:passwordHash WHERE ID=:id";
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("passwordHash", passwordHash));
    }

    public void updatePasswordByUsername(String username, String passwordHash) {
        String sql = "UPDATE APP_USERS SET PASSWORD_HASH=:passwordHash WHERE UPPER(USERNAME)=UPPER(:username)";
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("passwordHash", passwordHash));
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM APP_USERS WHERE ID=:id", new MapSqlParameterSource("id", id));
    }
}