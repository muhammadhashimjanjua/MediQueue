package mediqueue.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class DBConnection {

    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DB_NAME = "mediqueue_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static final String SERVER_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/?useSSL=false";
    private static final String DB_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + "?useSSL=false&autoReconnect=true";

    private static volatile boolean initialized = false;

    static {
        try {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                Class.forName("com.mysql.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    /**
     * Obtains a connection to the MediQueue database.
     * Automatically initializes database and tables on first call.
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            initDatabase();
        }
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    /**
     * Checks if the database server is reachable.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Initializes database, tables, and seed data if they do not exist.
     */
    public static synchronized void initDatabase() throws SQLException {
        if (initialized) return;

        // 1. Create database if missing
        try (Connection conn = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
        }

        // 2. Create tables & default data
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            // Users table
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "full_name VARCHAR(100) NOT NULL,"
                    + "username VARCHAR(50) NOT NULL UNIQUE,"
                    + "password VARCHAR(255) NOT NULL,"
                    + "role VARCHAR(30) NOT NULL,"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")";
            stmt.executeUpdate(createUsersTable);

            // Doctors table
            String createDoctorsTable = "CREATE TABLE IF NOT EXISTS doctors ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "name VARCHAR(100) NOT NULL,"
                    + "specialization VARCHAR(100) NOT NULL,"
                    + "room_no VARCHAR(20) NOT NULL,"
                    + "is_available BOOLEAN DEFAULT TRUE"
                    + ")";
            stmt.executeUpdate(createDoctorsTable);

            // Appointments table with double-booking prevention constraint
            String createAppointmentsTable = "CREATE TABLE IF NOT EXISTS appointments ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "patient_name VARCHAR(100) NOT NULL,"
                    + "doctor_id INT NOT NULL,"
                    + "doctor_name VARCHAR(100) NOT NULL,"
                    + "appointment_date DATE NOT NULL,"
                    + "time_slot VARCHAR(30) NOT NULL,"
                    + "appointment_type VARCHAR(20) DEFAULT 'SCHEDULED',"
                    + "status VARCHAR(20) DEFAULT 'BOOKED',"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE KEY uq_doctor_slot (doctor_id, appointment_date, time_slot)"
                    + ")";
            stmt.executeUpdate(createAppointmentsTable);

            // Triage entries table (Manchester Triage System Categories 1 to 5)
            String createTriageTable = "CREATE TABLE IF NOT EXISTS triage_entries ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "appointment_id INT NULL,"
                    + "patient_name VARCHAR(100) NOT NULL,"
                    + "doctor_id INT NOT NULL,"
                    + "doctor_name VARCHAR(100) NOT NULL,"
                    + "category INT NOT NULL,"
                    + "category_name VARCHAR(100) NOT NULL,"
                    + "target_wait_minutes INT NOT NULL,"
                    + "chief_complaint VARCHAR(255),"
                    + "check_in_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "status VARCHAR(20) DEFAULT 'WAITING'"
                    + ")";
            stmt.executeUpdate(createTriageTable);

            // Seed default users if empty
            String seedUsers = "INSERT IGNORE INTO users (id, full_name, username, password, role) VALUES "
                    + "(1, 'Ali Khan', 'patient', 'patient123', 'PATIENT'), "
                    + "(2, 'Sara Ahmed', 'reception', 'recep123', 'RECEPTIONIST'), "
                    + "(3, 'Dr. Hamza Malik', 'doctor', 'doctor123', 'DOCTOR'), "
                    + "(4, 'System Admin', 'admin', 'admin123', 'ADMINISTRATOR')";
            stmt.executeUpdate(seedUsers);

            // Seed default doctors
            String seedDoctors = "INSERT IGNORE INTO doctors (id, name, specialization, room_no, is_available) VALUES "
                    + "(1, 'Dr. Hamza Malik', 'General Medicine', 'Room 101', TRUE), "
                    + "(2, 'Dr. Ayesha Noor', 'Cardiology', 'Room 203', TRUE), "
                    + "(3, 'Dr. Bilal Tariq', 'Pediatrics', 'Room 105', TRUE), "
                    + "(4, 'Dr. Zainab Fatima', 'Emergency Medicine', 'ER-1', TRUE)";
            stmt.executeUpdate(seedDoctors);

            initialized = true;
        }
    }
}
