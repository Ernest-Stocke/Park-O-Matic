package com.parkomatic.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;

import com.parkomatic.model.Role;
import com.parkomatic.model.User;
import com.parkomatic.service.AuthService;
import com.parkomatic.service.ParkingService;

/** Main window. Admins get an extra "Admin" tab; employees do not see it. */
public class MainFrame extends JFrame {
    private final ParkingService service;
    private final AuthService auth;
    private final User user;

    private final SlotsPanel slotsPanel;
    private final OperationsPanel operationsPanel;
    private final BookingsPanel bookingsPanel;
    private final BillsPanel billsPanel;
    private AdminPanel adminPanel; // null for employees

    public MainFrame(ParkingService service, AuthService auth, User user, Runnable onLogout) {
        super("Park-O-Matic - Parking Management");
        this.service = service;
        this.auth = auth;
        this.user = user;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        slotsPanel = new SlotsPanel(service);
        operationsPanel = new OperationsPanel(service, user.getRole(), this::refreshAll);
        bookingsPanel = new BookingsPanel(service);
        billsPanel = new BillsPanel(service, this::refreshAll);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Slots", slotsPanel);
        tabs.addTab("Operations", operationsPanel);
        tabs.addTab("Bookings", bookingsPanel);
        tabs.addTab("Bills", billsPanel);
        if (user.getRole() == Role.ADMIN) {
            adminPanel = new AdminPanel(service, auth, this::refreshAll);
            tabs.addTab("Admin", adminPanel);
        }

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        header.add(new JLabel("<html><b>Logged in as " + user.getUsername() + "</b> (" + user.getRole() + ")</html>"),
                BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton refresh = new JButton("Refresh");
        JButton password = new JButton("Change my password");
        JButton logout = new JButton("Log out");
        refresh.addActionListener(e -> refreshAll());
        password.addActionListener(e -> changePassword());
        logout.addActionListener(e -> {
            dispose();
            onLogout.run();
        });
        buttons.add(refresh);
        buttons.add(password);
        buttons.add(logout);
        header.add(buttons, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        setPreferredSize(new Dimension(1000, 640));
        pack();
        setLocationRelativeTo(null);
    }

    /** Re-reads everything from the database and redraws every tab. */
    void refreshAll() {
        slotsPanel.refresh();
        operationsPanel.refresh();
        bookingsPanel.refresh();
        billsPanel.refresh();
        if (adminPanel != null) {
            adminPanel.refresh();
        }
    }

    private void changePassword() {
        JPasswordField field = new JPasswordField(14);
        int answer = JOptionPane.showConfirmDialog(this, field, "New password", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            Ui.attempt(this, () -> {
                auth.changePassword(user.getUsername(), new String(field.getPassword()));
                JOptionPane.showMessageDialog(this, "Password changed.");
            });
        }
    }
}
