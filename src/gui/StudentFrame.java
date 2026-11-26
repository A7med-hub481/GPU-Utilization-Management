package gui;

import model.GPUSystem;
import model.Job;
import model.Scheduler;
import model.Student;
import model.TimeAllowance;
import model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * The student window: allowance summary, a form to submit jobs, and the student's own jobs.
 */
public class StudentFrame extends DashboardFrame {

    private Student student;

    private JLabel allocatedValue = new JLabel();
    private JLabel usedValue = new JLabel();
    private JLabel reservedValue = new JLabel();
    private JLabel availableValue = new JLabel();

    private JTextField titleField = new JTextField();
    private JTextField applicationField = new JTextField();
    private JTextField descriptionField = new JTextField();
    private JComboBox<String> sizeBox = new JComboBox<String>(Job.TASK_SIZES);
    private JLabel durationLabel = createLabel("5 minutes", 14, true, NAVY);

    private JTextField searchField = new JTextField(14);
    private DefaultTableModel jobModel = createModel("Job", "Title", "Status", "GPU", "Start", "Finish",
            "Waiting time", "Result");
    private JTable jobTable = new JTable(jobModel);

    private JButton refreshButton = createButton("Refresh", TEAL);
    private JButton submitButton = createButton("Submit Job", TEAL);
    private JButton searchButton = createButton("Search", NAVY);
    private JButton cancelButton = createButton("Cancel Job", RED);

    public StudentFrame(GPUSystem system, User user) {
        super(system, user, "Student Dashboard", user.getName() + "  |  " + user.getUsername());
        this.student = (Student) user;
        timeBox.add(refreshButton, 2);

        // summary strip
        JPanel summary = new JPanel(new GridLayout(1, 4));
        summary.setBackground(Color.WHITE);
        summary.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));
        summary.add(createStat("Allocated time", allocatedValue, NAVY));
        summary.add(createStat("Used time", usedValue, BLUE));
        summary.add(createStat("Reserved time", reservedValue, ORANGE));
        summary.add(createStat("Available time", availableValue, GREEN));

        // submit form
        JPanel form = new JPanel(new GridLayout(6, 1, 0, 6));
        form.setOpaque(false);
        form.add(createRow("Job title:", titleField));
        form.add(createRow("Application name:", applicationField));
        form.add(createRow("Description:", descriptionField));
        form.add(createRow("Task size:", sizeBox));
        form.add(createRow("Duration:", durationLabel));
        JPanel submitRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        submitRow.setOpaque(false);
        submitRow.add(submitButton);
        form.add(submitRow);
        JPanel formHolder = new JPanel(new BorderLayout());     // keeps the form at the top
        formHolder.setOpaque(false);
        formHolder.add(form, BorderLayout.NORTH);
        JPanel formSection = createSection("Submit a new job", formHolder);
        formSection.setPreferredSize(new Dimension(400, 10));

        // my jobs
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRow.setOpaque(false);
        searchRow.add(createLabel("Search:", 14, false, Color.BLACK));
        searchRow.add(searchField);
        searchRow.add(searchButton);
        searchRow.add(cancelButton);
        searchRow.add(createLabel("  Start/finish of waiting or running jobs are predictions.", 12, false, GRAY));
        JPanel jobsPanel = new JPanel(new BorderLayout(0, 6));
        jobsPanel.setOpaque(false);
        setColumnWidths(jobTable, 45, 150, 85, 55, 130, 130, 105, 210);
        jobsPanel.add(createTable(jobTable, 12), BorderLayout.CENTER);
        jobsPanel.add(searchRow, BorderLayout.SOUTH);

        JPanel middle = new JPanel(new BorderLayout(14, 0));
        middle.setOpaque(false);
        middle.add(formSection, BorderLayout.WEST);
        middle.add(createSection("My jobs", jobsPanel), BorderLayout.CENTER);

        content.add(summary);
        content.add(javax.swing.Box.createVerticalStrut(12));
        content.add(middle);
        add(content, BorderLayout.CENTER);

        EventHandler handler = new EventHandler();
        refreshButton.addActionListener(handler);
        submitButton.addActionListener(handler);
        searchButton.addActionListener(handler);
        cancelButton.addActionListener(handler);
        sizeBox.addActionListener(handler);
        refreshAll();
    }

    private JPanel createRow(String label, java.awt.Component field) {
        JLabel l = createLabel(label, 14, false, Color.BLACK);
        l.setPreferredSize(new Dimension(140, 26));
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(l, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    // ================= button actions =================

    private class EventHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                if (e.getSource() == sizeBox) {
                    String size = (String) sizeBox.getSelectedItem();
                    durationLabel.setText(Job.getWorkloadUnits(size) + " minutes");
                    return;
                } else if (e.getSource() == submitButton) {
                    submitJob();
                } else if (e.getSource() == cancelButton) {
                    cancelJob();
                }
                saveData();
                refreshAll();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void submitJob() {
        String size = (String) sizeBox.getSelectedItem();
        int minutes = Job.getWorkloadUnits(size);
        int answer = JOptionPane.showConfirmDialog(this, "This " + size + " job takes " + minutes + " minutes.\n"
                + minutes + " minutes will be reserved from your " + student.getAllowance().getAvailableTime()
                + " available minutes.\nSubmit the job?", "Confirm duration", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            Job job = system.submitJob(student, titleField.getText(), applicationField.getText(),
                    descriptionField.getText(), size);
            JOptionPane.showMessageDialog(this, "Job J" + job.getJobId() + " was accepted. Status: " + job.getStatus());
            titleField.setText("");
            applicationField.setText("");
            descriptionField.setText("");
        }
    }

    private void cancelJob() {
        int id = getSelectedId(jobTable);
        if (id == -1) {
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "Why do you want to cancel job J" + id + "?\n"
                + "A queued job costs nothing. A running job costs the minutes it has already run.");
        if (reason != null) {
            system.cancelJob(user, id, reason);
        }
    }

    // ================= refresh =================

    public void refresh() {
        TimeAllowance a = student.getAllowance();
        allocatedValue.setText(a.getAllocatedTime() + " min");
        usedValue.setText(a.getUsedTime() + " min");
        reservedValue.setText(a.getReservedTime() + " min");
        availableValue.setText(a.getAvailableTime() + " min");

        Scheduler s = system.getScheduler();
        HashMap<Integer, LocalDateTime> predicted = s.predictStartTimes();
        String search = searchField.getText().trim().toLowerCase();

        jobModel.setRowCount(0);
        for (Job job : s.getJobsOf(student.getUniversityId())) {
            String text = ("J" + job.getJobId() + " " + job.getTitle() + " " + job.getStatus()).toLowerCase();
            if (!text.contains(search)) {
                continue;
            }
            String start = time(job.getStartTime());
            String finish = time(job.getFinishTime());
            String waiting = job.getQueueWait();
            String result;
            if (job.getStatus().equals("Queued")) {
                LocalDateTime p = predicted.get(job.getJobId());
                start = time(p);
                finish = time(p.plusMinutes(job.getEstimatedDuration()));
                waiting = s.getWaitMinutes(p) + " min left";
                result = "Waiting (position " + s.getQueuePosition(job) + ")";
            } else if (job.getStatus().equals("Running")) {
                finish = time(s.getCurrentTime().plusMinutes(job.getRemainingRuntime()));
                result = job.getRuntime() + " of " + job.getEstimatedDuration() + " min done";
            } else if (job.getStatus().equals("Completed")) {
                result = "Charged " + job.getChargedTime() + " min";
            } else {
                result = "Cancelled, charged " + job.getChargedTime() + " min";
            }
            jobModel.addRow(new Object[]{"J" + job.getJobId(), job.getTitle(), job.getStatus(), job.getGpuName(),
                    start, finish, waiting, result});
        }
    }
}
