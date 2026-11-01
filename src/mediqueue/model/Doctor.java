package mediqueue.model;

/**
 * Doctor entity representing clinic physicians.
 */
public class Doctor {
    private final int id;
    private final String name;
    private final String specialization;
    private final String roomNo;
    private final boolean available;

    public Doctor(int id, String name, String specialization, String roomNo, boolean available) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.roomNo = roomNo;
        this.available = available;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public String toString() {
        return name + " (" + specialization + " - " + roomNo + ")";
    }
}
