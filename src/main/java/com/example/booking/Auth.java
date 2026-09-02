package com.example.booking;

import jakarta.ws.rs.core.HttpHeaders;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.sql.*;

public final class Auth {
    private Auth() {}

    public static Staff require(HttpHeaders headers) throws Exception {
        String value = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (value == null || !value.startsWith("Basic ")) {
            throw new UnauthorizedException();
        }
        String decoded;
        try {
            decoded = new String(java.util.Base64.getDecoder().decode(value.substring(6)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException();
        }
        int colon = decoded.indexOf(':');
        if (colon <= 0) throw new UnauthorizedException();

        String username = decoded.substring(0, colon);
        String password = decoded.substring(colon + 1);

        try (Connection c = Database.pool().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id,username,password_hash,display_name,active FROM staff WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !rs.getBoolean("active")
                        || !BCrypt.checkpw(password, rs.getString("password_hash"))) {
                    throw new UnauthorizedException();
                }
                return new Staff(rs.getLong("id"), rs.getString("username"),
                        rs.getString("display_name"));
            }
        }
    }

    public record Staff(long id, String username, String displayName) {}
    public static class UnauthorizedException extends RuntimeException {}
}
