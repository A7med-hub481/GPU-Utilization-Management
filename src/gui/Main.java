package gui;

import model.GPUSystem;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.UIManager;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Starts the program: loads the saved files, or on the first run sets the clock, then opens the login window.
 */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // keep the default look
        }

        GPUSystem system = new GPUSystem();
        boolean loaded = false;
        try {
            loaded = system.loadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage() + "\nThe program will start with new data.");
            system = new GPUSystem();
        }

        if (!loaded) {
            setUpClock(system);
        }
        new LoginFrame(system).setVisible(true);
    }

    // first run: the date and time pickers set the simulation clock (or the sample data is loaded)
    private static void setUpClock(GPUSystem system) {
        JSpinner datePicker = new JSpinner(new SpinnerDateModel());
        datePicker.setEditor(new JSpinner.DateEditor(datePicker, "dd/MM/yyyy"));
        JSpinner timePicker = new JSpinner(new SpinnerDateModel());
        timePicker.setEditor(new JSpinner.DateEditor(timePicker, "HH:mm"));
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.add(new JLabel("Start date:"));
        panel.add(datePicker);
        panel.add(new JLabel("Start time:"));
        panel.add(timePicker);

        String[] options = {"Start Simulation", "Load Sample Data"};
        int choice = JOptionPane.showOptionDialog(null, panel, "Set the simulation clock",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            LocalDateTime date = toLocalDateTime((Date) datePicker.getValue());
            LocalDateTime time = toLocalDateTime((Date) timePicker.getValue());
            system.getScheduler().setStartTime(date.withHour(time.getHour()).withMinute(time.getMinute())
                    .withSecond(0).withNano(0));
        } else if (choice == 1) {
            system.loadSampleData();
            JOptionPane.showMessageDialog(null, "Sample data loaded.\nAdmin: admin / gpu2026\n"
                    + "Students: 100071234 ... 100075678, password: pass1234");
        } else {
            System.exit(0);
        }
        try {
            system.saveData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Could not save the data files.");
        }
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}

