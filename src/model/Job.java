package model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * A GPU job. The attributes follow the table in the project description.
 * States: Queued -> Running or Cancelled, Running -> Completed or Cancelled.
 */
public class Job implements Saveable {

    public static final String[] TASK_SIZES = {"Small", "Medium", "Large", "Very large"};

    private int jobId;
    private String studentId;
    private String title;
    private String applicationName;
    private String description;
    private String taskSize;
    private int workloadUnits;
    private int estimatedDuration;
    private LocalDateTime submissionTime;
    private String status;
    private int assignedGpuId;              // 0 = no GPU yet
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    // extra records needed by the allowance rules
    private int runtime;                    // minutes already run on a GPU
    private int chargedTime;                // minutes charged to the student
    private String cancelledBy = "-";
    private String cancelReason = "-";
    private LocalDateTime cancelTime;

    public Job(int jobId, String studentId, String title, String applicationName, String description,
               String taskSize, LocalDateTime submissionTime) {
        this.jobId = jobId;
        this.studentId = studentId;
        this.title = title;
        this.applicationName = applicationName;
        this.description = description;
        this.taskSize = taskSize;
        this.workloadUnits = getWorkloadUnits(taskSize);
        this.estimatedDuration = workloadUnits;          // 1 workload unit = 1 minute
        this.submissionTime = submissionTime;
        this.status = "Queued";
    }

    // Small = 5, Medium = 15, Large = 30, Very large = 60
    public static int getWorkloadUnits(String taskSize) {
        if (taskSize.equals("Small")) return 5;
        if (taskSize.equals("Medium")) return 15;
        if (taskSize.equals("Large")) return 30;
        if (taskSize.equals("Very large")) return 60;
        throw new IllegalArgumentException("Unknown task size: " + taskSize);
    }

    public void start(int gpuId, LocalDateTime time) {
        assignedGpuId = gpuId;
        startTime = time;
        status = "Running";
    }

    public void runOneMinute() {
        runtime++;
    }

    public boolean isFinished() {
        return runtime >= estimatedDuration;
    }

    public void complete(LocalDateTime time) {
        finishTime = time;
        status = "Completed";
        chargedTime = estimatedDuration;
    }

    // a queued job is free to cancel, a running job is charged its runtime
    public void cancel(String user, String reason, LocalDateTime time) {
        if (!status.equals("Queued") && !status.equals("Running")) {
            throw new IllegalStateException("Job " + jobId + " is " + status + " and cannot be cancelled.");
        }
        if (status.equals("Running")) {
            finishTime = time;
            chargedTime = runtime;
        }
        cancelledBy = user;
        cancelReason = reason;
        cancelTime = time;
        status = "Cancelled";
    }

    public int getRemainingRuntime() {
        return estimatedDuration - runtime;
    }

    // total queue wait = start time - submission time
    public String getQueueWait() {
        if (startTime == null) {
            return "-";
        }
        return Duration.between(submissionTime, startTime).toMinutes() + " min";
    }

    public int getJobId() { return jobId; }
    public String getStudentId() { return studentId; }
    public String getTitle() { return title; }
    public String getApplicationName() { return applicationName; }
    public String getDescription() { return description; }
    public String getTaskSize() { return taskSize; }
    public int getWorkloadUnits() { return workloadUnits; }
    public int getEstimatedDuration() { return estimatedDuration; }
    public LocalDateTime getSubmissionTime() { return submissionTime; }
    public String getStatus() { return status; }
    public int getAssignedGpuId() { return assignedGpuId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public int getRuntime() { return runtime; }
    public int getChargedTime() { return chargedTime; }
    public String getCancelledBy() { return cancelledBy; }
    public String getCancelReason() { return cancelReason; }
    public LocalDateTime getCancelTime() { return cancelTime; }

    public String getGpuName() {
        return assignedGpuId == 0 ? "-" : "GPU" + assignedGpuId;
    }

    @Override
    public String toString() {
        return "J" + jobId + " " + title + " (" + status + ")";
    }

    @Override
    public String toFileLine() {
        return jobId + "|" + studentId + "|" + title + "|" + applicationName + "|" + description + "|"
                + taskSize + "|" + status + "|" + Scheduler.format(submissionTime) + "|" + assignedGpuId + "|"
                + Scheduler.format(startTime) + "|" + Scheduler.format(finishTime) + "|" + runtime + "|"
                + chargedTime + "|" + cancelledBy + "|" + cancelReason + "|" + Scheduler.format(cancelTime);
    }

    // creates a Job from a line of jobs.txt
    public static Job fromFileLine(String line) {
        String[] f = line.split("\\|");
        Job job = new Job(Integer.parseInt(f[0]), f[1], f[2], f[3], f[4], f[5], Scheduler.parse(f[7]));
        job.status = f[6];
        job.assignedGpuId = Integer.parseInt(f[8]);
        job.startTime = Scheduler.parse(f[9]);
        job.finishTime = Scheduler.parse(f[10]);
        job.runtime = Integer.parseInt(f[11]);
        job.chargedTime = Integer.parseInt(f[12]);
        job.cancelledBy = f[13];
        job.cancelReason = f[14];
        job.cancelTime = Scheduler.parse(f[15]);
        return job;
    }
}
