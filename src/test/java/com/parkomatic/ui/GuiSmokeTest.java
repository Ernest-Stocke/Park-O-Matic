package com.parkomatic.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.parkomatic.model.Role;
import com.parkomatic.model.VehicleType;
import com.parkomatic.repository.InMemoryRepository;
import com.parkomatic.repository.Repository;
import com.parkomatic.service.AuthService;
import com.parkomatic.service.BillingService;
import com.parkomatic.service.ParkingService;

/** Builds the real windows against an in-memory repository and saves screenshots. Needs a display (xvfb-run). */
public class GuiSmokeTest {
    static JTabbedPane findTabs(Container c) {
        for (Component child : c.getComponents()) {
            if (child instanceof JTabbedPane) {
                return (JTabbedPane) child;
            }
            if (child instanceof Container) {
                JTabbedPane t = findTabs((Container) child);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    static void shot(JFrame frame, String path) throws Exception {
        frame.setVisible(true);
        BufferedImage img = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        frame.getContentPane().paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
    }

    public static void main(String[] args) throws Exception {
        String dir = args[0];
        Repository repo = new InMemoryRepository();
        BillingService billing = new BillingService(repo);
        ParkingService parking = new ParkingService(repo, billing);
        AuthService auth = new AuthService(repo);

        parking.registerVehicle("MH12AB1234", "Rahul Sharma", VehicleType.CAR, Role.ADMIN);
        parking.registerVehicle("DL8SAB4321", "Priya Verma", VehicleType.BIKE, Role.ADMIN);
        parking.registerVehicle("DL1CAB0007", "Chairman", VehicleType.VIP, Role.ADMIN);
        parking.registerVehicle("UP14CD1111", "City Hospital", VehicleType.EMERGENCY, Role.ADMIN);
        parking.registerVehicle("22BH1234AA", "Amit Gupta", VehicleType.CAR, Role.ADMIN);
        parking.parkVehicle("MH12AB1234");
        parking.parkVehicle("DL1CAB0007");
        parking.parkVehicle("UP14CD1111");
        parking.parkVehicle("22BH1234AA");
        parking.exitVehicleAfterMinutes("22BH1234AA", 135);
        parking.parkVehicle("DL8SAB4321");
        parking.exitVehicleAfterMinutes("DL8SAB4321", 20);
        parking.markBillPaid("BILL-002");

        final String[] names = {"slots", "operations", "bookings", "bills", "admin"};
        final int[] tabCount = new int[2];
        SwingUtilities.invokeAndWait(() -> {
            try {
                LoginFrame login = new LoginFrame(auth, u -> { });
                shot(login, dir + "/login.png");
                login.dispose();

                MainFrame admin = new MainFrame(parking, auth, auth.login("admin", "admin123"), () -> { });
                JTabbedPane tabs = findTabs(admin.getContentPane());
                tabCount[0] = tabs.getTabCount();
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    tabs.setSelectedIndex(i);
                    admin.refreshAll();
                    shot(admin, dir + "/admin_" + names[i] + ".png");
                }
                admin.dispose();

                MainFrame emp = new MainFrame(parking, auth, auth.login("employee", "employee123"), () -> { });
                tabCount[1] = findTabs(emp.getContentPane()).getTabCount();
                emp.dispose();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("admin tabs=" + tabCount[0] + " employee tabs=" + tabCount[1]);
        System.exit(tabCount[0] == 5 && tabCount[1] == 4 ? 0 : 1);
    }
}
