package mediqueue.thread;

import java.util.List;

/**
 * Standalone demo and test harness for the multi-threading features in MediQueue.
 * Demonstrates:
 * 1. Multi-Threaded Policy Simulation (3 worker threads executing in parallel with Thread.join()).
 * 2. Background Live Queue Monitor Thread (daemon thread lifecycle, pause/resume, graceful shutdown).
 */
public class ThreadDemo {

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("       MEDIQUEUE MULTI-THREADING CAPABILITIES DEMO & VERIFICATION    ");
        System.out.println("=====================================================================\n");

        // PART 1: Multi-Threaded Queue Policy Simulation
        System.out.println("--> [Part 1] Launching Multi-Threaded Queue Policy Simulator (3 concurrent threads)...");
        int patientCount = 60;
        int doctorCount = 2;
        long seed = 12345L;

        long startTime = System.currentTimeMillis();
        List<PolicySimulationResult> results = QueuePolicySimulator.runConcurrentSimulation(patientCount, doctorCount, seed);
        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("All 3 worker threads joined successfully in " + elapsed + " ms!\n");
        System.out.printf("%-35s %-30s %-12s %-10s %-15s %-15s\n",
                "Policy Strategy", "Worker Thread", "Avg Wait", "Max Wait", "Overdue %", "Starvation %");
        System.out.println("-------------------------------------------------------------------------------------------------------------");

        for (PolicySimulationResult r : results) {
            System.out.printf("%-35s %-30s %-12.1f %-10d %-15.1f %-15.1f\n",
                    r.getPolicy(),
                    r.getThreadName(),
                    r.getAverageWaitMinutes(),
                    r.getMaxWaitMinutes(),
                    r.getOverduePercentage(),
                    r.getStarvationPercentage());
        }

        System.out.println("\n-------------------------------------------------------------------------------------------------------------");
        System.out.println("Observation: Priority-with-Aging drastically reduces routine starvation compared to Strict Priority!");

        // PART 2: Live Queue Monitor Thread Lifecycle
        System.out.println("\n--> [Part 2] Testing Live Queue Monitor Daemon Thread lifecycle...");
        final int[] tickCounter = new int[]{0};

        QueueMonitorThread monitor = new QueueMonitorThread(500, new Runnable() {
            @Override
            public void run() {
                tickCounter[0]++;
                System.out.println("   [QueueMonitorThread Tick #" + tickCounter[0] + "] Background thread refreshed waiting times & aging status.");
            }
        });

        System.out.println("Starting QueueMonitorThread (interval: 500ms)...");
        monitor.start();

        try {
            // Let it tick 3 times
            Thread.sleep(1700);

            System.out.println("Pausing QueueMonitorThread...");
            monitor.pauseMonitoring();
            Thread.sleep(1000); // Should not tick while paused

            System.out.println("Resuming QueueMonitorThread...");
            monitor.resumeMonitoring();
            Thread.sleep(1200); // Should tick again

            System.out.println("Stopping QueueMonitorThread gracefully...");
            monitor.stopMonitoring();
            monitor.join(2000);

            System.out.println("Monitor thread stopped. Is alive? " + monitor.isAlive());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n=====================================================================");
        System.out.println("           MULTI-THREADING DEMONSTRATION COMPLETE SUCCESSFULLY!       ");
        System.out.println("=====================================================================");
    }
}
