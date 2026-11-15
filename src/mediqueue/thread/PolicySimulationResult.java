package mediqueue.thread;

import mediqueue.service.TriageService.QueuePolicy;

/**
 * Encapsulates the benchmark results and performance metrics computed
 * by an individual simulation worker thread.
 */
public class PolicySimulationResult {

    private final QueuePolicy policy;
    private final String threadName;
    private final int totalPatients;
    private final double averageWaitMinutes;
    private final int maxWaitMinutes;
    private final int overdueCount;
    private final double overduePercentage;
    private final int starvedRoutineCount;
    private final double starvationPercentage;
    private final long executionTimeNanos;

    public PolicySimulationResult(QueuePolicy policy, String threadName, int totalPatients,
                                  double averageWaitMinutes, int maxWaitMinutes,
                                  int overdueCount, int starvedRoutineCount,
                                  long executionTimeNanos) {
        this.policy = policy;
        this.threadName = threadName;
        this.totalPatients = totalPatients;
        this.averageWaitMinutes = averageWaitMinutes;
        this.maxWaitMinutes = maxWaitMinutes;
        this.overdueCount = overdueCount;
        this.overduePercentage = totalPatients > 0 ? (overdueCount * 100.0 / totalPatients) : 0.0;
        this.starvedRoutineCount = starvedRoutineCount;
        this.starvationPercentage = totalPatients > 0 ? (starvedRoutineCount * 100.0 / totalPatients) : 0.0;
        this.executionTimeNanos = executionTimeNanos;
    }

    public QueuePolicy getPolicy() {
        return policy;
    }

    public String getThreadName() {
        return threadName;
    }

    public int getTotalPatients() {
        return totalPatients;
    }

    public double getAverageWaitMinutes() {
        return averageWaitMinutes;
    }

    public int getMaxWaitMinutes() {
        return maxWaitMinutes;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public double getOverduePercentage() {
        return overduePercentage;
    }

    public int getStarvedRoutineCount() {
        return starvedRoutineCount;
    }

    public double getStarvationPercentage() {
        return starvationPercentage;
    }

    public long getExecutionTimeMillis() {
        return executionTimeNanos / 1000000L;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    @Override
    public String toString() {
        return String.format("[%s via %s] Avg Wait: %.1f mins | Max Wait: %d mins | Overdue: %d (%.1f%%) | Starved: %d (%.1f%%) | Thread Runtime: %d ms",
                policy, threadName, averageWaitMinutes, maxWaitMinutes, overdueCount, overduePercentage,
                starvedRoutineCount, starvationPercentage, getExecutionTimeMillis());
    }
}
