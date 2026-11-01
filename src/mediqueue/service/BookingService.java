package mediqueue.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mediqueue.db.DBConnection;
import mediqueue.model.Appointment;
import mediqueue.model.Doctor;

/**
 * Handles slot booking, double-booking validation, and appointment management.
 */
public class BookingService {

    public static final List<String> STANDARD_SLOTS = Arrays.asList(
            "09:00 AM - 09:30 AM",
            "09:30 AM - 10:00 AM",
            "10:00 AM - 10:30 AM",
            "10:30 AM - 11:00 AM",
            "11:00 AM - 11:30 AM",
            "11:30 AM - 12:00 PM",
            "02:00 PM - 02:30 PM",
            "02:30 PM - 03:00 PM",
            "03:00 PM - 03:30 PM",
            "03:30 PM - 04:00 PM",
            "04:00 PM - 04:30 PM",
            "04:30 PM - 05:00 PM"
    );

    /**
     * Retrieves all registered doctors.
     */
    public List<Doctor> getDoctors() {
        List<Doctor> list = new ArrayList<Doctor>();
        String sql = "SELECT id, name, specialization, room_no, is_available FROM doctors ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Doctor(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("specialization"),
                        rs.getString("room_no"),
                        rs.getBoolean("is_available")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Checks if a doctor already has an appointment booked for a given date & time slot.
     */
    public boolean isDoubleBooking(int doctorId, String date, String timeSlot) {
        String sql = "SELECT id FROM appointments WHERE doctor_id = ? AND appointment_date = ? AND time_slot = ? AND status != 'CANCELLED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setString(2, date);
            ps.setString(3, timeSlot);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Books a new appointment with double-booking prevention.
     * @return null on success, or an error description string on failure.
     */
    public String bookAppointment(String patientName, int doctorId, String doctorName,
                                  String date, String timeSlot, String appointmentType) {
        if (patientName == null || patientName.trim().isEmpty()) {
            return "Patient name is required.";
        }
        if (date == null || date.trim().isEmpty()) {
            return "Appointment date is required (YYYY-MM-DD).";
        }
        if (timeSlot == null || timeSlot.trim().isEmpty()) {
            return "Please select a time slot.";
        }

        // 1. Check double booking
        if (isDoubleBooking(doctorId, date.trim(), timeSlot.trim())) {
            return "Double-booking detected: " + doctorName + " already has an appointment on "
                    + date + " at " + timeSlot + ". Please pick another slot or doctor.";
        }

        // 2. Insert appointment
        String sql = "INSERT INTO appointments (patient_name, doctor_id, doctor_name, appointment_date, time_slot, appointment_type, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'BOOKED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patientName.trim());
            ps.setInt(2, doctorId);
            ps.setString(3, doctorName);
            ps.setString(4, date.trim());
            ps.setString(5, timeSlot.trim());
            ps.setString(6, appointmentType != null ? appointmentType : "SCHEDULED");

            int rows = ps.executeUpdate();
            return rows > 0 ? null : "Failed to record appointment.";
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // MySQL duplicate key
                return "Double-booking conflict: This slot was just reserved by another patient.";
            }
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    /**
     * Retrieves all appointments ordered by date and time slot.
     */
    public List<Appointment> getAllAppointments() {
        List<Appointment> list = new ArrayList<Appointment>();
        String sql = "SELECT id, patient_name, doctor_id, doctor_name, appointment_date, time_slot, appointment_type, status, created_at "
                + "FROM appointments ORDER BY appointment_date DESC, id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Appointment(
                        rs.getInt("id"),
                        rs.getString("patient_name"),
                        rs.getInt("doctor_id"),
                        rs.getString("doctor_name"),
                        rs.getString("appointment_date"),
                        rs.getString("time_slot"),
                        rs.getString("appointment_type"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
