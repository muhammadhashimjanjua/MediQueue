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

    public enum QueuePolicy {
        PRIORITY_WITH_AGING("Priority with Aging (Anti-Starvation)"),
        STRICT_PRIORITY("Strict Priority (Manchester Triage Only)"),
        FCFS("First-Come, First-Served (FCFS)");

        private final String displayName;

        QueuePolicy(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Default queue retrieval using the Anti-Starvation Aging Rule (Priority with Aging).
     */
    public List<TriageEntry> getActiveTriageQueue() {
        return getActiveTriageQueue(QueuePolicy.PRIORITY_WITH_AGING, 0);
    }

    /**
     * Retrieves the active waiting queue ordered by the selected Queue Policy:
     * - PRIORITY_WITH_AGING: Advances category by 1 level per 30 minutes waited (Anti-Starvation).
     * - STRICT_PRIORITY: Raw Manchester Triage category ordering (1 to 5).
     * - FCFS: Strict check-in order.
     */
    public List<TriageEntry> getActiveTriageQueue(QueuePolicy policy, final int simulatedExtraMinutes) {
        List<TriageEntry> list = new ArrayList<TriageEntry>();
        String sql = "SELECT id, appointment_id, patient_name, doctor_id, doctor_name, category, "
                + "chief_complaint, check_in_time, status "
                + "FROM triage_entries WHERE status = 'WAITING'";

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

        final QueuePolicy activePolicy = (policy != null) ? policy : QueuePolicy.PRIORITY_WITH_AGING;

        java.util.Collections.sort(list, new java.util.Comparator<TriageEntry>() {
            @Override
            public int compare(TriageEntry a, TriageEntry b) {
                if (activePolicy == QueuePolicy.PRIORITY_WITH_AGING) {
                    // Compare effective category (after anti-starvation aging)
                    int effA = a.getEffectiveCategory(simulatedExtraMinutes).getLevel();
                    int effB = b.getEffectiveCategory(simulatedExtraMinutes).getLevel();
                    if (effA != effB) {
                        return Integer.compare(effA, effB);
                    }
                    // Equal effective priority: FIFO by check-in time
                    Timestamp tA = a.getCheckInTime();
                    Timestamp tB = b.getCheckInTime();
                    if (tA != null && tB != null) return tA.compareTo(tB);
                    return Integer.compare(a.getId(), b.getId());
                } else if (activePolicy == QueuePolicy.STRICT_PRIORITY) {
                    int catA = a.getCategory().getLevel();
                    int catB = b.getCategory().getLevel();
                    if (catA != catB) {
                        return Integer.compare(catA, catB);
                    }
                    Timestamp tA = a.getCheckInTime();
                    Timestamp tB = b.getCheckInTime();
                    if (tA != null && tB != null) return tA.compareTo(tB);
                    return Integer.compare(a.getId(), b.getId());
                } else {
                    // FCFS
                    Timestamp tA = a.getCheckInTime();
                    Timestamp tB = b.getCheckInTime();
                    if (tA != null && tB != null) return tA.compareTo(tB);
                    return Integer.compare(a.getId(), b.getId());
                }
            }
        });

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

    /**
     * Executes a multi-threaded queue policy simulation comparing FCFS, Strict Priority,
     * and Priority with Aging using 3 concurrent worker threads and Thread.join() barrier synchronization.
     *
     * @param patientCount Number of patient arrivals to simulate.
     * @return List of benchmark results from each policy worker thread.
     */
    public List<mediqueue.thread.PolicySimulationResult> runConcurrentPolicySimulation(int patientCount) {
        return mediqueue.thread.QueuePolicySimulator.runConcurrentSimulation(patientCount, 2, System.currentTimeMillis());
    }
}
