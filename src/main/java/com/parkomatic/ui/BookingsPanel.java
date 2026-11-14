package com.parkomatic.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.parkomatic.model.Booking;
import com.parkomatic.service.ParkingService;

/** Parking history with a search box and an "active only" filter. */
class BookingsPanel extends JPanel {
    private final ParkingService service;
    private final DefaultTableModel model = Ui.readOnlyModel(
            "Booking", "Vehicle", "Type", "Owner", "Slot", "Entry", "Exit", "Status");
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField search = new JTextField(16);
    private final JCheckBox activeOnly = new JCheckBox("Active only");

    BookingsPanel(ParkingService service) {
        this.service = service;
        setLayout(new BorderLayout());
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(search);
        top.add(activeOnly);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        activeOnly.addActionListener(e -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        String text = search.getText().trim();
        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
        if (activeOnly.isSelected()) {
            filters.add(RowFilter.regexFilter("^ACTIVE$", 7));
        }
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    void refresh() {
        model.setRowCount(0);
        List<Booking> all = new ArrayList<>(service.getAllBookings());
        Collections.reverse(all); // newest first
        for (Booking b : all) {
            model.addRow(new Object[] {
                b.getBookingId(), b.getVehicle().getRegistrationNumber(), b.getVehicle().getType(),
                b.getVehicle().getOwnerName(), b.getSlot().getSlotId(), Ui.time(b.getEntryTime()),
                Ui.time(b.getExitTime()), b.isActive() ? "ACTIVE" : "COMPLETED"
            });
        }
    }
}
