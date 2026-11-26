package model;

/**
 * One NVIDIA H100 GPU. It runs at most one job at a time.
 */
public class GPU {

    private int gpuId;
    private Job currentJob;     // null when the GPU is free
    private int busyMinutes;    // total minutes spent running jobs (for utilization)

    public GPU(int gpuId) {
        this.gpuId = gpuId;
    }

    public boolean isAvailable() {
        return currentJob == null;
    }

    public void assignJob(Job job) {
        currentJob = job;
    }

    public void release() {
        currentJob = null;
    }

    // runs the current job for one simulated minute
    public void runOneMinute() {
        if (currentJob != null) {
            currentJob.runOneMinute();
            busyMinutes++;
        }
    }

    public int getRemainingRuntime() {
        if (currentJob == null) {
            return 0;
        }
        return currentJob.getRemainingRuntime();
    }

    public int getGpuId() { return gpuId; }
    public Job getCurrentJob() { return currentJob; }
    public int getBusyMinutes() { return busyMinutes; }
    public void setBusyMinutes(int busyMinutes) { this.busyMinutes = busyMinutes; }
}
