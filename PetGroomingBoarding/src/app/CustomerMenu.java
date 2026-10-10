package app;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import dao.AppointmentDAO;
import dao.BoardingDAO;
import dao.GroomerDAO;
import dao.KennelDAO;
import dao.PaymentDAO;
import dao.PetDAO;
import dao.ServiceDAO;
import model.Appointment;
import model.Boarding;
import model.Customer;
import model.Groomer;
import model.Kennel;
import model.Pet;
import model.Service;
import exception.BookingException;
import util.ConsoleUtil;
import util.InputUtil;

/** Customer self-service portal. */
public class CustomerMenu {
    private static final int MAX_ID = Integer.MAX_VALUE;
    private static final int BOOKING_WINDOW_DAYS = 60;

    public static void start(Customer cust) {
        boolean on = true;
        while (on) {
            ConsoleUtil.header("CUSTOMER MENU - " + cust.getFullName());
            System.out.println("   1. My Pets (add / update / remove)");
            System.out.println("   2. View Grooming Services and Prices");
            System.out.println("   3. Book Grooming Appointment");
            System.out.println("   4. Book Boarding Stay");
            System.out.println("   5. My Grooming Appointments");
            System.out.println("   6. My Boarding Bookings");
            System.out.println("   7. Cancel Grooming Appointment");
            System.out.println("   8. Cancel Boarding Booking");
            System.out.println("   9. My Payments");
            System.out.println("  10. Logout");
            switch (InputUtil.readInt("  Enter choice: ", 1, 10)) {
                case 1: myPets(cust); break;
                case 2: ConsoleUtil.header("GROOMING SERVICES"); ServiceDAO.viewAll(true); InputUtil.pause(); break;
                case 3: bookGrooming(cust); break;
                case 4: bookBoarding(cust); break;
                case 5: ConsoleUtil.header("MY GROOMING APPOINTMENTS"); AppointmentDAO.viewByCustomer(cust.getCustomerId()); InputUtil.pause(); break;
                case 6: ConsoleUtil.header("MY BOARDING BOOKINGS"); BoardingDAO.viewByCustomer(cust.getCustomerId()); InputUtil.pause(); break;
                case 7: cancelGrooming(cust); break;
                case 8: cancelBoarding(cust); break;
                case 9: ConsoleUtil.header("MY PAYMENTS"); PaymentDAO.viewByCustomer(cust.getCustomerId()); InputUtil.pause(); break;
                default: on = false; ConsoleUtil.info("Logged out.");
            }
        }
    }

    // ------------------------------------------------------------------ pets
    private static void myPets(Customer cust) {
        boolean back = false;
        while (!back) {
            ConsoleUtil.header("MY PETS");
            System.out.println("   1. Add a pet   2. View my pets   3. Update pet details   4. Remove a pet   5. Back");
            switch (InputUtil.readInt("  Enter choice: ", 1, 5)) {
                case 1:
                    Pet p = new Pet();
                    p.setCustomerId(cust.getCustomerId());
                    p.setPetName(InputUtil.readString("  Pet name: ", 50));
                    p.setSpecies(InputUtil.readChoice("  Species:", "Dog", "Cat", "Rabbit", "Bird", "Other"));
                    p.setBreed(InputUtil.readString("  Breed: ", 50));
                    p.setAge(InputUtil.readInt("  Age in years (0-40): ", 0, 40));
                    p.setGender(InputUtil.readChoice("  Gender:", "Male", "Female"));
                    p.setWeightKg(InputUtil.readDouble("  Weight in kg (0.1-100): ", 0.1, 100));
                    p.setMedicalNotes(InputUtil.readOptional("  Medical notes / allergies (optional): ", 255));
                    int id = PetDAO.add(p);
                    if (id > 0) ConsoleUtil.success("Pet added with ID " + id + ".");
                    break;
                case 2: PetDAO.viewByCustomer(cust.getCustomerId()); break;
                case 3:
                    Pet up = pickPet(cust);
                    if (up == null) break;
                    int age = InputUtil.readInt("  New age in years (0-40): ", 0, 40);
                    double w = InputUtil.readDouble("  New weight in kg: ", 0.1, 100);
                    String notes = InputUtil.readOptional("  Medical notes: ", 255);
                    if (PetDAO.update(up.getPetId(), age, w, notes)) ConsoleUtil.success("Pet details updated.");
                    break;
                case 4:
                    Pet rm = pickPet(cust);
                    if (rm != null && InputUtil.confirm("  Remove " + rm.getPetName() + "?") && PetDAO.remove(rm.getPetId()))
                        ConsoleUtil.success("Pet removed.");
                    break;
                default: back = true;
            }
        }
    }

    /** Lists the customer's pets and asks for one; returns null if none/cancelled/invalid. */
    private static Pet pickPet(Customer cust) {
        int count = PetDAO.viewByCustomer(cust.getCustomerId());
        if (count <= 0) { ConsoleUtil.info("Please add a pet first (My Pets > Add a pet)."); return null; }
        int id = InputUtil.readInt("  Enter Pet ID (0 to cancel): ", 0, MAX_ID);
        if (id == 0) return null;
        Pet p = PetDAO.getById(id);
        if (p == null || p.getCustomerId() != cust.getCustomerId() || !"Active".equals(p.getStatus())) {
            ConsoleUtil.error("Invalid Pet ID. Choose one of your own pets from the list.");
            return null;
        }
        return p;
    }

    // ------------------------------------------------------------------ payment simulation
    private static String choosePaymentMethod() {
        return InputUtil.readChoice("  Payment method:", "UPI", "Card", "Net Banking", "Cash");
    }

    /** Academic simulation: 1 = bank approves, 2 = bank declines. */
    private static boolean simulatePayment(double amount) {
        System.out.println("  Amount to pay: Rs. " + String.format("%.2f", amount));
        System.out.println("  --- PAYMENT SIMULATION ---");
        int r = InputUtil.readInt("  1 = Payment Success, 2 = Payment Failed : ", 1, 2);
        return r == 1;
    }

    // ------------------------------------------------------------------ grooming booking
    private static void bookGrooming(Customer cust) {
        ConsoleUtil.header("BOOK GROOMING APPOINTMENT");
        Pet pet = pickPet(cust);
        if (pet == null) return;

        if (ServiceDAO.viewAll(true) <= 0) { ConsoleUtil.error("No services are available right now."); return; }
        int sid = InputUtil.readInt("  Enter Service ID (0 to cancel): ", 0, MAX_ID);
        if (sid == 0) return;
        Service svc = ServiceDAO.getById(sid);
        if (svc == null || !"Active".equals(svc.getStatus())) { ConsoleUtil.error("Invalid or inactive service."); return; }

        LocalDate date = InputUtil.readDate("  Appointment date");
        if (date.isAfter(LocalDate.now().plusDays(BOOKING_WINDOW_DAYS))) {
            ConsoleUtil.error("Bookings are open only up to " + BOOKING_WINDOW_DAYS + " days in advance.");
            return;
        }
        LocalTime start = InputUtil.readTime("  Start time (salon hours " + AppointmentDAO.OPEN + "-" + AppointmentDAO.CLOSE + ")");
        LocalTime end = AppointmentDAO.calcEndTime(start, svc.getDurationMinutes());
        try {
            AppointmentDAO.validateSlot(date, start, end);
        } catch (BookingException e) {
            ConsoleUtil.error(e.getMessage());
            return;
        }

        List<Groomer> free = AppointmentDAO.findFreeGroomers(date, start, end);
        if (free.isEmpty()) { ConsoleUtil.error("No groomer is free from " + start + " to " + end + ". Please try another time."); return; }
        System.out.println("\n  Groomers available from " + start + " to " + end + ":");
        for (Groomer g : free) System.out.println("   [" + g.getGroomerId() + "] " + g.getFullName() + " - " + g.getSpecialization());
        int gid = InputUtil.readInt("  Enter Groomer ID (0 to cancel): ", 0, MAX_ID);
        if (gid == 0) return;
        Groomer chosen = null;
        for (Groomer g : free) if (g.getGroomerId() == gid) chosen = g;
        if (chosen == null) { ConsoleUtil.error("Please choose one of the listed groomers."); return; }

        System.out.println("\n  ----- BOOKING SUMMARY -----");
        System.out.println("  Pet     : " + pet.getPetName() + " (" + pet.getSpecies() + ")");
        System.out.println("  Service : " + svc.getServiceName() + " (" + svc.getDurationMinutes() + " min)");
        System.out.println("  Groomer : " + chosen.getFullName());
        System.out.println("  When    : " + date + ", " + start + " - " + end);
        System.out.println("  Price   : Rs. " + String.format("%.2f", svc.getPrice()));
        if (!InputUtil.confirm("  Proceed to payment?")) { ConsoleUtil.info("Booking cancelled."); return; }

        String method = choosePaymentMethod();
        boolean ok = simulatePayment(svc.getPrice());

        Appointment a = new Appointment();
        a.setPetId(pet.getPetId());
        a.setServiceId(svc.getServiceId());
        a.setGroomerId(chosen.getGroomerId());
        a.setAppointmentDate(date);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setPrice(svc.getPrice());
        try {
            int id = AppointmentDAO.book(a, method, ok);
            ConsoleUtil.success("Payment successful. Appointment confirmed!");
            ConsoleUtil.header("GROOMING RECEIPT");
            AppointmentDAO.printReceipt(id);
        } catch (BookingException e) {
            ConsoleUtil.error(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ boarding booking
    private static void bookBoarding(Customer cust) {
        ConsoleUtil.header("BOOK BOARDING STAY");
        Pet pet = pickPet(cust);
        if (pet == null) return;

        LocalDate in = InputUtil.readDate("  Check-in date");
        LocalDate out = InputUtil.readDate("  Check-out date");
        if (in.isBefore(LocalDate.now())) { ConsoleUtil.error("Check-in date cannot be in the past."); return; }
        if (!out.isAfter(in)) { ConsoleUtil.error("Check-out must be after check-in."); return; }
        if (BoardingDAO.calcDays(in, out) > 60) { ConsoleUtil.error("Maximum stay is 60 days."); return; }

        System.out.println("\n  Kennels available for " + pet.getSpecies() + "s from " + in + " to " + out + ":");
        if (KennelDAO.viewAvailable(in, out, pet.getSpecies()) <= 0) {
            ConsoleUtil.info("No kennel is free for these dates. Try different dates.");
            return;
        }
        int kid = InputUtil.readInt("  Enter Kennel ID (0 to cancel): ", 0, MAX_ID);
        if (kid == 0) return;
        Kennel kennel = KennelDAO.getById(kid);
        if (kennel == null) { ConsoleUtil.error("Kennel not found."); return; }

        String notes = InputUtil.readOptional("  Special instructions - diet, medicines (optional): ", 255);
        int days = BoardingDAO.calcDays(in, out);
        double total = BoardingDAO.calcTotal(days, kennel.getDailyRate());

        System.out.println("\n  ----- BOOKING SUMMARY -----");
        System.out.println("  Pet     : " + pet.getPetName() + " (" + pet.getSpecies() + ")");
        System.out.println("  Kennel  : " + kennel.getKennelName() + " @ Rs. " + String.format("%.2f", kennel.getDailyRate()) + " per night");
        System.out.println("  Stay    : " + in + " to " + out + " (" + days + " night(s))");
        if (days >= BoardingDAO.LONG_STAY_DAYS)
            System.out.println("  Offer   : " + (int) (BoardingDAO.LONG_STAY_DISCOUNT * 100) + "% long-stay discount applied");
        System.out.println("  Total   : Rs. " + String.format("%.2f", total));
        if (!InputUtil.confirm("  Proceed to payment?")) { ConsoleUtil.info("Booking cancelled."); return; }

        String method = choosePaymentMethod();
        boolean ok = simulatePayment(total);

        Boarding b = new Boarding();
        b.setPetId(pet.getPetId());
        b.setKennelId(kennel.getKennelId());
        b.setCheckInDate(in);
        b.setCheckOutDate(out);
        b.setSpecialInstructions(notes);
        try {
            int id = BoardingDAO.book(b, method, ok);
            ConsoleUtil.success("Payment successful. Boarding confirmed!");
            ConsoleUtil.header("BOARDING RECEIPT");
            BoardingDAO.printReceipt(id);
        } catch (BookingException e) {
            ConsoleUtil.error(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ cancellations
    private static void cancelGrooming(Customer cust) {
        ConsoleUtil.header("CANCEL GROOMING APPOINTMENT");
        AppointmentDAO.viewByCustomer(cust.getCustomerId());
        int id = InputUtil.readInt("  Appointment ID to cancel (0 to go back): ", 0, MAX_ID);
        if (id == 0 || !InputUtil.confirm("  Cancel appointment " + id + "? The amount will be refunded.")) return;
        try {
            double amt = AppointmentDAO.cancel(id, cust.getCustomerId());
            ConsoleUtil.success("Appointment cancelled. Rs. " + String.format("%.2f", amt) + " refunded.");
        } catch (BookingException e) {
            ConsoleUtil.error(e.getMessage());
        }
    }

    private static void cancelBoarding(Customer cust) {
        ConsoleUtil.header("CANCEL BOARDING BOOKING");
        BoardingDAO.viewByCustomer(cust.getCustomerId());
        int id = InputUtil.readInt("  Boarding ID to cancel (0 to go back): ", 0, MAX_ID);
        if (id == 0 || !InputUtil.confirm("  Cancel boarding " + id + "? The amount will be refunded.")) return;
        try {
            double amt = BoardingDAO.cancel(id, cust.getCustomerId());
            ConsoleUtil.success("Boarding cancelled. Rs. " + String.format("%.2f", amt) + " refunded.");
        } catch (BookingException e) {
            ConsoleUtil.error(e.getMessage());
        }
    }
}
