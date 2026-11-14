package com.parkomatic.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import com.parkomatic.model.Booking;
import com.parkomatic.model.ParkingSlot;
import com.parkomatic.model.VehicleType;
import com.parkomatic.service.ParkingService;

/** Colour-coded overview of every slot: green = free, red = occupied (shows the plate). */
class SlotsPanel extends JPanel {
    private static final Color FREE_BG = new Color(0xC8E6C9);
    private static final Color BUSY_BG = new Color(0xFFCDD2);
    private static final Color TEXT = new Color(0x202020);

    private final ParkingService service;
    private final JPanel content = new JPanel();

    SlotsPanel(ParkingService service) {
        this.service = service;
        setLayout(new BorderLayout());
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JScrollPane(content), BorderLayout.CENTER);
        refresh();
    }

    void refresh() {
        content.removeAll();
        Map<String, String> plateBySlot = new HashMap<>();
        for (Booking b : service.getActiveBookings()) {
            plateBySlot.put(b.getSlot().getSlotId(), b.getVehicle().getRegistrationNumber());
        }
        for (VehicleType type : VehicleType.values()) {
            List<ParkingSlot> slots = service.getSlots(type);
            int busy = 0;
            for (ParkingSlot s : slots) {
                if (s.isOccupied()) {
                    busy++;
                }
            }
            JLabel title = new JLabel(type.getLabel() + " parking  -  " + busy + " of " + slots.size() + " occupied");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
            title.setAlignmentX(LEFT_ALIGNMENT);
            title.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
            content.add(title);

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            row.setAlignmentX(LEFT_ALIGNMENT);
            if (slots.isEmpty()) {
                row.add(new JLabel("No slots configured (an admin can add some in the Admin tab)."));
            }
            for (ParkingSlot s : slots) {
                String plate = plateBySlot.get(s.getSlotId());
                String text = "<html><center><b>" + s.getSlotId() + "</b><br>"
                        + (s.isOccupied() ? (plate == null ? "Occupied" : plate) : "Free") + "</center></html>";
                JLabel card = new JLabel(text, SwingConstants.CENTER);
                card.setOpaque(true);
                card.setForeground(TEXT);
                card.setBackground(s.isOccupied() ? BUSY_BG : FREE_BG);
                card.setBorder(BorderFactory.createLineBorder(new Color(0x606060)));
                card.setPreferredSize(new Dimension(120, 54));
                row.add(card);
            }
            content.add(row);
        }
        content.revalidate();
        content.repaint();
    }
}
