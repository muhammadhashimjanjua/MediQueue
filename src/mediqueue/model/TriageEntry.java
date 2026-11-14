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

    /**
     * Calculates elapsed waiting time in minutes since check-in.
     */
    public long getWaitMinutes() {
        return getWaitMinutes(0);
    }

    public long getWaitMinutes(int simulatedExtraMinutes) {
        if (checkInTime == null) return simulatedExtraMinutes;
        long diffMs = System.currentTimeMillis() - checkInTime.getTime();
        long actualWait = Math.max(0, diffMs / (60 * 1000));
        return actualWait + simulatedExtraMinutes;
    }

    /**
     * Anti-Starvation Fairness Rule (Manchester Triage System with Aging):
     * For every 30 minutes a patient waits in queue, their effective priority
     * advances by 1 category level (up to Category 1 max priority).
     */
    public TriageCategory getEffectiveCategory() {
        return getEffectiveCategory(0);
    }

    public TriageCategory getEffectiveCategory(int simulatedExtraMinutes) {
        long totalWaitMinutes = getWaitMinutes(simulatedExtraMinutes);
        int originalLevel = category.getLevel();
        int agingSteps = (int) (totalWaitMinutes / 30); // 1 category up per 30 minutes
        int effectiveLevel = Math.max(1, originalLevel - agingSteps);
        return TriageCategory.fromLevel(effectiveLevel);
    }

    /**
     * Returns how many category levels this patient was boosted by aging (0, 1, 2, etc.).
     */
    public int getAgingBoost() {
        return getAgingBoost(0);
    }

    public int getAgingBoost(int simulatedExtraMinutes) {
        return category.getLevel() - getEffectiveCategory(simulatedExtraMinutes).getLevel();
    }

    /**
     * Checks if this patient has exceeded the Manchester Triage target max wait time.
     */
    public boolean isOverdue() {
        return isOverdue(0);
    }

    public boolean isOverdue(int simulatedExtraMinutes) {
        long totalWait = getWaitMinutes(simulatedExtraMinutes);
        int target = category.getTargetWaitMinutes();
        return target > 0 && totalWait > target;
    }
}
