package mediqueue.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import mediqueue.db.DBConnection;

/**
 * Handles database-backed authentication and user registration for MediQueue.
 */
public class AuthService {

    public enum Role {
        PATIENT, RECEPTIONIST, DOCTOR, ADMINISTRATOR;

        @Override
        public String toString() {
            String s = name().toLowerCase();
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }

        public static Role fromString(String text) {
            if (text == null) return null;
            for (Role r : Role.values()) {
                if (r.name().equalsIgnoreCase(text.trim())) {
                    return r;
                }
            }
            return null;
        }
    }

    private String lastError = null;

    public String getLastError() {
        return lastError;
    }

    /**
     * Authenticates a user against MySQL database.
     * @return the user's full name on success, or null on failure.
     */
    public String login(String username, char[] password, Role role) {
        lastError = null;
        if (username == null || username.trim().isEmpty() || password == null || password.length == 0 || role == null) {
            lastError = "Please enter username, password and select a role.";
            return null;
        }

        String userTrimmed = username.trim();
        String passString = new String(password);

        String sql = "SELECT full_name, password FROM users WHERE LOWER(username) = LOWER(?) AND role = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userTrimmed);
            ps.setString(2, role.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    String fullName = rs.getString("full_name");
                    if (passString.equals(storedPassword)) {
                        return fullName;
                    }
                }
            }

            lastError = "Invalid username, password or role.";
            return null;

        } catch (SQLException e) {
            e.printStackTrace();
            lastError = "Database error: Cannot connect to MySQL. Ensure XAMPP MySQL is running.";
            return null;
        }
    }

    /**
     * Registers a new user into MySQL database.
     * @return true on success, false on failure (check getLastError() for details).
     */
    public boolean register(String fullName, String username, char[] password, Role role) {
        lastError = null;

        if (fullName == null || fullName.trim().isEmpty()) {
            lastError = "Full name cannot be empty.";
            return false;
        }
        if (username == null || username.trim().isEmpty()) {
            lastError = "Username cannot be empty.";
            return false;
        }
        if (username.trim().length() < 3) {
            lastError = "Username must be at least 3 characters.";
            return false;
        }
        if (password == null || password.length == 0) {
            lastError = "Password cannot be empty.";
            return false;
        }
        if (password.length < 4) {
            lastError = "Password must be at least 4 characters.";
            return false;
        }
        if (role == null) {
            lastError = "Please select a role.";
            return false;
        }

        String cleanName = fullName.trim();
        String cleanUser = username.trim();
        String passStr = new String(password);

        try (Connection conn = DBConnection.getConnection()) {
            // Check if username already exists
            String checkSql = "SELECT id FROM users WHERE LOWER(username) = LOWER(?)";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, cleanUser);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        lastError = "Username '" + cleanUser + "' is already taken. Please choose another.";
                        return false;
                    }
                }
            }

            // Insert new user
            String insertSql = "INSERT INTO users (full_name, username, password, role) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                insertPs.setString(1, cleanName);
                insertPs.setString(2, cleanUser);
                insertPs.setString(3, passStr);
                insertPs.setString(4, role.name());

                int rows = insertPs.executeUpdate();
                return rows > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            lastError = "Database error: " + e.getMessage();
            return false;
        }
    }
}
