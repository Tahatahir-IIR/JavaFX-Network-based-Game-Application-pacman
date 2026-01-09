package application;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String DB_FILE = System.getProperty("user.home") + File.separator + ".pacman.db";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            // driver not on classpath
        }
        return DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
    }

    public static void init() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON;");
            s.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, is_admin INTEGER DEFAULT 0);");
            s.execute("CREATE TABLE IF NOT EXISTS scores (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, score INTEGER NOT NULL, difficulty TEXT DEFAULT 'EASY', game_mode TEXT DEFAULT 'SOLO', created_at DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE);");
            
            // Create admin account if it doesn't exist (password: admin)
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest("admin".getBytes("UTF-8"));
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) sb.append(String.format("%02x", b));
                String adminHash = sb.toString();
                
                s.execute("INSERT OR IGNORE INTO users (username, password_hash, is_admin) VALUES ('admin', '" + adminHash + "', 1)");
            } catch (Exception e) {
                System.err.println("Failed to create admin account: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
