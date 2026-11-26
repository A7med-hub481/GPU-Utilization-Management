package gui;

import model.AccessRequest;
import model.GPUSystem;
import model.Scheduler;
import model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The first window: login for the administrator and students, and student registration.
 */
public class LoginFrame extends JFrame {

    private GPUSystem system;
    private JTextField usernameField = new JTextField(15);
    private JPasswordField passwordField = new JPasswordField(15);
    private JButton loginButton = DashboardFrame.createButton("Login", DashboardFrame.TEAL);
    private JButton registerButton = DashboardFrame.createButton("Register", DashboardFrame.NAVY);

    public LoginFrame(GPUSystem system) {
        this.system = system;
        setTitle("GPU Utilization Management Application");
        setSize(520, 380);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(DashboardFrame.BACKGROUND);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("GPU Utilization Management", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(DashboardFrame.NAVY);
        JLabel info = DashboardFrame.createLabel("4 NVIDIA H100 GPUs  |  Simulated time: "
                + Scheduler.format(system.getScheduler().getCurrentTime()), 13, false, DashboardFrame.GRAY);
        info.setHorizontalAlignment(JLabel.CENTER);
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(25, 10, 10, 10));
        header.add(title);
        header.add(info);

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 14));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DashboardFrame.LINE),
                BorderFactory.createEmptyBorder(22, 30, 22, 30)));
        form.add(DashboardFrame.createLabel("Username:", 15, false, Color.BLACK));
        form.add(usernameField);
        form.add(DashboardFrame.createLabel("Password:", 15, false, Color.BLACK));
        form.add(passwordField);
        JPanel formHolder = new JPanel(new FlowLayout());
        formHolder.setOpaque(false);
        formHolder.add(form);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
        buttons.setOpaque(false);
        buttons.add(loginButton);
        buttons.add(registerButton);
        JLabel hint = DashboardFrame.createLabel("Students log in with their university ID.", 12, false,
                DashboardFrame.GRAY);
        hint.setHorizontalAlignment(JLabel.CENTER);
        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        bottom.add(buttons);
        bottom.add(hint);

        add(header, BorderLayout.NORTH);
        add(formHolder, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        EventHandler handler = new EventHandler();
        loginButton.addActionListener(handler);
        registerButton.addActionListener(handler);
        getRootPane().setDefaultButton(loginButton);     // Enter = Login
    }

    private class EventHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                if (e.getSource() == loginButton) {
                    login();
                } else if (e.getSource() == registerButton) {
                    register();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(LoginFrame.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void login() {
        User user = system.login(usernameField.getText().trim(), new String(passwordField.getPassword()));
        dispose();
        if (user.getRole().equals("Administrator")) {
            new AdminFrame(system, user).setVisible(true);
        } else {
            new StudentFrame(system, user).setVisible(true);
        }
    }

    // shows the registration form in a dialog
    private void register() throws Exception {
        JTextField id = new JTextField(15);
        JTextField name = new JTextField(15);
        JTextField email = new JTextField(15);
        JTextField phone = new JTextField(15);
        JPasswordField password = new JPasswordField(15);
        JPanel panel = new JPanel(new GridLayout(5, 2, 8, 8));
        panel.add(new JLabel("University ID:"));
        panel.add(id);
        panel.add(new JLabel("Name:"));
        panel.add(name);
        panel.add(new JLabel("Email:"));
        panel.add(email);
        panel.add(new JLabel("Phone number:"));
        panel.add(phone);
        panel.add(new JLabel("Password (6+ characters):"));
        panel.add(password);

        int answer = JOptionPane.showConfirmDialog(this, panel, "Student Registration", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            AccessRequest r = system.register(id.getText(), name.getText(), email.getText(), phone.getText(),
                    new String(password.getPassword()));
            system.saveData();
            JOptionPane.showMessageDialog(this, "Request " + r.getRequestId() + " was sent.\n"
                    + "You can log in after the administrator approves it.");
        }
    }
}
