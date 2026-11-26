package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Runs the simulation: the shared clock, one FCFS queue and four GPUs.
 */
public class Scheduler {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ArrayList<GPU> gpus = new ArrayList<GPU>();
    private ArrayList<Job> jobs = new ArrayList<Job>();      // all jobs (history)
    private ArrayList<Job> queue = new ArrayList<Job>();     // waiting jobs in FCFS order
    private HashMap<String, Student> students;               // used to update allowances
    private LocalDateTime startTime;
    private LocalDateTime currentTime;
    private int nextJobId = 1;

    public Scheduler(HashMap<String, Student> students) {
        this.students = students;
        for (int i = 1; i <= 4; i++) {
            gpus.add(new GPU(i));
        }
    }

    // ---------------- time ----------------

    public static String format(LocalDateTime time) {
        return time == null ? "-" : time.format(FORMAT);
    }

    public static LocalDateTime parse(String text) {
        return text.equals("-") ? null : LocalDateTime.parse(text, FORMAT);
    }

    public void setStartTime(LocalDateTime time) {
        startTime = time;
        currentTime = time;
    }

    // moves the clock one minute, runs the jobs, completes finished ones and starts waiting ones
    public void advanceMinute() {
        currentTime = currentTime.plusMinutes(1);
        for (GPU gpu : gpus) {
            if (!gpu.isAvailable()) {
                gpu.runOneMinute();
                Job job = gpu.getCurrentJob();
                if (job.isFinished()) {
                    job.complete(currentTime);
                    settle(job);
                    gpu.release();
                }
            }
        }
        startWaitingJobs();
    }

    // ---------------- jobs ----------------

    public Job createJob(Student student, String title, String application, String description, String size) {
        Job job = new Job(nextJobId++, student.getUniversityId(), title, application, description, size, currentTime);
        jobs.add(job);
        queue.add(job);
        startWaitingJobs();
        return job;
    }

    // each free GPU (lowest ID first) takes the first job in the queue
    private void startWaitingJobs() {
        for (GPU gpu : gpus) {
            if (gpu.isAvailable() && !queue.isEmpty()) {
                Job job = queue.remove(0);
                job.start(gpu.getGpuId(), currentTime);
                gpu.assignJob(job);
            }
        }
    }

    public void cancelJob(Job job, String user, String reason) {
        boolean wasRunning = job.getStatus().equals("Running");
        job.cancel(user, reason, currentTime);
        settle(job);
        if (wasRunning) {
            gpus.get(job.getAssignedGpuId() - 1).release();
            startWaitingJobs();
        } else {
            queue.remove(job);
        }
    }

    // release the reservation and charge the job (done once, when the job ends)
    private void settle(Job job) {
        students.get(job.getStudentId()).getAllowance().settle(job.getEstimatedDuration(), job.getChargedTime());
    }

    // ---------------- predictions ----------------

    /**
     * Predicts the start time of every queued job.
     * We copy the time each GPU becomes free, then give each queued job (FCFS order)
     * to the GPU that is free first (lower ID on a tie). The real GPUs are not changed.
     */
    public HashMap<Integer, LocalDateTime> predictStartTimes() {
        HashMap<Integer, LocalDateTime> predictions = new HashMap<Integer, LocalDateTime>();
        LocalDateTime[] freeAt = new LocalDateTime[4];
        for (int i = 0; i < 4; i++) {
            freeAt[i] = currentTime.plusMinutes(gpus.get(i).getRemainingRuntime());
        }
        for (Job job : queue) {
            int best = 0;
            for (int i = 1; i < 4; i++) {
                if (freeAt[i].isBefore(freeAt[best])) {
                    best = i;
                }
            }
            predictions.put(job.getJobId(), freeAt[best]);
            freeAt[best] = freeAt[best].plusMinutes(job.getEstimatedDuration());
        }
        return predictions;
    }

    public int getQueuePosition(Job job) {
        return queue.indexOf(job) + 1;
    }

    public long getWaitMinutes(LocalDateTime predictedStart) {
        return Duration.between(currentTime, predictedStart).toMinutes();
    }

    // ---------------- statistics ----------------

    // busy GPU minutes / (4 x elapsed minutes) x 100, or -1 at time zero
    public double getUtilization() {
        long elapsed = Duration.between(startTime, currentTime).toMinutes();
        if (elapsed == 0) {
            return -1;
        }
        int busy = 0;
        for (GPU gpu : gpus) {
            busy += gpu.getBusyMinutes();
        }
        return busy * 100.0 / (4 * elapsed);
    }

    public int countJobs(String status) {
        int count = 0;
        for (Job job : jobs) {
            if (job.getStatus().equals(status)) {
                count++;
            }
        }
        return count;
    }

    public int countBusyGpus() {
        int count = 0;
        for (GPU gpu : gpus) {
            if (!gpu.isAvailable()) {
                count++;
            }
        }
        return count;
    }

    public Job findJob(int jobId) {
        for (Job job : jobs) {
            if (job.getJobId() == jobId) {
                return job;
            }
        }
        return null;
    }

    public ArrayList<Job> getJobsOf(String studentId) {
        ArrayList<Job> list = new ArrayList<Job>();
        for (Job job : jobs) {
            if (job.getStudentId().equals(studentId)) {
                list.add(job);
            }
        }
        return list;
    }

    // puts a saved job back when the files are loaded (jobs are saved in ID order = FCFS order)
    public void restoreJob(Job job) {
        jobs.add(job);
        if (job.getStatus().equals("Queued")) {
            queue.add(job);
        } else if (job.getStatus().equals("Running")) {
            gpus.get(job.getAssignedGpuId() - 1).assignJob(job);
        }
        nextJobId = Math.max(nextJobId, job.getJobId() + 1);
    }

    public ArrayList<GPU> getGpus() { return gpus; }
    public ArrayList<Job> getJobs() { return jobs; }
    public ArrayList<Job> getQueue() { return queue; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getCurrentTime() { return currentTime; }
    public void setCurrentTime(LocalDateTime currentTime) { this.currentTime = currentTime; }
}
