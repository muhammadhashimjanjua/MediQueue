package mediqueue.thread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.swing.SwingUtilities;
import mediqueue.model.TriageCategory;
import mediqueue.service.TriageService.QueuePolicy;

/**
 * Multi-Threaded Queue Policy Benchmark Simulator.
 *
 * Implements the Core Simulator Module described in the MediQueue Project Charter:
 * Spawns multiple concurrent worker threads to simulate patient traffic under three
 * queue ordering strategies simultaneously on the exact same patient arrival stream:
 *  1. Thread-FCFS (First-Come, First-Served)
 *  2. Thread-StrictPriority (Manchester Triage raw category priority)
 *  3. Thread-PriorityWithAging (Anti-Starvation aging: +1 level per 30 mins)
 *
 * Concurrency features demonstrated:
 * 1. Spawning multiple independent worker threads running concurrently.
 * 2. Implementing java.lang.Runnable for task decoupling.
 * 3. Barrier synchronization via thread.join() to aggregate results.
 * 4. Asynchronous non-blocking GUI execution with callback dispatch on Swing EDT.
 */
public class QueuePolicySimulator {

    /**
     * Callback interface for asynchronous simulation completion.
     */
    public interface SimulationCallback {
        void onSimulationComplete(List<PolicySimulationResult> results, long totalWallClockTimeMs);
        void onSimulationError(Exception ex);
    }

    /**
     * Generates a reproducible list of simulated patient arrivals.
     * Uses clinical emergency room triage arrival distributions:
     * - ~5% Category 1 (Resuscitation)
     * - ~15% Category 2 (Very Urgent)
     * - ~35% Category 3 (Urgent)
     * - ~30% Category 4 (Standard)
     * - ~15% Category 5 (Non-Urgent)
     */
    public static List<SimulatedPatient> generateArrivals(int count, long seed) {
        List<SimulatedPatient> list = new ArrayList<SimulatedPatient>(count);
        Random rand = new Random(seed);

        int currentMinute = 0;
        for (int i = 1; i <= count; i++) {
            // Inter-arrival gap: 3 to 10 minutes (average ~6 mins)
            currentMinute += 3 + rand.nextInt(8);

            // Realistic Manchester triage distribution
            int roll = rand.nextInt(100);
            TriageCategory cat;
            if (roll < 5) {
                cat = TriageCategory.CATEGORY_1; // Immediate
            } else if (roll < 20) {
                cat = TriageCategory.CATEGORY_2; // Very Urgent
            } else if (roll < 55) {
                cat = TriageCategory.CATEGORY_3; // Urgent
            } else if (roll < 85) {
                cat = TriageCategory.CATEGORY_4; // Standard
            } else {
                cat = TriageCategory.CATEGORY_5; // Non-Urgent
            }

            // Doctor consultation time: 10 to 22 minutes
            int serviceDuration = 10 + rand.nextInt(13);

            list.add(new SimulatedPatient(i, "Patient #" + i, cat, currentMinute, serviceDuration));
        }
        return list;
    }

    /**
     * Runnable worker that simulates queue processing for one specific QueuePolicy.
     */
    public static class PolicyWorker implements Runnable {
        private final QueuePolicy policy;
        private final List<SimulatedPatient> arrivals;
        private final int doctorCount;
        private volatile PolicySimulationResult result;

        public PolicyWorker(QueuePolicy policy, List<SimulatedPatient> arrivals, int doctorCount) {
            this.policy = policy;
            this.arrivals = arrivals;
            this.doctorCount = Math.max(1, doctorCount);
        }

        @Override
        public void run() {
            long startNano = System.nanoTime();
            int total = arrivals.size();
            if (total == 0) {
                result = new PolicySimulationResult(policy, Thread.currentThread().getName(), 0, 0, 0, 0, 0, 0);
                return;
            }

            // Simulation state
            int[] doctorFreeAt = new int[doctorCount];
            List<SimulatedPatient> queue = new ArrayList<SimulatedPatient>();

            int arrivalIdx = 0;
            int currentTime = arrivals.get(0).getArrivalMinute();
            int served = 0;

            long totalWait = 0;
            int maxWait = 0;
            int overdueCount = 0;
            int starvedRoutineCount = 0;

            while (served < total) {
                // Admit all patients arriving up to currentTime
                while (arrivalIdx < total && arrivals.get(arrivalIdx).getArrivalMinute() <= currentTime) {
                    queue.add(arrivals.get(arrivalIdx));
                    arrivalIdx++;
                }

                // Check for free doctors
                int freeDoctorIdx = -1;
                for (int d = 0; d < doctorCount; d++) {
                    if (doctorFreeAt[d] <= currentTime) {
                        freeDoctorIdx = d;
                        break;
                    }
                }

                if (freeDoctorIdx != -1 && !queue.isEmpty()) {
                    // Pick the next patient based on this thread's policy
                    int bestIdx = selectNextPatientIndex(queue, policy, currentTime);
                    SimulatedPatient patient = queue.remove(bestIdx);

                    int waitTime = Math.max(0, currentTime - patient.getArrivalMinute());
                    totalWait += waitTime;
                    if (waitTime > maxWait) {
                        maxWait = waitTime;
                    }

                    // MTS Target Breached Check
                    if (waitTime > patient.getCategory().getTargetWaitMinutes()) {
                        overdueCount++;
                    }

                    // Anti-Starvation Metric: Cat 4 or 5 routine patient waiting > 120 mins
                    if (patient.getCategory().getLevel() >= 4 && waitTime > 120) {
                        starvedRoutineCount++;
                    }

                    doctorFreeAt[freeDoctorIdx] = currentTime + patient.getServiceMinutes();
                    served++;
                } else {
                    // Advance time to next discrete event
                    int nextEventTime = Integer.MAX_VALUE;

                    // Next arrival time
                    if (arrivalIdx < total) {
                        nextEventTime = Math.min(nextEventTime, arrivals.get(arrivalIdx).getArrivalMinute());
                    }

                    // Earliest doctor becoming free
                    for (int d = 0; d < doctorCount; d++) {
                        if (doctorFreeAt[d] > currentTime) {
                            nextEventTime = Math.min(nextEventTime, doctorFreeAt[d]);
                        }
                    }

                    if (nextEventTime != Integer.MAX_VALUE && nextEventTime > currentTime) {
                        currentTime = nextEventTime;
                    } else {
                        currentTime++;
                    }
                }
            }

            long elapsedNanos = System.nanoTime() - startNano;
            double avgWait = total > 0 ? ((double) totalWait / total) : 0.0;

            result = new PolicySimulationResult(
                    policy,
                    Thread.currentThread().getName(),
                    total,
                    avgWait,
                    maxWait,
                    overdueCount,
                    starvedRoutineCount,
                    elapsedNanos
            );
        }

        public PolicySimulationResult getResult() {
            return result;
        }

        /**
         * Selects the next patient index from the queue according to the policy.
         */
        private int selectNextPatientIndex(List<SimulatedPatient> queue, QueuePolicy policy, int currentTime) {
            if (queue.size() == 1 || policy == QueuePolicy.FCFS) {
                return 0; // FCFS order is preserved by chronological admission
            }

            int bestIdx = 0;
            SimulatedPatient best = queue.get(0);

            for (int i = 1; i < queue.size(); i++) {
                SimulatedPatient curr = queue.get(i);
                if (policy == QueuePolicy.STRICT_PRIORITY) {
                    // Strict category urgency (Cat 1 < Cat 2 < ... < Cat 5)
                    int bestLvl = best.getCategory().getLevel();
                    int currLvl = curr.getCategory().getLevel();
                    if (currLvl < bestLvl || (currLvl == bestLvl && curr.getArrivalMinute() < best.getArrivalMinute())) {
                        bestIdx = i;
                        best = curr;
                    }
                } else if (policy == QueuePolicy.PRIORITY_WITH_AGING) {
                    // Effective priority after Anti-Starvation Aging (+1 level per 30 mins)
                    int bestEff = best.getEffectiveCategoryLevel(currentTime);
                    int currEff = curr.getEffectiveCategoryLevel(currentTime);
                    if (currEff < bestEff || (currEff == bestEff && curr.getArrivalMinute() < best.getArrivalMinute())) {
                        bestIdx = i;
                        best = curr;
                    }
                }
            }
            return bestIdx;
        }
    }

    /**
     * Executes the multi-threaded simulation function.
     * Spawns 3 concurrent worker threads, synchronizes at the completion barrier with join(),
     * and returns the aggregated benchmark results.
     *
     * @param patientCount Number of patient arrivals to simulate (e.g. 60).
     * @param doctorCount Number of concurrent doctors handling consultations (e.g. 2).
     * @param seed Random seed for identical arrivals across all three policies.
     * @return List of PolicySimulationResult containing metrics for each policy.
     */
    public static List<PolicySimulationResult> runConcurrentSimulation(int patientCount, int doctorCount, long seed) {
        // Generate reproducible arrival dataset
        List<SimulatedPatient> arrivals = generateArrivals(patientCount, seed);

        PolicyWorker workerFcfs = new PolicyWorker(QueuePolicy.FCFS, arrivals, doctorCount);
        PolicyWorker workerStrict = new PolicyWorker(QueuePolicy.STRICT_PRIORITY, arrivals, doctorCount);
        PolicyWorker workerAging = new PolicyWorker(QueuePolicy.PRIORITY_WITH_AGING, arrivals, doctorCount);

        // 1. Instantiate 3 dedicated threads
        Thread threadFcfs = new Thread(workerFcfs, "Thread-Worker-FCFS");
        Thread threadStrict = new Thread(workerStrict, "Thread-Worker-StrictPriority");
        Thread threadAging = new Thread(workerAging, "Thread-Worker-PriorityWithAging");

        // 2. Launch all worker threads concurrently
        threadFcfs.start();
        threadStrict.start();
        threadAging.start();

        // 3. Barrier synchronization: Wait for all 3 worker threads to complete
        try {
            threadFcfs.join();
            threadStrict.join();
            threadAging.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Concurrent policy simulation interrupted: " + e.getMessage());
        }

        // 4. Collect results
        List<PolicySimulationResult> results = new ArrayList<PolicySimulationResult>(3);
        if (workerFcfs.getResult() != null) results.add(workerFcfs.getResult());
        if (workerStrict.getResult() != null) results.add(workerStrict.getResult());
        if (workerAging.getResult() != null) results.add(workerAging.getResult());

        return results;
    }

    /**
     * Asynchronously executes the concurrent simulation in a background thread,
     * ensuring the Swing GUI never freezes, and delivers results via callback onto the EDT.
     */
    public static void runSimulationAsync(final int patientCount, final int doctorCount, final long seed, final SimulationCallback callback) {
        Thread launcherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long wallStart = System.currentTimeMillis();
                    final List<PolicySimulationResult> results = runConcurrentSimulation(patientCount, doctorCount, seed);
                    final long wallElapsed = System.currentTimeMillis() - wallStart;

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSimulationComplete(results, wallElapsed);
                            }
                        }
                    });
                } catch (final Exception ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSimulationError(ex);
                            }
                        }
                    });
                }
            }
        }, "Thread-SimulationLauncher");

        launcherThread.setDaemon(true);
        launcherThread.start();
    }
}
