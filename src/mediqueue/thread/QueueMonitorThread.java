package mediqueue.thread;

import javax.swing.SwingUtilities;

/**
 * Background daemon thread that periodically refreshes the hospital triage queue,
 * recalculates patient wait times, evaluates anti-starvation aging promotions,
 * and detects category wait-time breaches in real time.
 *
 * Concurrency features demonstrated:
 * 1. Extending java.lang.Thread with custom lifecycle control.
 * 2. Daemon thread mode (does not prevent JVM shutdown).
 * 3. Graceful cooperative termination using volatile flags and interrupt().
 * 4. Thread-safe Swing GUI dispatch via SwingUtilities.invokeLater().
 * 5. Pause and resume synchronization.
 */
public class QueueMonitorThread extends Thread {

    private volatile boolean running = true;
    private volatile boolean paused = false;
    private final long intervalMillis;
    private final Runnable updateCallback;
    private long tickCount = 0;

    /**
     * Constructs a new QueueMonitorThread.
     *
     * @param intervalMillis The time between updates in milliseconds (e.g. 5000 ms).
     * @param updateCallback The Runnable to execute on each tick (dispatched safely onto Swing EDT).
     */
    public QueueMonitorThread(long intervalMillis, Runnable updateCallback) {
        super("MediQueue-LiveMonitorThread");
        setDaemon(true); // Daemon thread automatically exits when application closes
        this.intervalMillis = Math.max(1000, intervalMillis);
        this.updateCallback = updateCallback;
    }

    @Override
    public void run() {
        while (running && !isInterrupted()) {
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                // Thread interrupted during sleep -> terminate cleanly
                break;
            }

            if (!running) {
                break;
            }

            if (paused) {
                continue;
            }

            tickCount++;

            // Safely dispatch the GUI update on the Swing Event Dispatch Thread (EDT)
            if (updateCallback != null) {
                SwingUtilities.invokeLater(updateCallback);
            }
        }
    }

    /**
     * Pauses the periodic queue refresh without terminating the thread.
     */
    public synchronized void pauseMonitoring() {
        this.paused = true;
    }

    /**
     * Resumes periodic queue refreshing.
     */
    public synchronized void resumeMonitoring() {
        this.paused = false;
    }

    /**
     * Stops the monitor thread gracefully and interrupts any active sleep.
     */
    public synchronized void stopMonitoring() {
        this.running = false;
        this.interrupt();
    }

    /**
     * Returns whether the thread is actively monitoring (running and not paused).
     */
    public boolean isMonitoring() {
        return running && !paused && isAlive();
    }

    /**
     * Returns true if the monitor thread is currently paused.
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Returns the total number of monitor update ticks executed so far.
     */
    public long getTickCount() {
        return tickCount;
    }

    /**
     * Returns the refresh interval in milliseconds.
     */
    public long getIntervalMillis() {
        return intervalMillis;
    }
}
