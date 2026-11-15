package mediqueue.thread;

import mediqueue.model.TriageCategory;

/**
 * Model class representing a simulated patient arrival for multi-threaded
 * discrete-event queue policy benchmarking.
 */
public class SimulatedPatient {

    private final int id;
    private final String name;
    private final TriageCategory category;
    private final int arrivalMinute;
    private final int serviceMinutes;

    public SimulatedPatient(int id, String name, TriageCategory category, int arrivalMinute, int serviceMinutes) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.arrivalMinute = arrivalMinute;
        this.serviceMinutes = serviceMinutes;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TriageCategory getCategory() {
        return category;
    }

    public int getArrivalMinute() {
        return arrivalMinute;
    }

    public int getServiceMinutes() {
        return serviceMinutes;
    }

    /**
     * Calculates the patient's effective Manchester triage category level (1 to 5)
     * at a given minute, applying the Anti-Starvation Aging rule:
     * 1 category level promotion for every 30 minutes spent waiting in the queue.
     */
    public int getEffectiveCategoryLevel(int currentMinute) {
        int waitMinutes = Math.max(0, currentMinute - arrivalMinute);
        int agingPromotions = waitMinutes / 30; // 1 level up every 30 mins
        int originalLevel = category.getLevel();
        return Math.max(1, originalLevel - agingPromotions);
    }

    @Override
    public String toString() {
        return "Patient #" + id + " [" + category.getDescription() + ", arr=" + arrivalMinute + "m, dur=" + serviceMinutes + "m]";
    }
}
