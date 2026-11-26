package gui;

import model.AccessRequest;
import model.GPU;
import model.GPUSystem;
import model.Job;
import model.Scheduler;
import model.Student;
import model.TimeAllowance;
import model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * The administrator window: the dashboard (like the example in the project description)
 * and tabs for registration requests, students and all jobs.
 */
public class AdminFrame extends DashboardFrame {

    // dashboard
    private JLabel runningValue = new JLabel();
    private JLabel queuedValue = new JLabel();
    private JLabel completedValue = new JLabel();
    private JLabel cancelledValue = new JLabel();
    private JLabel busyValue = new JLabel();
    private JLabel utilizationValue = new JLabel();
    private JLabel[] gpuBadges = new JLabel[4];
    private JLabel[] gpuJobs = new JLabel[4];
    private JLabel[] gpuRemaining = new JLabel[4];
    private JLabel[] gpuFinish = new JLabel[4];
    private DefaultTableModel waitingModel = createModel("Job", "Student", "Application", "Duration",
            "Position", "Predicted start", "Predicted finish", "Wait");
    private DefaultTableModel completedModel = createModel("Job", "GPU", "Submitted", "Start", "Finish",
            "Runtime", "Queue wait", "Charged");
    private DefaultTableModel allowanceModel = createModel("Student", "Allocated", "Used", "Reserved", "Available");

    // other tabs
    private DefaultTableModel requestModel = createModel("Request", "University ID", "Name", "Email",
            "Phone number", "Password", "Status", "Request time");
    private JTable requestTable = new JTable(requestModel);
    private JTextField searchField = new JTextField(12);
    private DefaultTableModel studentModel = createModel("University ID", "Name", "Email", "Phone number",
            "Allocated", "Used", "Reserved", "Available");
    private JTable studentTable = new JTable(studentModel);
    private DefaultTableModel studentJobsModel = createModel("Job", "Title", "Task size", "Status", "GPU", "Charged");
    private DefaultTableModel historyModel = createModel("Time", "Minutes", "Reason");
    private DefaultTableModel jobModel = createModel("Job", "Student", "Application", "Status", "GPU", "Position",
            "Start", "Finish", "Remaining wait", "Cancelled by", "Reason");
    private JTable jobTable = new JTable(jobModel);

    // buttons
    private JButton advanceButton = createButton("Advance Minute", TEAL);
    private JButton approveButton = createButton("Approve", GREEN);
    private JButton rejectButton = createButton("Reject", RED);
    private JButton searchButton = createButton("Search", NAVY);
    private JButton extraButton = createButton("Grant Extra Time", GREEN);
    private JButton changeButton = createButton("Change Allocated Time", NAVY);
    private JButton cancelButton = createButton("Cancel Job", RED);

    public AdminFrame(GPUSystem system, User user) {
        super(system, user, "GPU Utilization Dashboard", "Administrator  |  Simulation");
        timeBox.add(advanceButton, 2);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabs.addTab("Dashboard", new JScrollPane(content));
        tabs.addTab("Registration Requests", createRequestsTab());
        tabs.addTab("Students", createStudentsTab());
        tabs.addTab("All Jobs", createJobsTab());
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        add(tabs, BorderLayout.CENTER);
        createDashboard();

        EventHandler handler = new EventHandler();
        JButton[] buttons = {advanceButton, approveButton, rejectButton, searchButton, extraButton,
                changeButton, cancelButton};
        for (JButton b : buttons) {
            b.addActionListener(handler);
        }
        refreshAll();
    }

    // ================= building the tabs =================

    private void createDashboard() {
        // summary strip
        JPanel summary = new JPanel(new GridLayout(1, 6));
        summary.setBackground(Color.WHITE);
        summary.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));
        summary.add(createStat("Running", runningValue, BLUE));
        summary.add(createStat("Queued", queuedValue, ORANGE));
        summary.add(createStat("Completed", completedValue, GREEN));
        summary.add(createStat("Cancelled", cancelledValue, RED));
        summary.add(createStat("Busy GPUs", busyValue, NAVY));
        summary.add(createStat("Utilization", utilizationValue, TEAL));

        // one card for each GPU
        JPanel gpuCards = new JPanel(new GridLayout(1, 4, 12, 0));
        gpuCards.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            gpuCards.add(createGpuCard(i));
        }

        // completed jobs and student allowances next to each other
        JPanel bottom = new JPanel(new BorderLayout(14, 0));
        bottom.setOpaque(false);
        JTable completedTable = new JTable(completedModel);
        setColumnWidths(completedTable, 45, 55, 130, 130, 130, 70, 80, 70);
        bottom.add(createSection("Completed jobs", createTable(completedTable, 7)), BorderLayout.CENTER);
        JPanel allowances = createSection("Student allowances", createTable(new JTable(allowanceModel), 7));
        allowances.setPreferredSize(new java.awt.Dimension(420, 10));
        bottom.add(allowances, BorderLayout.EAST);

        content.add(summary);
        content.add(javax.swing.Box.createVerticalStrut(12));
        content.add(createSection("GPU status", gpuCards));
        content.add(javax.swing.Box.createVerticalStrut(12));
        JTable waitingTable = new JTable(waitingModel);
        setColumnWidths(waitingTable, 50, 90, 120, 70, 60, 130, 130, 60);
        content.add(createSection("Waiting jobs", createTable(waitingTable, 5)));
        content.add(javax.swing.Box.createVerticalStrut(12));
        content.add(bottom);
    }

    private JPanel createGpuCard(int i) {
        gpuBadges[i] = new JLabel("", JLabel.CENTER);
        gpuBadges[i].setOpaque(true);
        gpuBadges[i].setForeground(Color.WHITE);
        gpuBadges[i].setFont(new Font("Segoe UI", Font.BOLD, 13));
        gpuBadges[i].setPreferredSize(new java.awt.Dimension(80, 24));
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(232, 245, 236));
        top.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        top.add(createLabel("GPU" + (i + 1), 18, true, Color.BLACK), BorderLayout.WEST);
        top.add(gpuBadges[i], BorderLayout.EAST);

        gpuJobs[i] = createLabel("", 14, true, Color.BLACK);
        gpuRemaining[i] = createLabel("", 14, true, Color.BLACK);
        gpuFinish[i] = createLabel("", 14, true, Color.BLACK);
        JPanel rows = new JPanel(new GridLayout(3, 2, 0, 4));
        rows.setBackground(Color.WHITE);
        rows.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        rows.add(createLabel("Current job:", 14, false, Color.BLACK));
        rows.add(gpuJobs[i]);
        rows.add(createLabel("Time remaining:", 14, false, Color.BLACK));
        rows.add(gpuRemaining[i]);
        rows.add(createLabel("Finish (simulated):", 14, false, Color.BLACK));
        rows.add(gpuFinish[i]);

        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(LINE));
        card.add(top, BorderLayout.NORTH);
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel createRequestsTab() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.add(approveButton);
        buttons.add(rejectButton);
        return createTab(createSection("Registration requests", createTable(requestTable, 15)), buttons);
    }

    private JPanel createStudentsTab() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(createLabel("University ID:", 14, false, Color.BLACK));
        top.add(searchField);
        top.add(searchButton);
        top.add(extraButton);
        top.add(changeButton);

        JPanel details = new JPanel(new GridLayout(1, 2, 12, 0));
        details.setOpaque(false);
        details.add(createSection("Jobs of the selected student", createTable(new JTable(studentJobsModel), 6)));
        details.add(createSection("Extra time history", createTable(new JTable(historyModel), 6)));

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 12));
        center.setOpaque(false);
        center.add(createSection("Approved students (click a student to see details)", createTable(studentTable, 8)));
        center.add(details);
        studentTable.getSelectionModel().addListSelectionListener(e -> showStudentDetails());
        return createTab(center, top);
    }

    private JPanel createJobsTab() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(createLabel("Start and finish times of queued jobs (and finish of running jobs) are predictions.",
                13, false, GRAY));
        setColumnWidths(jobTable, 45, 90, 110, 85, 55, 60, 135, 135, 95, 95, 170);
        return createTab(createSection("All jobs", createTable(jobTable, 20)), buttons);
    }

    private JPanel createTab(JPanel center, JPanel buttons) {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setBackground(BACKGROUND);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        tab.add(center, BorderLayout.CENTER);
        tab.add(buttons, BorderLayout.SOUTH);
        return tab;
    }

    // ================= button actions =================

    private class EventHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                if (e.getSource() == advanceButton) {
                    system.advanceMinute();
                } else if (e.getSource() == approveButton) {
                    approveRequest();
                } else if (e.getSource() == rejectButton) {
                    int id = getSelectedId(requestTable);
                    if (id != -1) {
                        system.rejectRequest(id);
                    }
                } else if (e.getSource() == searchButton) {
                    searchStudent();
                } else if (e.getSource() == extraButton) {
                    changeTime(true);
                } else if (e.getSource() == changeButton) {
                    changeTime(false);
                } else if (e.getSource() == cancelButton) {
                    cancelJob();
                }
                saveData();
                refreshAll();
            } catch (NumberFormatException ex) {
                showError("Please enter a whole number.");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void approveRequest() {
        int id = getSelectedId(requestTable);
        if (id == -1) {
            return;
        }
        String minutes = JOptionPane.showInputDialog(this, "GPU minutes for this student:", "100");
        if (minutes != null) {
            system.approveRequest(id, Integer.parseInt(minutes.trim()));
        }
    }

    private void searchStudent() {
        Student s = system.findStudent(searchField.getText());
        for (int row = 0; row < studentTable.getRowCount(); row++) {
            if (studentTable.getValueAt(row, 0).equals(s.getUniversityId())) {
                studentTable.setRowSelectionInterval(row, row);
            }
        }
    }

    // extra = true: grant extra time, extra = false: change the allocated time
    private void changeTime(boolean extra) {
        int row = studentTable.getSelectedRow();
        if (row == -1) {
            showError("Please select a student first.");
            return;
        }
        String id = studentTable.getValueAt(row, 0).toString();
        String question = extra ? "Extra minutes for " + id + ":" : "New allocated time for " + id + ":";
        String minutes = JOptionPane.showInputDialog(this, question);
        if (minutes == null) {
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "Reason:");
        if (reason == null) {
            return;
        }
        if (extra) {
            system.grantExtraTime(id, Integer.parseInt(minutes.trim()), reason);
        } else {
            system.changeAllocatedTime(id, Integer.parseInt(minutes.trim()), reason);
        }
    }

    private void cancelJob() {
        int id = getSelectedId(jobTable);
        if (id == -1) {
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "Reason for cancelling job J" + id + ":");
        if (reason != null) {
            system.cancelJob(user, id, reason);
        }
    }

    // ================= refresh =================

    public void refresh() {
        Scheduler s = system.getScheduler();
        LocalDateTime now = s.getCurrentTime();
        HashMap<Integer, LocalDateTime> predicted = s.predictStartTimes();

        // summary
        runningValue.setText("" + s.countJobs("Running"));
        queuedValue.setText("" + s.countJobs("Queued"));
        completedValue.setText("" + s.countJobs("Completed"));
        cancelledValue.setText("" + s.countJobs("Cancelled"));
        busyValue.setText(s.countBusyGpus() + " / 4");
        double utilization = s.getUtilization();
        utilizationValue.setText(utilization < 0 ? "N/A" : String.format("%.0f%%", utilization));

        // GPU cards
        for (int i = 0; i < 4; i++) {
            GPU gpu = s.getGpus().get(i);
            if (gpu.isAvailable()) {
                gpuBadges[i].setText("Free");
                gpuBadges[i].setBackground(GRAY);
                gpuJobs[i].setText("-");
                gpuRemaining[i].setText("-");
                gpuFinish[i].setText("-");
            } else {
                gpuBadges[i].setText("Busy");
                gpuBadges[i].setBackground(GREEN);
                gpuJobs[i].setText("J" + gpu.getCurrentJob().getJobId());
                gpuRemaining[i].setText(gpu.getRemainingRuntime() + " min");
                gpuFinish[i].setText(time(now.plusMinutes(gpu.getRemainingRuntime())).substring(11));
            }
        }

        // tables
        waitingModel.setRowCount(0);
        for (Job job : s.getQueue()) {
            LocalDateTime start = predicted.get(job.getJobId());
            waitingModel.addRow(new Object[]{"J" + job.getJobId(), job.getStudentId(), job.getApplicationName(),
                    job.getEstimatedDuration() + " min", s.getQueuePosition(job), time(start),
                    time(start.plusMinutes(job.getEstimatedDuration())), s.getWaitMinutes(start) + " min"});
        }

        completedModel.setRowCount(0);
        jobModel.setRowCount(0);
        for (Job job : s.getJobs()) {
            if (job.getStatus().equals("Completed")) {
                completedModel.addRow(new Object[]{"J" + job.getJobId(), job.getGpuName(),
                        time(job.getSubmissionTime()), time(job.getStartTime()), time(job.getFinishTime()),
                        job.getRuntime() + " min", job.getQueueWait(), job.getChargedTime() + " min"});
            }
            String start = time(job.getStartTime());
            String finish = time(job.getFinishTime());
            String position = "-";
            String wait = "-";
            if (job.getStatus().equals("Queued")) {
                LocalDateTime p = predicted.get(job.getJobId());
                start = time(p);
                finish = time(p.plusMinutes(job.getEstimatedDuration()));
                position = "" + s.getQueuePosition(job);
                wait = s.getWaitMinutes(p) + " min";
            } else if (job.getStatus().equals("Running")) {
                finish = time(now.plusMinutes(job.getRemainingRuntime()));
            }
            jobModel.addRow(new Object[]{"J" + job.getJobId(), job.getStudentId(), job.getApplicationName(),
                    job.getStatus(), job.getGpuName(), position, start, finish, wait, job.getCancelledBy(),
                    job.getCancelReason()});
        }

        allowanceModel.setRowCount(0);
        studentModel.setRowCount(0);
        for (Student st : system.getStudents().values()) {
            TimeAllowance a = st.getAllowance();
            allowanceModel.addRow(new Object[]{st.getUniversityId(), a.getAllocatedTime(), a.getUsedTime(),
                    a.getReservedTime(), a.getAvailableTime()});
            studentModel.addRow(new Object[]{st.getUniversityId(), st.getName(), st.getEmail(), st.getPhoneNumber(),
                    a.getAllocatedTime(), a.getUsedTime(), a.getReservedTime(), a.getAvailableTime()});
        }

        requestModel.setRowCount(0);
        for (AccessRequest r : system.getRequests()) {
            requestModel.addRow(new Object[]{r.getRequestId(), r.getUniversityId(), r.getName(), r.getEmail(),
                    r.getPhoneNumber(), r.getPassword(), r.getStatus(), time(r.getRequestTime())});
        }
    }

    // shows the jobs and extra-time history of the student selected in the Students tab
    private void showStudentDetails() {
        studentJobsModel.setRowCount(0);
        historyModel.setRowCount(0);
        int row = studentTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        Student st = system.findStudent(studentTable.getValueAt(row, 0).toString());
        for (Job job : system.getScheduler().getJobsOf(st.getUniversityId())) {
            studentJobsModel.addRow(new Object[]{"J" + job.getJobId(), job.getTitle(), job.getTaskSize(),
                    job.getStatus(), job.getGpuName(), job.getChargedTime() + " min"});
        }
        for (String entry : st.getAllowance().getHistory()) {
            historyModel.addRow(entry.split("\\|"));
        }
    }
}
