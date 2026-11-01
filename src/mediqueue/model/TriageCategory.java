package mediqueue.model;

import java.awt.Color;

/**
 * Manchester Triage System (MTS) Categories 1 to 5.
 * Core triage model for MediQueue.
 */
public enum TriageCategory {
    CATEGORY_1(1, "Category 1: Immediate / Resuscitation", 0, new Color(0xDC, 0x26, 0x26), "Red"),
    CATEGORY_2(2, "Category 2: Very Urgent", 10, new Color(0xEA, 0x58, 0x0C), "Orange"),
    CATEGORY_3(3, "Category 3: Urgent", 60, new Color(0xCA, 0x8A, 0x04), "Yellow"),
    CATEGORY_4(4, "Category 4: Standard", 120, new Color(0x16, 0xA3, 0x4A), "Green"),
    CATEGORY_5(5, "Category 5: Non-Urgent", 240, new Color(0x25, 0x63, 0xEB), "Blue");

    private final int level;
    private final String description;
    private final int targetWaitMinutes;
    private final Color color;
    private final String colorName;

    TriageCategory(int level, String description, int targetWaitMinutes, Color color, String colorName) {
        this.level = level;
        this.description = description;
        this.targetWaitMinutes = targetWaitMinutes;
        this.color = color;
        this.colorName = colorName;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public int getTargetWaitMinutes() {
        return targetWaitMinutes;
    }

    public Color getColor() {
        return color;
    }

    public String getColorName() {
        return colorName;
    }

    public static TriageCategory fromLevel(int level) {
        for (TriageCategory cat : values()) {
            if (cat.level == level) return cat;
        }
        return CATEGORY_4; // default
    }

    @Override
    public String toString() {
        return level + " - " + description + " (" + (targetWaitMinutes == 0 ? "Immediate" : targetWaitMinutes + " mins") + ")";
    }
}
