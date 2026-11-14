package com.parkomatic.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import com.parkomatic.model.Role;
import com.parkomatic.model.User;
import com.parkomatic.model.VehicleType;
import com.parkomatic.service.AuthService;
import com.parkomatic.service.BillingService;
import com.parkomatic.service.ParkingService;

/** Admin-only: slot capacity and hourly rate per vehicle type, dashboard totals, user accounts. */
class AdminPanel extends JPanel {
    private final ParkingService service;
    private final AuthService auth;
    private final Runnable onChange;

    private final Map<VehicleType, JSpinner> capacitySpinners = new EnumMap<>(VehicleType.class);
    private final Map<VehicleType, JSpinner> rateSpinners = new EnumMap<>(VehicleType.class);
    private final Map<VehicleType, JLabel> occupiedLabels = new EnumMap<>(VehicleType.class);

    private final JLabel summary = new JLabel(" ");
    private final DefaultTableModel userModel = Ui.readOnlyModel("Username", "Role");
    private final JTable userTable = new JTable(userModel);
    private final JTextField newUser = new JTextField(12);
    private final JPasswordField newPass = new JPasswordField(12);
    private final JComboBox<Role> newRole = new JComboBox<>(Role.values());

    AdminPanel(ParkingService service, AuthService auth, Runnable onChange) {
        this.service = service;
        this.auth = auth;
        this.onChange = onChange;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(buildSummaryPanel());
        add(buildCapacityPanel());
        add(buildUsersPanel());
        refresh();
    }

    private JPanel buildSummaryPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Overview"));
        p.add(summary, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCapacityPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Parking spaces and hourly rates"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;

        String[] headers = {"Vehicle type", "Number of slots", "Currently occupied", "Rate per hour (Rs.)", ""};
        for (int i = 0; i < headers.length; i++) {
            c.gridx = i;
            c.gridy = 0;
            p.add(new JLabel("<html><b>" + headers[i] + "</b></html>"), c);
        }
        int row = 1;
        for (VehicleType type : VehicleType.values()) {
            JSpinner cap = new JSpinner(new SpinnerNumberModel(0, 0, ParkingService.MAX_SLOTS_PER_TYPE, 1));
            JSpinner rate = new JSpinner(new SpinnerNumberModel(0, 0, BillingService.MAX_RATE, 5));
            JLabel occupied = new JLabel("0");
            JButton apply = new JButton("Apply");
            capacitySpinners.put(type, cap);
            rateSpinners.put(type, rate);
            occupiedLabels.put(type, occupied);
            apply.addActionListener(e -> {
                boolean ok = Ui.attempt(this, () -> {
                    service.setCapacity(type, (Integer) cap.getValue(), Role.ADMIN);
                    service.setRate(type, (Integer) rate.getValue(), Role.ADMIN);
                });
                if (ok) {
                    onChange.run();
                }
            });
            c.gridy = row++;
            c.gridx = 0; p.add(new JLabel(type.getLabel()), c);
            c.gridx = 1; p.add(cap, c);
            c.gridx = 2; p.add(occupied, c);
            c.gridx = 3; p.add(rate, c);
            c.gridx = 4; p.add(apply, c);
        }
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 5;
        p.add(new JLabel("<html>Reducing slots removes the highest-numbered ones and is blocked while they are occupied. "
                + "New rates apply to bills created from now on.</html>"), c);
        return p;
    }

    private JPanel buildUsersPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("User accounts"));
        userTable.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setPreferredSize(new Dimension(300, 110));
        p.add(scroll, BorderLayout.CENTER);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Username"));
        form.add(newUser);
        form.add(new JLabel("Password"));
        form.add(newPass);
        form.add(newRole);
        JButton add = new JButton("Add user");
        add.addActionListener(e -> {
            boolean ok = Ui.attempt(this, () -> {
                auth.addUser(Role.ADMIN, newUser.getText(), new String(newPass.getPassword()), (Role) newRole.getSelectedItem());
                newUser.setText("");
                newPass.setText("");
            });
            if (ok) {
                refresh();
            }
        });
        JButton reset = new JButton("Reset password of selected");
        reset.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a user first.");
                return;
            }
            String username = (String) userModel.getValueAt(row, 0);
            JPasswordField field = new JPasswordField(14);
            int answer = JOptionPane.showConfirmDialog(this, field, "New password for " + username,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer == JOptionPane.OK_OPTION) {
                Ui.attempt(this, () -> {
                    auth.resetPassword(Role.ADMIN, username, new String(field.getPassword()));
                    JOptionPane.showMessageDialog(this, "Password updated for " + username + ".");
                });
            }
        });
        form.add(add);
        form.add(reset);
        p.add(form, BorderLayout.SOUTH);
        return p;
    }

    void refresh() {
        for (VehicleType type : VehicleType.values()) {
            capacitySpinners.get(type).setValue(service.getCapacity(type));
            rateSpinners.get(type).setValue(service.getRate(type));
            occupiedLabels.get(type).setText(String.valueOf(service.getOccupiedCount(type)));
        }
        ParkingService.Summary s = service.getSummary();
        summary.setText("<html>Slots: <b>" + s.occupiedSlots + "</b> occupied of <b>" + s.totalSlots + "</b>"
                + " &nbsp;|&nbsp; Registered vehicles: <b>" + s.vehicles + "</b>"
                + " &nbsp;|&nbsp; Bookings: <b>" + s.bookings + "</b> (" + s.activeBookings + " active)<br>"
                + "Billed today: <b>Rs." + s.billedToday + "</b>"
                + " &nbsp;|&nbsp; Collected (paid): <b>Rs." + s.collected + "</b>"
                + " &nbsp;|&nbsp; Outstanding (unpaid): <b>Rs." + s.outstanding + "</b></html>");
        userModel.setRowCount(0);
        for (User u : auth.listUsers()) {
            userModel.addRow(new Object[] {u.getUsername(), u.getRole()});
        }
    }
}
