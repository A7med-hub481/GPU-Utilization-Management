package model;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * A student's GPU minutes.
 * Available time = Allocated time - Used time - Reserved time
 */
public class TimeAllowance {

    private int allocatedTime;
    private int usedTime;
    private int reservedTime;
    private ArrayList<String> history = new ArrayList<String>();   // "time|minutes|reason"

    public TimeAllowance(int allocatedTime) {
        this(allocatedTime, 0, 0);
    }

    public TimeAllowance(int allocatedTime, int usedTime, int reservedTime) {
        this.allocatedTime = allocatedTime;
        this.usedTime = usedTime;
        this.reservedTime = reservedTime;
    }

    public int getAvailableTime() {
        return allocatedTime - usedTime - reservedTime;
    }

    // reserve the full job duration when a job is accepted
    public void reserve(int minutes) {
        if (minutes > getAvailableTime()) {
            throw new IllegalArgumentException("Not enough time. The job needs " + minutes
                    + " minutes but you have " + getAvailableTime() + " minutes available.");
        }
        reservedTime += minutes;
    }

    // a job has ended: release its reservation and charge only the real runtime
    public void settle(int reservedMinutes, int chargedMinutes) {
        reservedTime -= reservedMinutes;
        usedTime += chargedMinutes;
    }

    public void addExtraTime(int minutes, String reason, LocalDateTime time) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Extra time must be more than 0 minutes.");
        }
        allocatedTime += minutes;
        history.add(Scheduler.format(time) + "|+" + minutes + "|" + reason);
    }

    public void changeAllocatedTime(int newTime, String reason, LocalDateTime time) {
        if (newTime < usedTime + reservedTime) {
            throw new IllegalArgumentException("Allocated time cannot be less than used + reserved time ("
                    + (usedTime + reservedTime) + " minutes).");
        }
        int change = newTime - allocatedTime;
        allocatedTime = newTime;
        history.add(Scheduler.format(time) + "|" + (change >= 0 ? "+" : "") + change + "|" + reason);
    }

    public int getAllocatedTime() { return allocatedTime; }
    public int getUsedTime() { return usedTime; }
    public int getReservedTime() { return reservedTime; }
    public ArrayList<String> getHistory() { return history; }
}
