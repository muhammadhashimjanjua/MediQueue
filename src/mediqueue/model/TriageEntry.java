package mediqueue.model;

import java.sql.Timestamp;

/**
 * Triage queue entry representing a checked-in patient categorized by MTS.
 */
public class TriageEntry {
    private final int id;
    private final Integer appointmentId;
    private final String patientName;
    private final int doctorId;
    private final String doctorName;
    private final TriageCategory category;
    private final String chiefComplaint;
    private final Timestamp checkInTime;
    private final String status;

    public TriageEntry(int id, Integer appointmentId, String patientName, int doctorId,
                       String doctorName, TriageCategory category, String chiefComplaint,
                       Timestamp checkInTime, String status) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.category = category;
        this.chiefComplaint = chiefComplaint;
        this.checkInTime = checkInTime;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public Integer getAppointmentId() {
        return appointmentId;
    }

    public String getPatientName() {
        return patientName;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public TriageCategory getCategory() {
        return category;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public Timestamp getCheckInTime() {
        return checkInTime;
    }

    public String getStatus() {
        return status;
    }
}
