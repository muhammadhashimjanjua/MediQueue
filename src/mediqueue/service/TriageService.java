package mediqueue.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import mediqueue.db.DBConnection;
import mediqueue.model.Appointment;
import mediqueue.model.TriageCategory;
import mediqueue.model.TriageEntry;

/**
 * Handles Manchester Triage System check-ins, urgency classification,
 * and priority queue ordering.
 */
public class TriageService {

    /**
     * Checks in a patient with a Manchester Triage System (MTS) category.
     * @return null on success, or an error description.
     */
    public String checkInPatient(Integer appointmentId, String patientName, int doctorId,
                                 String doctorName, TriageCategory category, String chiefComplaint) {
        if (patientName == null || patientName.trim().isEmpty()) {
            return "Patient name is required.";
        }
        if (category == null) {
            return "Please select a Manchester Triage Category (1 to 5).";
        }

        String sqlTriage = "INSERT INTO triage_entries (appointment_id, patient_name, doctor_id, doctor_name, "
                + "category, category_name, target_wait_minutes, chief_complaint, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'WAITING')";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlTriage)) {
                    if (appointmentId != null && appointmentId > 0) {
                        ps.setInt(1, appointmentId);
                    } else {
                        ps.setNull(1, Types.INTEGER);
                    }
                    ps.setString(2, patientName.trim());
                    ps.setInt(3, doctorId);
                    ps.setString(4, doctorName);
                    ps.setInt(5, category.getLevel());
                    ps.setString(6, category.getDescription());
                    ps.setInt(7, category.getTargetWaitMinutes());
                    ps.setString(8, chiefComplaint != null ? chiefComplaint.trim() : "");
                    ps.executeUpdate();
                }

                if (appointmentId != null && appointmentId > 0) {
                    String sqlAppt = "UPDATE appointments SET status = 'CHECKED_IN' WHERE id = ?";
                    try (PreparedStatement ps2 = conn.prepareStatement(sqlAppt)) {
                        ps2.setInt(1, appointmentId);
                        ps2.executeUpdate();
                    }
                }

                conn.commit();
                return null; // Success
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Triage check-in error: " + e.getMessage();
        }
    }

    /**
     * Returns the active queue ordered by Manchester Triage Category (Category 1 first)
     * and then by check-in time (FIFO for equal priority).
     */
    public List<TriageEntry> getActiveTriageQueue() {
        List<TriageEntry> list = new ArrayList<TriageEntry>();
        String sql = "SELECT id, appointment_id, patient_name, doctor_id, doctor_name, category, "
                + "chief_complaint, check_in_time, status "
                + "FROM triage_entries WHERE status = 'WAITING' "
                + "ORDER BY category ASC, check_in_time ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int catLevel = rs.getInt("category");
                int apptIdVal = rs.getInt("appointment_id");
                Integer apptId = rs.wasNull() ? null : apptIdVal;

                list.add(new TriageEntry(
                        rs.getInt("id"),
                        apptId,
                        rs.getString("patient_name"),
                        rs.getInt("doctor_id"),
                        rs.getString("doctor_name"),
                        TriageCategory.fromLevel(catLevel),
                        rs.getString("chief_complaint"),
                        rs.getTimestamp("check_in_time"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Fetches appointments currently in BOOKED status awaiting check-in.
     */
    public List<Appointment> getPendingAppointments() {
        List<Appointment> list = new ArrayList<Appointment>();
        String sql = "SELECT id, patient_name, doctor_id, doctor_name, appointment_date, time_slot, appointment_type, status, created_at "
                + "FROM appointments WHERE status = 'BOOKED' ORDER BY appointment_date ASC, time_slot ASC";
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

    /**
     * Marks a triaged patient as served/consulted.
     */
    public boolean markServed(int triageId) {
        String sql = "UPDATE triage_entries SET status = 'SERVED' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, triageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
