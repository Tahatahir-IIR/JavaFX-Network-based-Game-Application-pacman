package application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoreManager {
    private static final String SCORE_FILE = System.getProperty("user.home") + File.separator + ".pacman_scores.txt";
    private static int lastScore = 0;

    public static synchronized void addScore(int score) {
        addScore(score, "EASY", "SOLO");
    }
    
    public static synchronized void addScore(int score, String difficulty, String gameMode) {
        lastScore = score;
        User u = UserSession.getCurrentUser();
        if (u == null) {
            // fallback to file-based storage for anonymous users
            List<Integer> scores = getAllScores();
            scores.add(score);
            Collections.sort(scores, Collections.reverseOrder());
            if (scores.size() > 50) scores = scores.subList(0, 50);
            saveScores(scores);
            return;
        }

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO scores(user_id, score, difficulty, game_mode) VALUES(?,?,?,?)")) {
            p.setInt(1, u.id);
            p.setInt(2, score);
            p.setString(3, difficulty);
            p.setString(4, gameMode);
            p.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized List<Integer> getTopScores(int limit) {
        User u = UserSession.getCurrentUser();
        if (u == null) return getTopScoresFromFile(limit);

        List<Integer> scores = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT score FROM scores WHERE user_id = ? ORDER BY score DESC LIMIT ?")) {
            p.setInt(1, u.id);
            p.setInt(2, limit);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) scores.add(rs.getInt("score"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // pad with zeros
        while (scores.size() < limit) scores.add(0);
        return scores;
    }

    public static synchronized java.util.List<String> getGlobalTopScores(int limit) {
        return getGlobalTopScoresByMode(limit, "SOLO");
    }
    
    public static synchronized java.util.List<String> getGlobalTopScoresByMode(int limit, String gameMode) {
        java.util.List<String> out = new java.util.ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT u.username, MAX(s.score) AS top_score FROM scores s JOIN users u ON s.user_id = u.id WHERE s.game_mode = ? GROUP BY u.id ORDER BY top_score DESC LIMIT ?")) {
            p.setString(1, gameMode);
            p.setInt(2, limit);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    int score = rs.getInt("top_score");
                    out.add(username + " - " + score);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // pad
        while (out.size() < limit) out.add("- 0");
        return out;
    }
    
    public static synchronized java.util.List<String> getSoloGlobalScores(int limit) {
        return getGlobalTopScoresByMode(limit, "SOLO");
    }
    
    public static synchronized java.util.List<Integer> getSoloScores(int limit) {
        User u = UserSession.getCurrentUser();
        if (u == null) return getTopScoresFromFile(limit);

        List<Integer> scores = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT score FROM scores WHERE user_id = ? AND game_mode = 'SOLO' ORDER BY score DESC LIMIT ?")) {
            p.setInt(1, u.id);
            p.setInt(2, limit);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) scores.add(rs.getInt("score"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (scores.size() < limit) scores.add(0);
        return scores;
    }
    
    public static synchronized java.util.List<Integer> getDuoScores(int limit) {
        User u = UserSession.getCurrentUser();
        if (u == null) return new ArrayList<>();

        List<Integer> scores = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT score FROM scores WHERE user_id = ? AND game_mode = 'DUO' ORDER BY score DESC LIMIT ?")) {
            p.setInt(1, u.id);
            p.setInt(2, limit);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) scores.add(rs.getInt("score"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (scores.size() < limit) scores.add(0);
        return scores;
    }
    
    public static synchronized java.util.List<String> getDuoGlobalScores(int limit) {
        return getGlobalTopScoresByMode(limit, "DUO");
    }
    
    public static synchronized java.util.List<Integer> getRankedScores(int limit) {
        User u = UserSession.getCurrentUser();
        if (u == null) return new ArrayList<>();

        List<Integer> scores = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT score FROM scores WHERE user_id = ? AND difficulty = 'INSANE' ORDER BY score DESC LIMIT ?")) {
            p.setInt(1, u.id);
            p.setInt(2, limit);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) scores.add(rs.getInt("score"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (scores.size() < limit) scores.add(0);
        return scores;
    }

    private static List<Integer> getTopScoresFromFile(int limit) {
        List<Integer> scores = getAllScores();
        Collections.sort(scores, Collections.reverseOrder());
        if (scores.size() > limit) return new ArrayList<>(scores.subList(0, limit));
        while (scores.size() < limit) scores.add(0);
        return scores;
    }

    private static List<Integer> getAllScores() {
        List<Integer> scores = new ArrayList<>();
        File f = new File(SCORE_FILE);
        if (!f.exists()) return scores;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                try {
                    scores.add(Integer.parseInt(line.trim()));
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return scores;
    }

    private static void saveScores(List<Integer> scores) {
        File f = new File(SCORE_FILE);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
            for (Integer s : scores) w.write(s.toString() + "\n");
        } catch (Exception ignored) {}
    }

    public static int getLastScore() {
        User u = UserSession.getCurrentUser();
        if (u == null) return lastScore;
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT score FROM scores WHERE user_id = ? ORDER BY created_at DESC LIMIT 1")) {
            p.setInt(1, u.id);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getInt("score");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lastScore;
    }
    
    // Admin function to delete a user's score
    public static synchronized boolean deleteScore(int scoreId) {
        User u = UserSession.getCurrentUser();
        if (u == null || !u.isAdmin) return false;
        
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM scores WHERE id = ?")) {
            p.setInt(1, scoreId);
            int rows = p.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Admin function to delete all scores for a specific user
    public static synchronized boolean deleteUserScores(int userId) {
        User u = UserSession.getCurrentUser();
        if (u == null || !u.isAdmin) return false;
        
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM scores WHERE user_id = ?")) {
            p.setInt(1, userId);
            p.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Get all scores with user info for admin view
    public static synchronized java.util.List<java.util.Map<String, Object>> getAllScoresForAdmin(int limit) {
        java.util.List<java.util.Map<String, Object>> scores = new java.util.ArrayList<>();
        User u = UserSession.getCurrentUser();
        if (u == null || !u.isAdmin) return scores;
        
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT s.id, s.user_id, u.username, s.score, s.difficulty, s.game_mode, s.created_at " +
                     "FROM scores s JOIN users u ON s.user_id = u.id " +
                     "ORDER BY s.created_at DESC LIMIT ?")) {
            p.setInt(1, limit);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> scoreData = new java.util.HashMap<>();
                    scoreData.put("id", rs.getInt("id"));
                    scoreData.put("user_id", rs.getInt("user_id"));
                    scoreData.put("username", rs.getString("username"));
                    scoreData.put("score", rs.getInt("score"));
                    scoreData.put("difficulty", rs.getString("difficulty"));
                    scoreData.put("game_mode", rs.getString("game_mode"));
                    scoreData.put("created_at", rs.getString("created_at"));
                    scores.add(scoreData);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scores;
    }
}

