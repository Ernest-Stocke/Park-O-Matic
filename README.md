
# Park-O-Matic (Swing + SQL edition)

Java Swing parking management system with a SQL database, admin / employee login,
configurable slot counts, and Indian licence-plate validation.

## 1. Run it (SQLite, no server needed)

You need JDK 11 or newer, plus the SQLite JDBC driver jar.

1. Download `sqlite-jdbc-<version>.jar` from https://github.com/xerial/sqlite-jdbc/releases
   (or Maven Central, group `org.xerial`) and put it in this folder.
   If you see `NoClassDefFoundError: org/slf4j/...`, also download `slf4j-api-<version>.jar` into the folder
   and add it to the classpath below.
2. Compile (all `.java` files in this folder; the `test` folder is optional):

```
javac *.java
```

3. Run:

```
Linux / macOS:  java -cp ".:sqlite-jdbc-<version>.jar" Main
Windows:        java -cp ".;sqlite-jdbc-<version>.jar" Main
```

A file `parkomatic.db` is created in the folder on first run. Delete it to start from scratch.

## 2. Default logins (first run only)

| Username   | Password      | Role     |
|------------|---------------|----------|
| `admin`    | `admin123`    | ADMIN    |
| `employee` | `employee123` | EMPLOYEE |

Change both passwords straight away (Change my password button, or Admin tab > Reset password).
Passwords are stored as salted PBKDF2 hashes, never as plain text.

## 3. What each role can do

| Feature                                           | Employee | Admin |
|---------------------------------------------------|:--------:|:-----:|
| Register Car / Bike, park, exit and bill          | yes      | yes   |
| Register VIP / Emergency vehicles                 | no       | yes   |
| View slots, bookings, bills; mark paid; export CSV| yes     | yes   |
| Set number of Car / Bike / VIP / Emergency slots  | no       | yes   |
| Set hourly rate per vehicle type                  | no       | yes   |
| Dashboard totals, add users, reset passwords      | no       | yes   |

Rules enforced in the service layer (not just hidden in the GUI).

Defaults on first run: 4 car, 4 bike, 2 VIP, 2 emergency slots. Rate Rs.30/hour for car, bike and VIP;
Emergency vehicles are free (Rs.0) - the admin can change any of these.
Billing is still per started hour with a minimum of one hour.

Slot IDs: `C01..` cars, `B01..` bikes, `V01..` VIP, `E01..` emergency.
Reducing a slot count removes the highest-numbered slots and is refused if any of them is occupied.

## 4. Licence plate validation

`PlateValidator` accepts (spaces / hyphens / lower case are fine):

* Standard: state code + 1-2 digit RTO number + 1-3 letters + 4 digits, e.g. `MH12AB1234`, `DL1CAB1234`
  (state code must be a real state/UT code; RTO number and the 4-digit number cannot be zero)
* Bharat series: `22BH1234AA` (year 21 or later, last two letters never I or O)

It checks the **format only**. It cannot tell whether the plate is really issued, and it does not accept
temporary, diplomatic or military plate formats.

## 5. Using MySQL / MariaDB instead

1. Create an empty database: `CREATE DATABASE parkomatic;`
2. Put the MySQL Connector/J jar in this folder.
3. Copy `db.properties.example` to `db.properties` and fill in the URL, user and password.
4. Run with the MySQL jar on the classpath instead of the SQLite jar.

Tables are created automatically (see `schema.sql`).

## 6. Tests

```
javac -d out *.java test/*.java
java -cp out TestRunner                       # logic tests, no database needed
xvfb-run java -cp out GuiSmokeTest shots/     # optional: builds the windows and saves screenshots
```

The tests use an in-memory repository, so they do not exercise the SQL code.

## 7. File map

| Layer    | Files |
|----------|-------|
| Model    | `Vehicle`, `VehicleType`, `ParkingSlot`, `Booking`, `Bill`, `User`, `Role` |
| Rules    | `PlateValidator`, `PasswordHasher`, `BillingService`, `ParkingService`, `AuthService` |
| Database | `Repository` (interface), `JdbcRepository`, `Database`, `DataAccessException` |
| GUI      | `Main`, `LoginFrame`, `MainFrame`, `SlotsPanel`, `OperationsPanel`, `BookingsPanel`, `BillsPanel`, `AdminPanel`, `Ui` |

The old console UI (`ConsoleUI`) has been replaced by the Swing GUI.

# PARK-O-MATIC
## An Intelligent Parking Resource Allocation and Management System

