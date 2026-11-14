package com.parkomatic;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.parkomatic.repository.Database;
import com.parkomatic.repository.Repository;
import com.parkomatic.service.AuthService;
import com.parkomatic.service.BillingService;
import com.parkomatic.service.ParkingService;
import com.parkomatic.ui.LoginFrame;
import com.parkomatic.ui.MainFrame;

/** Application entry point: opens the database, wires the services together and shows the login window. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                // fall back to the default look and feel
            }
            try {
                Repository repo = Database.open();
                BillingService billing = new BillingService(repo);
                ParkingService parking = new ParkingService(repo, billing);
                AuthService auth = new AuthService(repo);
                showLogin(parking, auth);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(null,
                        e.getMessage() + "\n\nIs the JDBC driver jar (e.g. sqlite-jdbc) on the classpath?\n"
                                + "See README.md for the exact run command.",
                        "Park-O-Matic could not start", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    static void showLogin(ParkingService parking, AuthService auth) {
        new LoginFrame(auth, user -> {
            MainFrame frame = new MainFrame(parking, auth, user, () -> showLogin(parking, auth));
            frame.setVisible(true);
        }).setVisible(true);
    }
}
