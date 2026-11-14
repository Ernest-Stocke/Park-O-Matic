package com.parkomatic.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.parkomatic.model.Bill;
import com.parkomatic.model.Booking;
import com.parkomatic.service.ParkingService;

/** Bills list: mark paid (demo), view receipt, export to CSV. */
class BillsPanel extends JPanel {
    private final ParkingService service;
    private final Runnable onChange;
    private final DefaultTableModel model = Ui.readOnlyModel(
            "Bill", "Booking", "Vehicle", "Minutes", "Hours", "Rate (Rs.)", "Total (Rs.)", "Status");
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField search = new JTextField(14);
    private final JCheckBox unpaidOnly = new JCheckBox("Unpaid only");
    private final Map<String, Booking> bookingByBill = new HashMap<>();

    BillsPanel(ParkingService service, Runnable onChange) {
        this.service = service;
        this.onChange = onChange;
        setLayout(new BorderLayout());
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(search);
        top.add(unpaidOnly);
        JButton pay = new JButton("Mark selected as paid");
        JButton receipt = new JButton("View receipt");
        JButton export = new JButton("Export CSV...");
        top.add(pay);
        top.add(receipt);
        top.add(export);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(new JLabel("  Marking a bill as paid only changes its status. No real payment is processed."), BorderLayout.SOUTH);

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        unpaidOnly.addActionListener(e -> applyFilter());

        pay.addActionListener(e -> {
            String id = selectedBillId();
            if (id == null) {
                return;
            }
            if (Ui.attempt(this, () -> service.markBillPaid(id))) {
                onChange.run();
            }
        });
        receipt.addActionListener(e -> {
            String id = selectedBillId();
            if (id != null && bookingByBill.containsKey(id)) {
                Ui.showText(this, "Receipt " + id, Ui.receipt(bookingByBill.get(id)));
            }
        });
        export.addActionListener(e -> exportCsv());
        refresh();
    }

    private String selectedBillId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a bill first.");
            return null;
        }
        return (String) model.getValueAt(table.convertRowIndexToModel(viewRow), 0);
    }

    private void applyFilter() {
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        String text = search.getText().trim();
        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
        if (unpaidOnly.isSelected()) {
            filters.add(RowFilter.regexFilter("^UNPAID$", 7));
        }
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    void refresh() {
        model.setRowCount(0);
        bookingByBill.clear();
        List<Booking> all = new ArrayList<>(service.getAllBookings());
        Collections.reverse(all);
        for (Booking b : all) {
            Bill bill = b.getBill();
            if (bill == null) {
                continue;
            }
            bookingByBill.put(bill.getBillId(), b);
            model.addRow(new Object[] {
                bill.getBillId(), b.getBookingId(), b.getVehicle().getRegistrationNumber(),
                bill.getDurationMinutes(), bill.getBillableHours(), bill.getHourlyRate(),
                bill.getTotalAmount(), bill.isPaid() ? "PAID" : "UNPAID"
            });
        }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("parkomatic-bills.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        StringBuilder sb = new StringBuilder("Bill,Booking,Vehicle,Minutes,Hours,Rate,Total,Status\n");
        for (int r = 0; r < model.getRowCount(); r++) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) {
                    sb.append(',');
                }
                sb.append('"').append(String.valueOf(model.getValueAt(r, c)).replace("\"", "\"\"")).append('"');
            }
            sb.append('\n');
        }
        try {
            Files.write(chooser.getSelectedFile().toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(this, "Saved to " + chooser.getSelectedFile());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
