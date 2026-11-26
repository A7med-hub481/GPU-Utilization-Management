package gui;

import model.GPUSystem;
import model.Scheduler;
import model.User;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Parent class of AdminFrame and StudentFrame.
 * It builds the header (title + simulated time box) and has helper methods
 * for cards, sections and tables. Each subclass writes its own refresh() method.
 */
public abstract class DashboardFrame extends JFrame {

    // colours of the dashboard
    public static final Color BACKGROUND = new Color(244, 247, 251);
    public static final Color NAVY = new Color(16, 42, 84);
    public static final Color TEAL = new Color(21, 151, 168);
    public static final Color BLUE = new Color(30, 111, 217);
    public static final Color ORANGE = new Color(240, 150, 0);
    public static final Color GREEN = new Color(46, 139, 87);
    public static final Color RED = new Color(210, 60, 60);
    public static final Color GRAY = new Color(100, 110, 125);
    public static final Color LINE = new Color(218, 225, 233);

    protected GPUSystem system;
    protected User user;
    protected JPanel content = new JPanel();            // subclasses add their sections here
    protected JPanel timeBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
    private JLabel timeLabel = new JLabel();
    private JButton logoutButton = createButton("Logout", GRAY);

    public DashboardFrame(GPUSystem system, User user, String title, String subtitle) {
        this.system = system;
        this.user = user;
        setTitle("GPU Utilization Management Application");
        setSize(1350, 900);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout());

        // left part of the header: title and subtitle
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        titleLabel.setForeground(NAVY);
        JLabel subtitleLabel = createLabel(subtitle, 16, true, GRAY);
        JLabel infoLabel = createLabel("4 NVIDIA H100 GPUs  |  Available 24/7", 13, false, GRAY);
        JPanel titles = new JPanel(new GridLayout(3, 1));
        titles.setOpaque(false);
        titles.add(titleLabel);
        titles.add(subtitleLabel);
        titles.add(infoLabel);

        // right part of the header: the simulated time box
        timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        timeLabel.setForeground(NAVY);
        timeBox.setBackground(Color.WHITE);
        timeBox.setBorder(BorderFactory.createLineBorder(LINE));
        timeBox.add(createLabel("Simulated time:", 14, false, Color.BLACK));
        timeBox.add(timeLabel);
        timeBox.add(logoutButton);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setOpaque(false);
        right.add(timeBox);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 24, 6, 24));
        header.add(titles, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(6, 24, 16, 24));

        logoutButton.addActionListener(new LogoutHandler());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                saveData();
                System.exit(0);
            }
        });
    }

    // each window updates its own tables and labels
    public abstract void refresh();

    // updates the time in the header, then the rest of the window
    public void refreshAll() {
        timeLabel.setText(Scheduler.format(system.getScheduler().getCurrentTime()));
        refresh();
    }

    public void saveData() {
        try {
            system.saveData();
        } catch (Exception ex) {
            showError("Could not save the data files: " + ex.getMessage());
        }
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private class LogoutHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            saveData();
            dispose();
            new LoginFrame(system).setVisible(true);
        }
    }

    // ================= helpers used by both windows =================

    public static JLabel createLabel(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    public static JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return button;
    }

    // a white box with a bold title, used for every part of the dashboard
    public JPanel createSection(String title, Component inside) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(10, 14, 12, 14)));
        section.add(createLabel(title, 19, true, NAVY), BorderLayout.NORTH);
        section.add(inside, BorderLayout.CENTER);
        return section;
    }

    // one number in the summary strip, for example "Running 4"
    public JPanel createStat(String title, JLabel value, Color color) {
        value.setFont(new Font("Segoe UI", Font.BOLD, 32));
        value.setForeground(color);
        JPanel stat = new JPanel(new GridLayout(2, 1));
        stat.setOpaque(false);
        stat.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        stat.add(createLabel(title, 14, true, Color.BLACK));
        stat.add(value);
        return stat;
    }

    // a table model whose cells cannot be edited
    public DefaultTableModel createModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    // a table in a scroll pane, showing the given number of rows; status words are coloured
    public JScrollPane createTable(JTable table, int visibleRows) {
        table.setRowHeight(24);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(232, 238, 245));
        table.setGridColor(LINE);
        table.setPreferredScrollableViewportSize(new Dimension(400, visibleRows * 24));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                setHorizontalAlignment(CENTER);
                setForeground(statusColor(String.valueOf(value)));
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        return scroll;
    }

    private Color statusColor(String text) {
        if (text.equals("Queued") || text.equals("Pending")) return ORANGE;
        if (text.equals("Running")) return BLUE;
        if (text.equals("Completed") || text.equals("Approved")) return GREEN;
        if (text.equals("Cancelled") || text.equals("Rejected")) return RED;
        return Color.BLACK;
    }

    // gives wide columns (like dates) more space than short ones
    public void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // returns the number in the first column of the selected row, or -1
    public int getSelectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) {
            showError("Please select a row first.");
            return -1;
        }
        return Integer.parseInt(table.getValueAt(row, 0).toString().replace("J", ""));
    }

    public String time(java.time.LocalDateTime t) {
        return Scheduler.format(t);
    }
}
