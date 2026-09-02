package com.example.booking;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mindrot.jbcrypt.BCrypt;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;

public final class Database {
    private static HikariDataSource ds;

    private Database() {}

    public static synchronized HikariDataSource pool() {
        if (ds != null) return ds;

        String jdbc = System.getenv("JDBC_DATABASE_URL");
        if (jdbc == null || jdbc.isBlank()) {
            jdbc = localJdbcFromDatabaseUrl(System.getenv("DATABASE_URL"));
        }
        if (jdbc == null || jdbc.isBlank()) {
            throw new IllegalStateException("Set DATABASE_URL or JDBC_DATABASE_URL");
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbc);
        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(10000);
        ds = new HikariDataSource(cfg);
        return ds;
    }

    public static void init() throws Exception {
        try (Connection c = pool().getConnection();
             InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new IllegalStateException("schema.sql missing");
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement st = c.createStatement()) {
                for (String statement : sql.split(";")) {
                    if (!statement.isBlank()) st.execute(statement);
                }
            }
        }

        String username = System.getenv("STAFF_USERNAME");
        String password = System.getenv("STAFF_PASSWORD");
        String display = System.getenv("STAFF_DISPLAY_NAME");

        if (username != null && password != null && display != null
                && !username.isBlank() && !password.isBlank() && !display.isBlank()) {
            String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            try (Connection c = pool().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "INSERT INTO staff(username,password_hash,display_name) VALUES(?,?,?) " +
                         "ON CONFLICT (username) DO NOTHING")) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.setString(3, display);
                ps.executeUpdate();
            }
        }
    }

    private static String localJdbcFromDatabaseUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI u = URI.create(url);
            String userInfo = u.getUserInfo();
            String[] parts = userInfo == null ? new String[]{"", ""} : userInfo.split(":", 2);
            return "jdbc:postgresql://" + u.getHost() + ":" +
                    (u.getPort() > 0 ? u.getPort() : 5432) + u.getPath() +
                    "?user=" + enc(parts[0]) + "&password=" + enc(parts.length > 1 ? parts[1] : "");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid DATABASE_URL", e);
        }
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
