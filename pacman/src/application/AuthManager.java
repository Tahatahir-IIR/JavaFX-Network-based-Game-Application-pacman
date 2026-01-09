package application;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthManager {
    private static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    public static User register(String username, String password) throws Exception {
        String ph = hash(password);
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO users(username,password_hash) VALUES(?,?)")) {
            p.setString(1, username);
            p.setString(2, ph);
            p.executeUpdate();
        }
        return login(username, password);
    }

    public static User login(String username, String password) throws Exception {
        String ph = hash(password);
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT id, username, COALESCE(is_admin, 0) as is_admin FROM users WHERE username = ? AND password_hash = ?")) {
            p.setString(1, username);
            p.setString(2, ph);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    boolean isAdmin = rs.getInt("is_admin") == 1;
                    return new User(rs.getInt("id"), rs.getString("username"), isAdmin);
                }
            }
        }
        return null;
    }
}
