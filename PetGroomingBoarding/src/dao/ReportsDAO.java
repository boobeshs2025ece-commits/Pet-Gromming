package dao;

import java.time.LocalDate;
import util.ConsoleUtil;

/** 16 analytical SQL reports for the admin (joins, GROUP BY, aggregates, sub-queries). */
public class ReportsDAO {

    public static final String[] TITLES = {
        "Total registered customers",
        "Total registered pets",
        "Pets grouped by species",
        "Number of pets per customer",
        "Grooming appointments on a selected date",
        "Upcoming grooming appointments",
        "Most popular grooming services",
        "Groomer workload summary",
        "Current boarding guests (checked-in)",
        "Upcoming boarding reservations",
        "Kennel occupancy on a selected date",
        "Boarding history of a pet",
        "Total grooming revenue (successful payments)",
        "Total boarding revenue (successful payments)",
        "Revenue summary (net revenue and refunds)",
        "Top customers by spending"
    };

    /** Reports 5 and 11 need a date; report 12 needs a pet id. */
    public static boolean needsDate(int no) { return no == 5 || no == 11; }
    public static boolean needsPetId(int no) { return no == 12; }

    public static void run(int no, LocalDate date, int petId) {
        ConsoleUtil.header("REPORT " + no + ": " + TITLES[no - 1]);
        switch (no) {
            case 1:
                ConsoleUtil.showQuery("SELECT COUNT(*) AS 'Total Customers' FROM customer");
                break;
            case 2:
                ConsoleUtil.showQuery("SELECT COUNT(*) AS 'Total Active Pets' FROM pet WHERE status = 'Active'");
                break;
            case 3:
                ConsoleUtil.showQuery("SELECT species AS 'Species', COUNT(*) AS 'Pets' FROM pet WHERE status = 'Active' "
                        + "GROUP BY species ORDER BY COUNT(*) DESC");
                break;
            case 4:
                ConsoleUtil.showQuery("SELECT c.customer_id AS 'Cust ID', c.full_name AS 'Owner', COUNT(p.pet_id) AS 'Pets' "
                        + "FROM customer c LEFT JOIN pet p ON p.customer_id = c.customer_id AND p.status = 'Active' "
                        + "GROUP BY c.customer_id, c.full_name ORDER BY COUNT(p.pet_id) DESC");
                break;
            case 5:
                AppointmentDAO.viewByDate(date);
                break;
            case 6:
                ConsoleUtil.showQuery("SELECT a.appointment_id AS 'Appt ID', p.pet_name AS 'Pet', s.service_name AS 'Service', "
                        + "g.full_name AS 'Groomer', a.appointment_date AS 'Date', a.start_time AS 'Start' FROM appointment a "
                        + "JOIN pet p ON a.pet_id = p.pet_id JOIN service s ON a.service_id = s.service_id "
                        + "JOIN groomer g ON a.groomer_id = g.groomer_id "
                        + "WHERE a.status = 'Booked' AND a.appointment_date >= CURDATE() ORDER BY a.appointment_date, a.start_time");
                break;
            case 7:
                ConsoleUtil.showQuery("SELECT s.service_name AS 'Service', COUNT(a.appointment_id) AS 'Bookings', "
                        + "COALESCE(SUM(a.price), 0) AS 'Value (Rs.)' FROM service s "
                        + "LEFT JOIN appointment a ON a.service_id = s.service_id AND a.status <> 'Cancelled' "
                        + "GROUP BY s.service_id, s.service_name ORDER BY COUNT(a.appointment_id) DESC");
                break;
            case 8:
                ConsoleUtil.showQuery("SELECT g.full_name AS 'Groomer', "
                        + "COALESCE(SUM(a.status = 'Booked'), 0) AS 'Booked', "
                        + "COALESCE(SUM(a.status = 'Completed'), 0) AS 'Completed', "
                        + "COALESCE(SUM(a.status = 'Cancelled'), 0) AS 'Cancelled' FROM groomer g "
                        + "LEFT JOIN appointment a ON a.groomer_id = g.groomer_id GROUP BY g.groomer_id, g.full_name");
                break;
            case 9:
                BoardingDAO.viewCurrentGuests();
                break;
            case 10:
                ConsoleUtil.showQuery("SELECT b.boarding_id AS 'Board ID', p.pet_name AS 'Pet', k.kennel_name AS 'Kennel', "
                        + "b.check_in_date AS 'Check-In', b.check_out_date AS 'Check-Out', b.total_amount AS 'Total (Rs.)' "
                        + "FROM boarding b JOIN pet p ON b.pet_id = p.pet_id JOIN kennel k ON b.kennel_id = k.kennel_id "
                        + "WHERE b.status = 'Booked' AND b.check_in_date >= CURDATE() ORDER BY b.check_in_date");
                break;
            case 11:
                ConsoleUtil.showQuery("SELECT k.kennel_id AS 'Kennel ID', k.kennel_name AS 'Kennel', k.status AS 'Kennel Status', "
                        + "COALESCE(p.pet_name, 'Vacant') AS 'Occupied By' FROM kennel k "
                        + "LEFT JOIN boarding b ON b.kennel_id = k.kennel_id AND b.status IN ('Booked','Checked-In') "
                        + "AND b.check_in_date <= ? AND b.check_out_date > ? LEFT JOIN pet p ON b.pet_id = p.pet_id "
                        + "ORDER BY k.kennel_id", date, date);
                break;
            case 12:
                ConsoleUtil.showQuery("SELECT b.boarding_id AS 'Board ID', k.kennel_name AS 'Kennel', b.check_in_date AS 'Check-In', "
                        + "b.check_out_date AS 'Check-Out', b.total_days AS 'Days', b.total_amount AS 'Total (Rs.)', "
                        + "b.status AS 'Status' FROM boarding b JOIN kennel k ON b.kennel_id = k.kennel_id "
                        + "WHERE b.pet_id = ? ORDER BY b.check_in_date DESC", petId);
                break;
            case 13:
                ConsoleUtil.showQuery("SELECT COALESCE(SUM(amount), 0) AS 'Grooming Revenue (Rs.)' FROM payment "
                        + "WHERE appointment_id IS NOT NULL AND payment_status = 'Successful'");
                break;
            case 14:
                ConsoleUtil.showQuery("SELECT COALESCE(SUM(amount), 0) AS 'Boarding Revenue (Rs.)' FROM payment "
                        + "WHERE boarding_id IS NOT NULL AND payment_status = 'Successful'");
                break;
            case 15:
                ConsoleUtil.showQuery("SELECT COALESCE(SUM(CASE WHEN payment_status = 'Successful' THEN amount END), 0) AS 'Net Revenue (Rs.)', "
                        + "COALESCE(SUM(CASE WHEN payment_status = 'Refunded' THEN amount END), 0) AS 'Refunded (Rs.)' FROM payment");
                break;
            case 16:
                ConsoleUtil.showQuery("SELECT c.full_name AS 'Customer', COUNT(py.payment_id) AS 'Payments', "
                        + "SUM(py.amount) AS 'Total Spent (Rs.)' FROM payment py "
                        + "LEFT JOIN appointment a ON py.appointment_id = a.appointment_id "
                        + "LEFT JOIN boarding b ON py.boarding_id = b.boarding_id "
                        + "JOIN pet p ON p.pet_id = COALESCE(a.pet_id, b.pet_id) "
                        + "JOIN customer c ON c.customer_id = p.customer_id WHERE py.payment_status = 'Successful' "
                        + "GROUP BY c.customer_id, c.full_name ORDER BY SUM(py.amount) DESC");
                break;
            default:
                ConsoleUtil.error("Invalid report number.");
        }
    }
}
