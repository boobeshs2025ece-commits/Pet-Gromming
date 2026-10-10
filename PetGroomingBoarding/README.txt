================================================================================
        PET GROOMING AND BOARDING MANAGEMENT SYSTEM - CORE JAVA + JDBC
================================================================================

1. PROJECT OVERVIEW
-------------------
A console-based application built with Core Java, JDBC and MySQL using the
Model-DAO-Util-App layered architecture. It manages a pet care centre that offers
two businesses: (a) GROOMING appointments and (b) BOARDING (pet hotel) stays.

Roles
1. Administrator : manages grooming services, groomers, boarding kennels, views
   customers/pets, manages appointments and boarding stays (complete, check-in,
   check-out, cancel with refund), views payments, and runs 16 SQL reports.
2. Customer      : registers/logs in, manages own pets, books grooming
   appointments (choose service, date, time, free groomer), books boarding stays
   (kennel availability check, automatic price + long-stay discount), simulates
   payment, views history and cancels bookings with automatic refund.


2. TECHNOLOGIES USED
--------------------
* Core Java (JDK 17 or newer; tested with JDK 21)
* JDBC with MySQL Connector/J 8.0.33   (lib/mysql-connector-j-8.0.33.jar)
* MySQL Server 8.0+ and MySQL Workbench
* SHA-256 password hashing (java.security.MessageDigest)
* JDBC transactions: setAutoCommit(false) / commit() / rollback()
* java.time API (LocalDate, LocalTime, LocalDateTime)


3. FOLDER STRUCTURE
-------------------
PetGroomingBoarding/
|-- src/
|   |-- app/
|   |   |-- Main.java           (start-up, main menu, login, registration)
|   |   |-- AdminMenu.java      (all admin screens)
|   |   `-- CustomerMenu.java   (all customer screens, booking + payment flow)
|   |-- model/                  (POJOs: Admin, Customer, Pet, Service, Groomer,
|   |                            Kennel, Appointment, Boarding, Payment)
|   |-- dao/                    (all SQL lives here)
|   |   |-- AdminDAO, CustomerDAO, PetDAO, ServiceDAO, GroomerDAO, KennelDAO
|   |   |-- AppointmentDAO      (slot rules, overlap check, booking transaction)
|   |   |-- BoardingDAO         (stay rules, pricing, booking transaction)
|   |   |-- PaymentDAO          (payment record + refund)
|   |   `-- ReportsDAO          (16 reports)
|   `-- util/
|       |-- DBConnection        (connection manager + credential discovery)
|       |-- DatabaseSetup       (auto-creates DB, tables, sample data)
|       |-- InputUtil           (validated console input)
|       |-- ConsoleUtil         (table printer for any SELECT)
|       |-- PasswordUtil        (SHA-256)
|       `-- BookingException    (business-rule errors)
|-- lib/                        (put mysql-connector-j-8.0.33.jar here)
|-- database.sql                (full DDL + sample data for MySQL Workbench)
|-- db.properties               (host, port, user, password)
|-- run.bat                     (Windows build and run)
|-- .project / .classpath       (Eclipse project files)
`-- README.txt


4. HOW TO RUN IN ECLIPSE  (recommended)
---------------------------------------
Step 1  Install: JDK 17+, MySQL Server 8, MySQL Workbench. Make sure the MySQL
        service is running.
Step 2  Download mysql-connector-j-8.0.33.jar (link in lib/PUT_MYSQL_CONNECTOR_JAR_HERE.txt)
        and copy it into the project's lib folder with exactly that name.
Step 3  Eclipse: File > Import > General > Existing Projects into Workspace >
        Browse > select the PetGroomingBoarding folder (or the unzipped folder) > Finish.
        (If you prefer a new project: copy the src folder into a new Java Project and
        add the jar via Build Path > Configure Build Path > Libraries > Add JARs.)
Step 4  Open db.properties and set your MySQL password (default is root / 1234).
        If the password is wrong, the program will ask for it and save it.
Step 5  Right-click src/app/Main.java > Run As > Java Application.

The database pet_care_db and all 9 tables are created automatically on first run,
with sample data. To see them: open MySQL Workbench > connect > refresh Schemas.

Optional manual import: Workbench > File > Open SQL Script > database.sql > Execute.


5. DEMONSTRATION LOGIN DETAILS
------------------------------
Admin      : username  admin                    password  admin123
Customer 1 : ravi.kumar@example.com             password  cust123   (pets: Bruno, Coco)
Customer 2 : priya.sharma@example.com           password  pass123   (pet: Whiskers)
(All passwords are stored as SHA-256 hashes.)


6. DATABASE DESIGN (9 TABLES)
-----------------------------
customer (1) ----< pet (1) ----< appointment >---- (1) service
                         |              `--------- (1) groomer
                         `------< boarding >------- (1) kennel
appointment (1) ----< payment >---- (1) boarding      (a payment belongs to ONE of them)
admin (standalone login table)

Relationships
1. customer -> pet          : one owner has many pets
2. pet -> appointment       : one pet has many grooming appointments
3. service -> appointment   : one service is used in many appointments
4. groomer -> appointment   : one groomer handles many (non-overlapping) appointments
5. pet -> boarding          : one pet has many boarding stays
6. kennel -> boarding       : one kennel hosts many (non-overlapping) stays
7. appointment/boarding -> payment : payment has two nullable foreign keys,
   exactly one of them is filled; ON DELETE CASCADE.


7. BUSINESS RULES IMPLEMENTED (good viva points)
-----------------------------------------------
Grooming
* Salon hours 09:00 - 18:00; end time = service duration + 10 minute buffer.
* A groomer cannot have two overlapping appointments; a pet cannot either.
  Overlap test:  existing.start < new.end  AND  existing.end > new.start.
* Only groomers who are free in the chosen slot are offered to the customer.
* Bookings allowed from today up to 60 days ahead; past times are rejected.
* Admin marks appointments Completed (today or earlier only).

Boarding
* Nights = check-out date - check-in date (must be at least 1).
* Kennel must be 'Available', suit the species (Dog / Cat / Any) and be free for
  the whole stay (overlap test on dates). Same pet cannot have overlapping stays.
* Stays of 7 or more nights get a 10% discount.
* Admin check-in (only on/after check-in date) and check-out (only if Checked-In).

Money, safety and integrity
* Booking + payment are ONE transaction: if payment fails or anything throws, rollback
  - nothing is saved. The groomer/kennel row is locked (SELECT ... FOR UPDATE) so
  two customers can never take the same slot at the same time.
* Cancellation: status -> Cancelled (history kept) and payment -> Refunded in one
  transaction. Customers can cancel only future bookings (boarding: until the day
  before check-in); admin can cancel any 'Booked' item.
* Payment is simulated: choose 1 = Success, 2 = Failed.
* Pets are soft-deleted (status Inactive) so history stays; deletes of services,
  groomers, kennels and customers are blocked when history exists.
* All SQL uses PreparedStatement (prevents SQL injection).


8. THE 16 REPORTS (Admin > Reports)
-----------------------------------
 1 Total customers              9 Current boarding guests
 2 Total pets                  10 Upcoming boarding reservations
 3 Pets by species             11 Kennel occupancy on a date
 4 Pets per customer           12 Boarding history of a pet
 5 Appointments on a date      13 Grooming revenue
 6 Upcoming appointments       14 Boarding revenue
 7 Popular services            15 Net revenue and refunds
 8 Groomer workload            16 Top customers by spending


9. SUGGESTED DEMO FLOW
----------------------
1. Run the program, log in as customer ravi.kumar@example.com / cust123.
2. My Pets > View. Book Grooming: pick Bruno, Basic Bath, a date, 11:00 - choose a
   groomer - pay with UPI (Success) - receipt appears.
3. Try the same groomer and time again with Coco: the system offers other groomers.
4. Book Boarding for Bruno for 8 nights: see the 10% long-stay discount.
5. Cancel one booking: refund message; check My Payments (status Refunded).
6. Log in as admin: Appointments > Mark Completed, Boarding > Check-in, Reports > 15.


10. TROUBLESHOOTING
-------------------
* "MySQL JDBC driver not found"  -> jar missing from lib/ or not on Build Path
  (right-click project > Build Path > Configure Build Path > Libraries > Add JARs).
* "Could not log in to MySQL"    -> start MySQL service (services.msc > MySQL80) and
  fix db.user / db.password in db.properties.
* Eclipse shows a red project    -> the jar is missing; add it, then Project > Clean.
* "Unknown database"             -> just run Main once; it creates the database.
================================================================================
