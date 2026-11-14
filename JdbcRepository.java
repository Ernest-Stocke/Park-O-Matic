import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL storage through plain JDBC. The SQL is deliberately portable, so the same code runs on
 * SQLite (default, no server needed) and MySQL/MariaDB. Money is stored as whole rupees (INT),
 * timestamps as ISO-8601 text, booleans as 0/1.
 *
 * Multi-step operations (start booking, complete booking, resize slots) run inside one
 * transaction, so a failure part-way leaves the database unchanged.
 */
public class JdbcRepository implements Repository, AutoCloseable {
    private final Connection conn;

    public JdbcRepository(String url, String user, String password) {
        try {
            if (user == null || user.isEmpty()) {
                conn = DriverManager.getConnection(url);
            } else {
                conn = DriverManager.getConnection(url, user, password == null ? "" : password);
            }
            createSchema();
        } catch (SQLException e) {
            throw new DataAccessException("Could not open the database (" + url + "): " + e.getMessage(), e);
        }
    }

    private interface Work<T> {
        T run() throws SQLException;
    }

    /** Runs the work in a transaction: commit on success, roll back on any failure. */
    private synchronized <T> T tx(Work<T> work) {
        try {
            boolean previous = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                T result = work.run();
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previous);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }

    private void createSchema() throws SQLException {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS app_users ("
                + "username VARCHAR(50) PRIMARY KEY, password_hash VARCHAR(200) NOT NULL, role VARCHAR(20) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS vehicles ("
                + "reg_no VARCHAR(20) PRIMARY KEY, owner_name VARCHAR(100) NOT NULL, vehicle_type VARCHAR(20) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS parking_slots ("
                + "slot_id VARCHAR(10) PRIMARY KEY, slot_type VARCHAR(20) NOT NULL, occupied INT NOT NULL DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS bookings ("
                + "booking_id VARCHAR(20) PRIMARY KEY, reg_no VARCHAR(20) NOT NULL, slot_id VARCHAR(10) NOT NULL, "
                + "entry_time VARCHAR(30) NOT NULL, exit_time VARCHAR(30), "
                + "FOREIGN KEY (reg_no) REFERENCES vehicles(reg_no))",
            "CREATE TABLE IF NOT EXISTS bills ("
                + "bill_id VARCHAR(20) PRIMARY KEY, booking_id VARCHAR(20) NOT NULL, duration_minutes INT NOT NULL, "
                + "billable_hours INT NOT NULL, hourly_rate INT NOT NULL, total_amount INT NOT NULL, "
                + "paid INT NOT NULL DEFAULT 0, FOREIGN KEY (booking_id) REFERENCES bookings(booking_id))",
            "CREATE TABLE IF NOT EXISTS rates (vehicle_type VARCHAR(20) PRIMARY KEY, hourly_rate INT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS counters (name VARCHAR(30) PRIMARY KEY, value INT NOT NULL)"
        };
        try (Statement st = conn.createStatement()) {
            for (String sql : ddl) {
                st.execute(sql);
            }
        }
    }

    // ------------------------------------------------------------------ users

    @Override
    public User findUser(String username) {
        return tx(() -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT username, password_hash, role FROM app_users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? readUser(rs) : null;
                }
            }
        });
    }

    @Override
    public List<User> listUsers() {
        return tx(() -> {
            List<User> out = new ArrayList<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT username, password_hash, role FROM app_users ORDER BY username")) {
                while (rs.next()) {
                    out.add(readUser(rs));
                }
            }
            return out;
        });
    }

    private User readUser(ResultSet rs) throws SQLException {
        return new User(rs.getString(1), rs.getString(2), Role.valueOf(rs.getString(3)));
    }

    @Override
    public void addUser(User user) {
        tx(() -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO app_users (username, password_hash, role) VALUES (?, ?, ?)")) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getRole().name());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void updateUserPassword(String username, String passwordHash) {
        tx(() -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE app_users SET password_hash = ? WHERE username = ?")) {
                ps.setString(1, passwordHash);
                ps.setString(2, username);
                ps.executeUpdate();
            }
            return null;
        });
    }

    // --------------------------------------------------------------- vehicles

    private Map<String, Vehicle> loadVehicles() throws SQLException {
        Map<String, Vehicle> out = new LinkedHashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT reg_no, owner_name, vehicle_type FROM vehicles ORDER BY reg_no")) {
            while (rs.next()) {
                out.put(rs.getString(1), new Vehicle(rs.getString(1), rs.getString(2), VehicleType.valueOf(rs.getString(3))));
            }
        }
        return out;
    }

    @Override
    public Vehicle findVehicle(String registrationNumber) {
        return tx(() -> loadVehicles().get(registrationNumber));
    }

    @Override
    public List<Vehicle> listVehicles() {
        return tx(() -> new ArrayList<>(loadVehicles().values()));
    }

    @Override
    public void addVehicle(Vehicle vehicle) {
        tx(() -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO vehicles (reg_no, owner_name, vehicle_type) VALUES (?, ?, ?)")) {
                ps.setString(1, vehicle.getRegistrationNumber());
                ps.setString(2, vehicle.getOwnerName());
                ps.setString(3, vehicle.getType().name());
                ps.executeUpdate();
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ slots

    private Map<String, ParkingSlot> loadSlots() throws SQLException {
        Map<String, ParkingSlot> out = new LinkedHashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT slot_id, slot_type, occupied FROM parking_slots ORDER BY slot_id")) {
            while (rs.next()) {
                ParkingSlot slot = new ParkingSlot(rs.getString(1), VehicleType.valueOf(rs.getString(2)));
                slot.setOccupied(rs.getInt(3) != 0);
                out.put(slot.getSlotId(), slot);
            }
        }
        return out;
    }

    @Override
    public List<ParkingSlot> listSlots() {
        return tx(() -> new ArrayList<>(loadSlots().values()));
    }

    @Override
    public void applySlotChanges(List<ParkingSlot> toAdd, List<String> idsToRemove) {
        tx(() -> {
            try (PreparedStatement add = conn.prepareStatement(
                         "INSERT INTO parking_slots (slot_id, slot_type, occupied) VALUES (?, ?, 0)");
                 PreparedStatement del = conn.prepareStatement(
                         "DELETE FROM parking_slots WHERE slot_id = ? AND occupied = 0")) {
                for (String id : idsToRemove) {
                    del.setString(1, id);
                    if (del.executeUpdate() != 1) {
                        throw new IllegalStateException("Slot " + id + " could not be removed (it is occupied or missing).");
                    }
                }
                for (ParkingSlot slot : toAdd) {
                    add.setString(1, slot.getSlotId());
                    add.setString(2, slot.getSupportedType().name());
                    add.executeUpdate();
                }
            }
            return null;
        });
    }

    // ------------------------------------------------- bookings and bills

    private Map<String, Bill> loadBillsByBooking() throws SQLException {
        Map<String, Bill> out = new LinkedHashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT bill_id, booking_id, duration_minutes, billable_hours, "
                     + "hourly_rate, total_amount, paid FROM bills")) {
            while (rs.next()) {
                Bill bill = new Bill(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4),
                        BigDecimal.valueOf(rs.getLong(5)), BigDecimal.valueOf(rs.getLong(6)));
                if (rs.getInt(7) != 0) {
                    bill.markPaid();
                }
                out.put(bill.getBookingId(), bill);
            }
        }
        return out;
    }

    @Override
    public List<Booking> listBookings() {
        return tx(() -> {
            Map<String, Vehicle> vehicles = loadVehicles();
            Map<String, ParkingSlot> slots = loadSlots();
            Map<String, Bill> bills = loadBillsByBooking();
            List<Booking> out = new ArrayList<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT booking_id, reg_no, slot_id, entry_time, exit_time "
                         + "FROM bookings ORDER BY booking_id")) {
                while (rs.next()) {
                    String bookingId = rs.getString(1);
                    Vehicle vehicle = vehicles.get(rs.getString(2));
                    ParkingSlot slot = slots.get(rs.getString(3));
                    if (slot == null) {
                        // The slot was removed by an admin after this booking finished; keep the history readable.
                        slot = new ParkingSlot(rs.getString(3), vehicle.getType());
                    }
                    Booking booking = new Booking(bookingId, vehicle, slot, LocalDateTime.parse(rs.getString(4)));
                    String exit = rs.getString(5);
                    if (exit != null) {
                        booking.complete(LocalDateTime.parse(exit), bills.get(bookingId));
                    }
                    out.add(booking);
                }
            }
            return out;
        });
    }

    @Override
    public void startBooking(Booking booking) {
        tx(() -> {
            try (PreparedStatement ins = conn.prepareStatement(
                         "INSERT INTO bookings (booking_id, reg_no, slot_id, entry_time, exit_time) VALUES (?, ?, ?, ?, NULL)");
                 PreparedStatement occ = conn.prepareStatement(
                         "UPDATE parking_slots SET occupied = 1 WHERE slot_id = ? AND occupied = 0")) {
                ins.setString(1, booking.getBookingId());
                ins.setString(2, booking.getVehicle().getRegistrationNumber());
                ins.setString(3, booking.getSlot().getSlotId());
                ins.setString(4, booking.getEntryTime().toString());
                ins.executeUpdate();
                occ.setString(1, booking.getSlot().getSlotId());
                if (occ.executeUpdate() != 1) {
                    throw new IllegalStateException("Slot " + booking.getSlot().getSlotId() + " is no longer free.");
                }
            }
            return null;
        });
    }

    @Override
    public void completeBooking(Booking booking) {
        Bill bill = booking.getBill();
        if (booking.isActive() || bill == null) {
            throw new IllegalStateException("Booking must be completed before it is saved.");
        }
        tx(() -> {
            try (PreparedStatement upd = conn.prepareStatement(
                         "UPDATE bookings SET exit_time = ? WHERE booking_id = ?");
                 PreparedStatement ins = conn.prepareStatement(
                         "INSERT INTO bills (bill_id, booking_id, duration_minutes, billable_hours, hourly_rate, "
                                 + "total_amount, paid) VALUES (?, ?, ?, ?, ?, ?, 0)");
                 PreparedStatement free = conn.prepareStatement(
                         "UPDATE parking_slots SET occupied = 0 WHERE slot_id = ?")) {
                upd.setString(1, booking.getExitTime().toString());
                upd.setString(2, booking.getBookingId());
                upd.executeUpdate();
                ins.setString(1, bill.getBillId());
                ins.setString(2, bill.getBookingId());
                ins.setLong(3, bill.getDurationMinutes());
                ins.setLong(4, bill.getBillableHours());
                ins.setLong(5, bill.getHourlyRate().longValueExact());
                ins.setLong(6, bill.getTotalAmount().longValueExact());
                ins.executeUpdate();
                free.setString(1, booking.getSlot().getSlotId());
                free.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void markBillPaid(String billId) {
        tx(() -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE bills SET paid = 1 WHERE bill_id = ?")) {
                ps.setString(1, billId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    // --------------------------------------------------------------- settings

    @Override
    public Integer getRate(VehicleType type) {
        return tx(() -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT hourly_rate FROM rates WHERE vehicle_type = ?")) {
                ps.setString(1, type.name());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Integer.valueOf(rs.getInt(1)) : null;
                }
            }
        });
    }

    @Override
    public void setRate(VehicleType type, int rate) {
        tx(() -> {
            try (PreparedStatement upd = conn.prepareStatement("UPDATE rates SET hourly_rate = ? WHERE vehicle_type = ?");
                 PreparedStatement ins = conn.prepareStatement("INSERT INTO rates (vehicle_type, hourly_rate) VALUES (?, ?)")) {
                upd.setInt(1, rate);
                upd.setString(2, type.name());
                if (upd.executeUpdate() == 0) {
                    ins.setString(1, type.name());
                    ins.setInt(2, rate);
                    ins.executeUpdate();
                }
            }
            return null;
        });
    }

    @Override
    public int nextCounter(String name) {
        return tx(() -> {
            try (PreparedStatement sel = conn.prepareStatement("SELECT value FROM counters WHERE name = ?");
                 PreparedStatement upd = conn.prepareStatement("UPDATE counters SET value = ? WHERE name = ?");
                 PreparedStatement ins = conn.prepareStatement("INSERT INTO counters (name, value) VALUES (?, ?)")) {
                sel.setString(1, name);
                int next;
                boolean exists;
                try (ResultSet rs = sel.executeQuery()) {
                    exists = rs.next();
                    next = exists ? rs.getInt(1) + 1 : 1;
                }
                if (exists) {
                    upd.setInt(1, next);
                    upd.setString(2, name);
                    upd.executeUpdate();
                } else {
                    ins.setString(1, name);
                    ins.setInt(2, next);
                    ins.executeUpdate();
                }
                return next;
            }
        });
    }

    @Override
    public synchronized void close() {
        try {
            conn.close();
        } catch (SQLException ignored) {
            // nothing useful to do on shutdown
        }
    }
}
