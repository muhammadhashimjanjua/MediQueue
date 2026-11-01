package mediqueue.model;

import java.sql.Timestamp;

/**
 * Appointment entity for patient slot booking.
 */
public class Appointment {
    private final int id;
    private final String patientName;
    private final int doctorId;
    private final String doctorName;
    private final String appointmentDate;
    private final String timeSlot;
    private final String appointmentType;
    private final String status;
    private final Timestamp createdAt;

    public Appointment(int id, String patientName, int doctorId, String doctorName,
                       String appointmentDate, String timeSlot, String appointmentType,
                       String status, Timestamp createdAt) {
        this.id = id;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.appointmentDate = appointmentDate;
        this.timeSlot = timeSlot;
        this.appointmentType = appointmentType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
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

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public String getAppointmentType() {
        return appointmentType;
    }

    public String getStatus() {
        return status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "#" + id + " - " + patientName + " with " + doctorName + " (" + appointmentDate + " " + timeSlot + ")";
    }
}
