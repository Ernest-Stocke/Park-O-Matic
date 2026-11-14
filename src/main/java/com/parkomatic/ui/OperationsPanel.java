package com.parkomatic.ui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.parkomatic.model.Booking;
import com.parkomatic.model.Role;
import com.parkomatic.model.Vehicle;
import com.parkomatic.model.VehicleType;
import com.parkomatic.service.ParkingService;
import com.parkomatic.util.PlateValidator;

/** Day-to-day work: register a vehicle, park it, and exit it with a bill. */
class OperationsPanel extends JPanel {
    private static final Color OK = new Color(0, 110, 0);
    private static final Color BAD = new Color(170, 0, 0);

    private final ParkingService service;
    private final Role role;
    private final Runnable onChange;

    private final JTextField regField = new JTextField(14);
    private final JLabel regHint = new JLabel(" ");
    private final JTextField ownerField = new JTextField(18);
    private final JComboBox<VehicleType> typeBox = new JComboBox<>();

    private final JComboBox<String> plateBox = new JComboBox<>();
    private final JSpinner minutesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 15));
    private final JLabel status = new JLabel(" ");

    OperationsPanel(ParkingService service, Role role, Runnable onChange) {
        this.service = service;
        this.role = role;
        this.onChange = onChange;
        setLayout(new GridBagLayout());

        for (VehicleType t : VehicleType.values()) {
            if (role == Role.ADMIN || !t.needsAdminToRegister()) {
                typeBox.addItem(t);
            }
        }
        plateBox.setEditable(true);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 10, 4, 10);
        add(buildRegisterPanel(), c);
        c.gridy = 1;
        add(buildParkPanel(), c);
        c.gridy = 2;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        add(status, c);

        regField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateHint(); }
            public void removeUpdate(DocumentEvent e) { updateHint(); }
            public void changedUpdate(DocumentEvent e) { updateHint(); }
        });
        refresh();
    }

    private JPanel buildRegisterPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("1. Register a vehicle"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; p.add(new JLabel("Registration no."), c);
        c.gridx = 1; p.add(regField, c);
        c.gridx = 2; p.add(regHint, c);
        c.gridx = 0; c.gridy = 1; p.add(new JLabel("Owner name"), c);
        c.gridx = 1; c.gridwidth = 2; p.add(ownerField, c);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 1; p.add(new JLabel("Vehicle type"), c);
        c.gridx = 1; p.add(typeBox, c);

        JButton register = new JButton("Register vehicle");
        register.addActionListener(e -> {
            boolean ok = Ui.attempt(this, () -> {
                Vehicle v = service.registerVehicle(regField.getText(), ownerField.getText(),
                        (VehicleType) typeBox.getSelectedItem(), role);
                regField.setText("");
                ownerField.setText("");
                setStatus("Registered " + v, true);
            });
            if (ok) {
                onChange.run();
            }
        });
        c.gridx = 1; c.gridy = 3; p.add(register, c);
        if (role != Role.ADMIN) {
            c.gridx = 2; p.add(new JLabel("VIP / Emergency registration is admin-only"), c);
        }
        return p;
    }

    private JPanel buildParkPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("2. Park or exit a registered vehicle"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; p.add(new JLabel("Registration no."), c);
        c.gridx = 1; c.gridwidth = 2; p.add(plateBox, c);

        JButton park = new JButton("Park (entry)");
        park.addActionListener(e -> {
            boolean ok = Ui.attempt(this, () -> {
                Booking b = service.parkVehicle(selectedPlate());
                setStatus("Parked " + b.getVehicle().getRegistrationNumber() + " in slot "
                        + b.getSlot().getSlotId() + " (booking " + b.getBookingId() + ")", true);
            });
            if (ok) {
                onChange.run();
            }
        });
        c.gridx = 0; c.gridy = 1; c.gridwidth = 1; p.add(park, c);

        c.gridx = 1; p.add(new JLabel("Simulated minutes parked (0 = real time):"), c);
        c.gridx = 2; p.add(minutesSpinner, c);

        JButton exit = new JButton("Exit and bill");
        exit.addActionListener(e -> {
            final Booking[] result = new Booking[1];
            boolean ok = Ui.attempt(this, () -> {
                int minutes = (Integer) minutesSpinner.getValue();
                String plate = selectedPlate();
                result[0] = minutes == 0 ? service.exitVehicle(plate) : service.exitVehicleAfterMinutes(plate, minutes);
                setStatus("Exited " + result[0].getVehicle().getRegistrationNumber() + ". Total Rs."
                        + result[0].getBill().getTotalAmount(), true);
            });
            if (ok) {
                onChange.run();
                Ui.showText(this, "Receipt", Ui.receipt(result[0]));
            }
        });
        c.gridx = 3; p.add(exit, c);
        return p;
    }

    private String selectedPlate() {
        Object item = plateBox.getEditor().getItem();
        return item == null ? "" : item.toString();
    }

    private void updateHint() {
        String text = regField.getText();
        if (text.trim().isEmpty()) {
            regHint.setText(" ");
            return;
        }
        String error = PlateValidator.validate(text);
        if (error == null) {
            regHint.setForeground(OK);
            regHint.setText("Valid: " + PlateValidator.normalize(text));
        } else {
            regHint.setForeground(BAD);
            regHint.setText("<html>" + error + "</html>");
        }
    }

    private void setStatus(String message, boolean good) {
        status.setForeground(good ? OK : BAD);
        status.setText(message);
    }

    void refresh() {
        String current = selectedPlate();
        plateBox.removeAllItems();
        for (Vehicle v : service.getVehicles()) {
            plateBox.addItem(v.getRegistrationNumber());
        }
        plateBox.setSelectedItem(current);
    }
}
