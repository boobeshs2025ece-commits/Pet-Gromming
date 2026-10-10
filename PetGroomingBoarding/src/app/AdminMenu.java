package app;

import java.time.LocalDate;
import dao.AppointmentDAO;
import dao.BoardingDAO;
import dao.CustomerDAO;
import dao.GroomerDAO;
import dao.KennelDAO;
import dao.PaymentDAO;
import dao.PetDAO;
import dao.ReportsDAO;
import dao.ServiceDAO;
import model.Groomer;
import model.Kennel;
import model.Service;
import exception.BookingException;
import util.ConsoleUtil;
import util.InputUtil;

/** All administrator screens. */
public class AdminMenu {
    private static final int MAX_ID = Integer.MAX_VALUE;

    public static void start() {
        boolean on = true;
        while (on) {
            ConsoleUtil.header("ADMIN DASHBOARD");
            System.out.println("   1. Manage Grooming Services");
            System.out.println("   2. Manage Groomers");
            System.out.println("   3. Manage Boarding Kennels");
            System.out.println("   4. View Customers and Pets");
            System.out.println("   5. Manage Grooming Appointments");
            System.out.println("   6. Manage Boarding Stays");
            System.out.println("   7. View All Payments");
            System.out.println("   8. Reports (16 reports)");
            System.out.println("   9. Logout");
            switch (InputUtil.readInt("  Enter choice: ", 1, 9)) {
                case 1: manageServices(); break;
                case 2: manageGroomers(); break;
                case 3: manageKennels(); break;
                case 4: customersAndPets(); break;
                case 5: manageAppointments(); break;
                case 6: manageBoarding(); break;
                case 7: ConsoleUtil.header("ALL PAYMENTS"); PaymentDAO.viewAll(); InputUtil.pause(); break;
                case 8: reports(); break;
                default: on = false; ConsoleUtil.info("Logged out.");
            }
        }
    }

    // ------------------------------------------------------------------ services
    private static void manageServices() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.header("MANAGE GROOMING SERVICES");
            System.out.println("   1. Add service   2. View all   3. Update price   4. Activate/Deactivate   5. Delete   6. Back");
            switch (InputUtil.readInt("  Enter choice: ", 1, 6)) {
                case 1:
                    Service s = new Service();
                    s.setServiceName(InputUtil.readString("  Service name: ", 80));
                    s.setDescription(InputUtil.readOptional("  Description: ", 255));
                    s.setPrice(InputUtil.readDouble("  Price (Rs.): ", 1, 100000));
                    s.setDurationMinutes(InputUtil.readInt("  Duration in minutes (10-240): ", 10, 240));
                    if (ServiceDAO.add(s)) ConsoleUtil.success("Service added.");
                    break;
                case 2: ServiceDAO.viewAll(false); break;
                case 3:
                    ServiceDAO.viewAll(false);
                    int id = InputUtil.readInt("  Service ID: ", 1, MAX_ID);
                    if (ServiceDAO.getById(id) == null) { ConsoleUtil.error("Service not found."); break; }
                    double price = InputUtil.readDouble("  New price (Rs.): ", 1, 100000);
                    if (ServiceDAO.updatePrice(id, price)) ConsoleUtil.success("Price updated.");
                    break;
                case 4:
                    ServiceDAO.viewAll(false);
                    if (ServiceDAO.toggleStatus(InputUtil.readInt("  Service ID: ", 1, MAX_ID))) ConsoleUtil.success("Status toggled.");
                    else ConsoleUtil.error("Service not found.");
                    break;
                case 5:
                    ServiceDAO.viewAll(false);
                    if (ServiceDAO.delete(InputUtil.readInt("  Service ID to delete: ", 1, MAX_ID))) ConsoleUtil.success("Service deleted.");
                    break;
                default: back = true;
            }
        }
    }

    // ------------------------------------------------------------------ groomers
    private static void manageGroomers() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.header("MANAGE GROOMERS");
            System.out.println("   1. Add groomer   2. View all   3. Activate/Deactivate   4. Delete   5. Back");
            switch (InputUtil.readInt("  Enter choice: ", 1, 5)) {
                case 1:
                    Groomer g = new Groomer();
                    g.setFullName(InputUtil.readString("  Full name: ", 100));
                    g.setSpecialization(InputUtil.readOptional("  Specialization: ", 80));
                    g.setPhone(InputUtil.readPhone("  Phone (10 digits): "));
                    if (GroomerDAO.add(g)) ConsoleUtil.success("Groomer added.");
                    break;
                case 2: GroomerDAO.viewAll(); break;
                case 3:
                    GroomerDAO.viewAll();
                    if (GroomerDAO.toggleStatus(InputUtil.readInt("  Groomer ID: ", 1, MAX_ID))) ConsoleUtil.success("Status toggled.");
                    else ConsoleUtil.error("Groomer not found.");
                    break;
                case 4:
                    GroomerDAO.viewAll();
                    if (GroomerDAO.delete(InputUtil.readInt("  Groomer ID to delete: ", 1, MAX_ID))) ConsoleUtil.success("Groomer deleted.");
                    break;
                default: back = true;
            }
        }
    }

    // ------------------------------------------------------------------ kennels
    private static void manageKennels() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.header("MANAGE BOARDING KENNELS");
            System.out.println("   1. Add kennel   2. View all   3. Set Available/Maintenance   4. Check availability   5. Delete   6. Back");
            switch (InputUtil.readInt("  Enter choice: ", 1, 6)) {
                case 1:
                    Kennel k = new Kennel();
                    k.setKennelName(InputUtil.readString("  Kennel name (e.g. K4 - Dog Standard): ", 50));
                    k.setKennelType(InputUtil.readChoice("  Kennel type:", "Standard", "Deluxe", "Suite"));
                    k.setPetType(InputUtil.readChoice("  Suitable for:", "Dog", "Cat", "Any"));
                    k.setDailyRate(InputUtil.readDouble("  Daily rate (Rs.): ", 1, 100000));
                    if (KennelDAO.add(k)) ConsoleUtil.success("Kennel added.");
                    break;
                case 2: KennelDAO.viewAll(); break;
                case 3:
                    KennelDAO.viewAll();
                    int id = InputUtil.readInt("  Kennel ID: ", 1, MAX_ID);
                    if (KennelDAO.getById(id) == null) { ConsoleUtil.error("Kennel not found."); break; }
                    String st = InputUtil.readChoice("  New status:", "Available", "Maintenance");
                    if (KennelDAO.setStatus(id, st)) ConsoleUtil.success("Kennel status updated.");
                    break;
                case 4:
                    LocalDate in = InputUtil.readDate("  Check-in date");
                    LocalDate out = InputUtil.readDate("  Check-out date");
                    if (!out.isAfter(in)) { ConsoleUtil.error("Check-out must be after check-in."); break; }
                    String sp = InputUtil.readChoice("  Species:", "Dog", "Cat", "Rabbit", "Bird", "Other");
                    KennelDAO.viewAvailable(in, out, sp);
                    break;
                case 5:
                    KennelDAO.viewAll();
                    if (KennelDAO.delete(InputUtil.readInt("  Kennel ID to delete: ", 1, MAX_ID))) ConsoleUtil.success("Kennel deleted.");
                    break;
                default: back = true;
            }
        }
    }

    // ------------------------------------------------------------------ customers & pets
    private static void customersAndPets() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.header("CUSTOMERS AND PETS");
            System.out.println("   1. View all customers   2. Search customer by name   3. View all pets");
            System.out.println("   4. View pets of a customer   5. Delete customer   6. Back");
            switch (InputUtil.readInt("  Enter choice: ", 1, 6)) {
                case 1: CustomerDAO.viewAll(); break;
                case 2: CustomerDAO.searchByName(InputUtil.readString("  Name contains: ", 100)); break;
                case 3: PetDAO.viewAll(); break;
                case 4: PetDAO.viewByCustomer(InputUtil.readInt("  Customer ID: ", 1, MAX_ID)); break;
                case 5:
                    CustomerDAO.viewAll();
                    if (CustomerDAO.delete(InputUtil.readInt("  Customer ID to delete: ", 1, MAX_ID))) ConsoleUtil.success("Customer deleted.");
                    break;
                default: back = true;
            }
        }
    }

    // ------------------------------------------------------------------ appointments
    private static void manageAppointments() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.header("MANAGE GROOMING APPOINTMENTS");
            System.out.println("   1. View all   2. View by date   3. Mark as Completed   4. Cancel and refund   5. Back");
            switch (InputUtil.readInt("  Enter choice: ", 1, 5)) {
                case 1: AppointmentDAO.viewAll(); break;
                case 2: AppointmentDAO.viewByDate(InputUtil.readDate("  Date")); break;
                case 3:
                    AppointmentDAO.viewAll();
                    if (AppointmentDAO.complete(InputUtil.readInt("  Appointment ID: ", 1, MAX_ID))) ConsoleUtil.success("Marked as Completed.");
                    else ConsoleUtil.error("Not updated: appointment must be 'Booked' and its date must be today or earlier.");
                    break;
                case 4:
                    AppointmentDAO.viewAll();
                    int id = InputUtil.readInt("  Appointment ID to cancel: ", 1, MAX_ID);
                    try {
                        double amt = AppointmentDAO.cancel(id, null);
                        ConsoleUtil.success("Appointment cancelled. Rs. " + String.format("%.2f", amt) + " refunded.");
                    } catch (BookingException e) { ConsoleUtil.error(e.getMessage()); }
                    break;
                default: back = true;
            }
        }
    }

    // ------------------------------------------------------------------ boarding
    private static void manageBoarding() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.header("MANAGE BOARDING STAYS");
            System.out.println("   1. View all   2. Current guests   3. Check-in   4. Check-out   5. Cancel and refund   6. Back");
            switch (InputUtil.readInt("  Enter choice: ", 1, 6)) {
                case 1: BoardingDAO.viewAll(); break;
                case 2: BoardingDAO.viewCurrentGuests(); break;
                case 3:
                    BoardingDAO.viewAll();
                    if (BoardingDAO.checkIn(InputUtil.readInt("  Boarding ID to check in: ", 1, MAX_ID))) ConsoleUtil.success("Pet checked in.");
                    else ConsoleUtil.error("Not updated: stay must be 'Booked' and the check-in date must have arrived.");
                    break;
                case 4:
                    BoardingDAO.viewCurrentGuests();
                    if (BoardingDAO.checkOut(InputUtil.readInt("  Boarding ID to check out: ", 1, MAX_ID))) ConsoleUtil.success("Pet checked out.");
                    else ConsoleUtil.error("Not updated: only 'Checked-In' stays can be checked out.");
                    break;
                case 5:
                    BoardingDAO.viewAll();
                    int id = InputUtil.readInt("  Boarding ID to cancel: ", 1, MAX_ID);
                    try {
                        double amt = BoardingDAO.cancel(id, null);
                        ConsoleUtil.success("Stay cancelled. Rs. " + String.format("%.2f", amt) + " refunded.");
                    } catch (BookingException e) { ConsoleUtil.error(e.getMessage()); }
                    break;
                default: back = true;
            }
        }
    }

    // ------------------------------------------------------------------ reports
    private static void reports() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.header("REPORTS");
            for (int i = 0; i < ReportsDAO.TITLES.length; i++) System.out.printf("   %2d. %s%n", i + 1, ReportsDAO.TITLES[i]);
            System.out.println("   17. Back");
            int no = InputUtil.readInt("  Enter report number: ", 1, 17);
            if (no == 17) { back = true; continue; }
            LocalDate date = null;
            int petId = 0;
            if (ReportsDAO.needsDate(no)) date = InputUtil.readDate("  Date");
            if (ReportsDAO.needsPetId(no)) petId = InputUtil.readInt("  Pet ID: ", 1, MAX_ID);
            ReportsDAO.run(no, date, petId);
            InputUtil.pause();
        }
    }
}
