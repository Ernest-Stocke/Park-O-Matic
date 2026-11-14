# Park-O-Matic

An intelligent parking space allocation and management system.
Java Swing GUI, SQL database (SQLite or MySQL), admin / employee login, configurable slot counts per
vehicle type, and Indian licence-plate validation.

## Project layout

```
parkomatic/
├── src/
│   ├── main/java/com/parkomatic/
│   │   ├── Main.java                  entry point: wires everything together
│   │   ├── model/                     plain data classes
│   │   │   ├── Vehicle, VehicleType, ParkingSlot, Booking, Bill
│   │   │   └── User, Role
│   │   ├── util/                      stateless helpers
│   │   │   ├── PlateValidator         Indian plate format check
│   │   │   └── PasswordHasher         salted PBKDF2 hashing
│   │   ├── service/                   business rules (no GUI, no SQL)
│   │   │   ├── ParkingService         register, park, exit, capacity, reports
│   │   │   ├── BillingService         rates and bill calculation
│   │   │   └── AuthService            login and user accounts
│   │   ├── repository/                storage
│   │   │   ├── Repository             interface the services depend on
│   │   │   ├── JdbcRepository         SQL implementation (SQLite / MySQL)
│   │   │   ├── Database               opens the connection from db.properties
│   │   │   └── DataAccessException
│   │   └── ui/                        Swing windows and panels
│   │       ├── LoginFrame, MainFrame
│   │       ├── SlotsPanel, OperationsPanel, BookingsPanel, BillsPanel, AdminPanel
│   │       └── Ui                     shared GUI helpers
│   └── test/java/com/parkomatic/
│       ├── TestRunner.java            logic tests (no database needed)
│       ├── repository/InMemoryRepository.java   fake storage used by tests
│       └── ui/GuiSmokeTest.java       builds the windows and saves screenshots
├── docs/schema.sql                    database tables (reference)
├── lib/                               put the JDBC driver jar here
├── db.properties.example              copy to db.properties to change the database
├── build.sh / run.sh / test.sh        Mac / Linux scripts
├── build.bat / run.bat                Windows scripts
└── README.md
```

Dependencies only point one way: `ui -> service -> repository / model / util`.
The GUI never touches SQL, and the services never touch Swing.


```

## 1. Default logins (first run only)

| Username   | Password      | Role     |
|------------|---------------|----------|
| `admin`    | `admin123`    | ADMIN    |
| `employee` | `employee123` | EMPLOYEE |

Change both passwords straight away (Change my password button, or Admin tab > Reset password).
Passwords are stored as salted PBKDF2 hashes, never as plain text.

## 2. What each role can do

| Feature                                            | Employee | Admin |
|----------------------------------------------------|:--------:|:-----:|
| Register Car / Bike, park, exit and bill           | yes      | yes   |
| Register VIP / Emergency vehicles                  | no       | yes   |
| View slots, bookings, bills; mark paid; export CSV | yes      | yes   |
| Set number of Car / Bike / VIP / Emergency slots   | no       | yes   |
| Set hourly rate per vehicle type                   | no       | yes   |
| Dashboard totals, add users, reset passwords       | no       | yes   |

These rules are enforced in the service layer, not just hidden in the GUI.

Defaults on first run: 4 car, 4 bike, 2 VIP, 2 emergency slots. Rate Rs.30/hour for car, bike and VIP;
Emergency vehicles are free (Rs.0). The admin can change any of these. Billing is per started hour with a
minimum of one hour.

Slot IDs: `C01..` cars, `B01..` bikes, `V01..` VIP, `E01..` emergency.
Reducing a slot count removes the highest-numbered slots and is refused if any of them is occupied.

## 3. Licence plate validation

`PlateValidator` accepts (spaces, hyphens and lower case are fine):

* Standard: state code + 1-2 digit RTO number + 1-3 letters + 4 digits, e.g. `MH12AB1234`, `DL1CAB1234`
  (the state code must be a real state/UT code; the RTO number and the 4-digit number cannot be zero)
* Bharat series: `22BH1234AA` (year 21 or later, last two letters never I or O)

It checks the **format only**. It cannot tell whether the plate is really issued, and it does not accept
temporary, diplomatic or military formats.

