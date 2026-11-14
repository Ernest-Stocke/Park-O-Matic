package com.parkomatic;

import java.util.List;

import com.parkomatic.model.Booking;
import com.parkomatic.model.Role;
import com.parkomatic.model.User;
import com.parkomatic.model.VehicleType;
import com.parkomatic.repository.InMemoryRepository;
import com.parkomatic.repository.Repository;
import com.parkomatic.service.AuthService;
import com.parkomatic.service.BillingService;
import com.parkomatic.service.ParkingService;
import com.parkomatic.util.PlateValidator;

/** Plain-Java tests (no JUnit needed). Run: java -cp out TestRunner */
public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void expectThrows(String name, Class<? extends Exception> type, Runnable r) {
        try {
            r.run();
            failed++;
            System.out.println("FAIL (no exception): " + name);
        } catch (Exception e) {
            if (type.isInstance(e)) {
                passed++;
            } else {
                failed++;
                System.out.println("FAIL (wrong exception " + e + "): " + name);
            }
        }
    }

    public static void main(String[] args) {
        testPlates();
        testBilling();
        testParkingFlow();
        testCapacityAndRoles();
        testAuth();
        System.out.println(passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void testPlates() {
        String[] good = {"MH12AB1234", "mh 12 ab 1234", "DL-1-C-AB-1234", "DL1CAB1234", "KA01A0001",
                "UP32AB9999", "TN09BZ4321", "22BH1234AA", "21 BH 0001 KL", "AP39ZZ9999"};
        for (String p : good) {
            // KA01A0001 has a 1-letter series, which is valid; keep it in the list
            check("valid: " + p, PlateValidator.isValid(p));
        }
        String[] bad = {"", "   ", "MH12AB123", "MH12AB12345", "XX12AB1234", "MH00AB1234", "MH12AB0000",
                "MH12ABCD1234", "1234MH12AB", "MH12 AB 12#4", "22BH1234IO", "19BH1234AA", "99BH1234AA", "ABCDEFGH", "MH12"};
        for (String p : bad) {
            check("invalid: " + p, !PlateValidator.isValid(p));
        }
        check("normalize", PlateValidator.normalize(" mh-12 ab 1234 ").equals("MH12AB1234"));
        check("error message set", PlateValidator.validate("XX12AB1234").contains("Unknown state"));
    }

    private static ParkingService newService(Repository repo) {
        return new ParkingService(repo, new BillingService(repo));
    }

    private static void testBilling() {
        BillingService b = new BillingService(new InMemoryRepository());
        long[][] cases = {{0, 1}, {1, 1}, {20, 1}, {60, 1}, {61, 2}, {120, 2}, {135, 3}};
        for (long[] c : cases) {
            check("hours for " + c[0] + " min", b.calculateBillableHours(c[0]) == c[1]);
        }
    }

    private static void testParkingFlow() {
        Repository repo = new InMemoryRepository();
        ParkingService s = newService(repo);
        check("default slots 12", s.getSlots().size() == 12);
        check("default car slots 4", s.getCapacity(VehicleType.CAR) == 4);
        check("default emergency slots 2", s.getCapacity(VehicleType.EMERGENCY) == 2);

        s.registerVehicle("mh12ab1234", "Rahul", VehicleType.CAR, Role.EMPLOYEE);
        expectThrows("duplicate registration", IllegalStateException.class,
                () -> s.registerVehicle(" MH 12 AB 1234 ", "X", VehicleType.CAR, Role.EMPLOYEE));
        expectThrows("bad plate", IllegalArgumentException.class,
                () -> s.registerVehicle("NOTAPLATE", "X", VehicleType.CAR, Role.EMPLOYEE));
        expectThrows("unregistered park", IllegalArgumentException.class, () -> s.parkVehicle("KA01AB1234"));

        Booking b = s.parkVehicle("MH12AB1234");
        check("first car slot C01", b.getSlot().getSlotId().equals("C01"));
        expectThrows("double park", IllegalStateException.class, () -> s.parkVehicle("MH12AB1234"));
        check("occupied count", s.getOccupiedCount(VehicleType.CAR) == 1);

        Booking done = s.exitVehicleAfterMinutes("MH12AB1234", 61);
        check("61 min -> Rs.60", done.getBill().getTotalAmount().intValue() == 60);
        check("slot freed", s.getOccupiedCount(VehicleType.CAR) == 0);
        expectThrows("exit when not parked", IllegalStateException.class, () -> s.exitVehicle("MH12AB1234"));

        s.markBillPaid(done.getBill().getBillId());
        expectThrows("pay twice", IllegalStateException.class, () -> s.markBillPaid(done.getBill().getBillId()));
        ParkingService.Summary sum = s.getSummary();
        check("collected 60", sum.collected == 60 && sum.outstanding == 0);

        // Fill all bike slots, then one more must fail
        for (int i = 1; i <= 5; i++) {
            s.registerVehicle("MH14AB000" + i, "Owner" + i, VehicleType.BIKE, Role.EMPLOYEE);
        }
        for (int i = 1; i <= 4; i++) {
            s.parkVehicle("MH14AB000" + i);
        }
        expectThrows("bike slots full", IllegalStateException.class, () -> s.parkVehicle("MH14AB0005"));
    }

    private static void testCapacityAndRoles() {
        Repository repo = new InMemoryRepository();
        ParkingService s = newService(repo);
        expectThrows("employee cannot set capacity", IllegalStateException.class,
                () -> s.setCapacity(VehicleType.CAR, 10, Role.EMPLOYEE));
        expectThrows("employee cannot register VIP", IllegalStateException.class,
                () -> s.registerVehicle("MH12AB1111", "V", VehicleType.VIP, Role.EMPLOYEE));
        expectThrows("employee cannot register emergency", IllegalStateException.class,
                () -> s.registerVehicle("MH12AB2222", "Amb", VehicleType.EMERGENCY, Role.EMPLOYEE));
        expectThrows("employee cannot set rate", IllegalStateException.class,
                () -> s.setRate(VehicleType.CAR, 50, Role.EMPLOYEE));
        expectThrows("negative capacity", IllegalArgumentException.class,
                () -> s.setCapacity(VehicleType.CAR, -1, Role.ADMIN));
        expectThrows("capacity too large", IllegalArgumentException.class,
                () -> s.setCapacity(VehicleType.CAR, 1000, Role.ADMIN));

        s.setCapacity(VehicleType.CAR, 10, Role.ADMIN);
        check("grow to 10 cars", s.getCapacity(VehicleType.CAR) == 10);
        check("new slot id", s.getSlots(VehicleType.CAR).get(9).getSlotId().equals("C10"));
        s.setCapacity(VehicleType.VIP, 5, Role.ADMIN);
        s.setCapacity(VehicleType.EMERGENCY, 3, Role.ADMIN);
        check("vip 5", s.getCapacity(VehicleType.VIP) == 5);
        check("emergency 3", s.getCapacity(VehicleType.EMERGENCY) == 3);

        // Park in C01, then try to shrink to 0: must fail and change nothing
        s.registerVehicle("MH12AB3333", "Owner", VehicleType.CAR, Role.ADMIN);
        s.parkVehicle("MH12AB3333");
        expectThrows("cannot remove occupied slot", IllegalStateException.class,
                () -> s.setCapacity(VehicleType.CAR, 0, Role.ADMIN));
        check("capacity unchanged after failure", s.getCapacity(VehicleType.CAR) == 10);
        s.setCapacity(VehicleType.CAR, 1, Role.ADMIN); // only C01 (occupied) stays
        check("shrink to 1", s.getCapacity(VehicleType.CAR) == 1);

        // VIP and emergency vehicles use their own pools
        s.registerVehicle("DL1CAB1234", "Boss", VehicleType.VIP, Role.ADMIN);
        s.registerVehicle("DL3CAB0001", "City Hospital Ambulance", VehicleType.EMERGENCY, Role.ADMIN);
        check("vip slot V01", s.parkVehicle("DL1CAB1234").getSlot().getSlotId().equals("V01"));
        check("ambulance slot E01", s.parkVehicle("DL3CAB0001").getSlot().getSlotId().equals("E01"));
        Booking amb = s.exitVehicleAfterMinutes("DL3CAB0001", 300);
        check("emergency free by default", amb.getBill().getTotalAmount().intValue() == 0);

        // Admin changes the VIP rate; new bills use it
        s.setRate(VehicleType.VIP, 100, Role.ADMIN);
        Booking vip = s.exitVehicleAfterMinutes("DL1CAB1234", 130);
        check("vip 3h x 100", vip.getBill().getTotalAmount().intValue() == 300);

        // Reopening the service must not recreate default slots over admin's settings
        ParkingService again = newService(repo);
        check("capacity survives restart", again.getCapacity(VehicleType.CAR) == 1);
        expectThrows("shrink blocked while C01 occupied", IllegalStateException.class,
                () -> s.setCapacity(VehicleType.CAR, 0, Role.ADMIN));
    }

    private static void testAuth() {
        Repository repo = new InMemoryRepository();
        AuthService auth = new AuthService(repo);
        check("admin login", auth.login("admin", "admin123").getRole() == Role.ADMIN);
        check("employee login", auth.login(" Employee ", "employee123").getRole() == Role.EMPLOYEE);
        expectThrows("wrong password", IllegalArgumentException.class, () -> auth.login("admin", "nope"));
        expectThrows("unknown user", IllegalArgumentException.class, () -> auth.login("ghost", "whatever"));
        auth.addUser(Role.ADMIN, "Priya", "secret1", Role.EMPLOYEE);
        check("new user can log in", auth.login("priya", "secret1").getRole() == Role.EMPLOYEE);
        expectThrows("employee cannot add user", IllegalStateException.class,
                () -> auth.addUser(Role.EMPLOYEE, "evil", "secret1", Role.ADMIN));
        expectThrows("duplicate user", IllegalStateException.class,
                () -> auth.addUser(Role.ADMIN, "priya", "secret1", Role.EMPLOYEE));
        expectThrows("short password", IllegalArgumentException.class,
                () -> auth.addUser(Role.ADMIN, "newguy", "123", Role.EMPLOYEE));
        auth.changePassword("priya", "newsecret");
        check("password changed", auth.login("priya", "newsecret") != null);
        expectThrows("old password rejected", IllegalArgumentException.class, () -> auth.login("priya", "secret1"));
        List<User> all = auth.listUsers();
        check("3 users", all.size() == 3);
        check("hash not plain", !all.get(0).getPasswordHash().contains("admin123"));
    }
}
