import java.awt.Component;
import java.awt.Font;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

/** Small helpers shared by the Swing panels. */
final class Ui {
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    /** A GUI action that may fail with any exception; failures are shown in a dialog. */
    interface Action {
        void run() throws Exception;
    }

    private Ui() { }

    static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /** Runs the action; returns true on success, otherwise shows the error and returns false. */
    static boolean attempt(Component parent, Action action) {
        try {
            action.run();
            return true;
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.toString() : e.getMessage();
            JOptionPane.showMessageDialog(parent, message, "Cannot complete that", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    static void showText(Component parent, String title, String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JOptionPane.showMessageDialog(parent, new JScrollPane(area), title, JOptionPane.PLAIN_MESSAGE);
    }

    static String time(LocalDateTime t) {
        return t == null ? "-" : t.format(TIME);
    }

    static String receipt(Booking b) {
        Bill bill = b.getBill();
        long minutes = Duration.between(b.getEntryTime(), b.getExitTime()).toMinutes();
        StringBuilder sb = new StringBuilder();
        sb.append("========== RECEIPT ==========\n");
        sb.append(String.format("Bill ID      : %s%n", bill.getBillId()));
        sb.append(String.format("Booking ID   : %s%n", b.getBookingId()));
        sb.append(String.format("Vehicle      : %s%n", b.getVehicle()));
        sb.append(String.format("Slot         : %s%n", b.getSlot().getSlotId()));
        sb.append(String.format("Entry        : %s%n", time(b.getEntryTime())));
        sb.append(String.format("Exit         : %s%n", time(b.getExitTime())));
        sb.append(String.format("Duration     : %d min%n", minutes));
        sb.append(String.format("Billable hrs : %d%n", bill.getBillableHours()));
        sb.append(String.format("Rate         : Rs.%s / hour%n", bill.getHourlyRate()));
        sb.append(String.format("TOTAL        : Rs.%s%n", bill.getTotalAmount()));
        sb.append(String.format("Status       : %s%n", bill.isPaid() ? "PAID" : "UNPAID"));
        sb.append("=============================\n");
        return sb.toString();
    }
}
