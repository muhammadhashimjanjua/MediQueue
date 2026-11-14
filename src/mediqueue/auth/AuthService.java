package mediqueue.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import mediqueue.db.DBConnection;


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
     * Checks if a string contains ONLY alphabetic characters (letters and spaces).
     */
    public static boolean isAlphabetOnly(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        return str.trim().matches("^[a-zA-Z\\s]+$");
    }

    /**
     * Checks if a string contains ONLY numeric digits (0-9).
     */
    public static boolean isNumericOnly(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        return str.trim().matches("^[0-9]+$");
    }

    /**
     * Checks if a string contains alphanumeric characters (both letters and numbers allowed).
     */
    public static boolean isAlphanumeric(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        return str.trim().matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * Checks if a string contains both letters and numbers.
     */
    public static boolean containsBothLettersAndNumbers(String str) {
        if (str == null) return false;
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : str.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (hasLetter && hasDigit) return true;
        }
        return false;
    }

    public boolean register(String fullName, String username, char[] password, Role role) {
        return register(fullName, "", username, password, role);
    }

    public boolean register(String fullName, String phone, String username, char[] password, Role role) {
        lastError = null;

        if (fullName == null || fullName.trim().isEmpty()) {
            lastError = "Full name cannot be empty.";
            return false;
        }
        String cleanName = fullName.trim();
        if (!isAlphabetOnly(cleanName)) {
            lastError = "Full Name must contain only alphabets (letters and spaces).";
            return false;
        }

        String cleanPhone = (phone != null) ? phone.trim() : "";
        if (!cleanPhone.isEmpty()) {
            if (!isNumericOnly(cleanPhone)) {
                lastError = "Phone number must contain only numbers (digits).";
                return false;
            }
            if (cleanPhone.length() < 7 || cleanPhone.length() > 15) {
                lastError = "Phone number must be between 7 and 15 digits.";
                return false;
            }
        }

        if (username == null || username.trim().isEmpty()) {
            lastError = "Username cannot be empty.";
            return false;
        }
        String cleanUser = username.trim();
        if (cleanUser.length() < 3) {
            lastError = "Username must be at least 3 characters.";
            return false;
        }
        if (!isAlphanumeric(cleanUser)) {
            lastError = "Username can only contain letters and numbers.";
            return false;
        }

        if (password == null || password.length == 0) {
            lastError = "Password cannot be empty.";
            return false;
        }
        String passStr = new String(password);
        if (passStr.length() < 6) {
            lastError = "Password must be at least 6 characters.";
            return false;
        }
        if (!containsBothLettersAndNumbers(passStr)) {
            lastError = "Password must contain both letters and numbers.";
            return false;
        }

        if (role == null) {
            lastError = "Please select a role.";
            return false;
        }

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
            String insertSql = "INSERT INTO users (full_name, phone, username, password, role) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                insertPs.setString(1, cleanName);
                insertPs.setString(2, cleanPhone);
                insertPs.setString(3, cleanUser);
                insertPs.setString(4, passStr);
                insertPs.setString(5, role.name());

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
