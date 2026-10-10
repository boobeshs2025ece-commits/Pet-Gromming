package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import model.Appointment;
import model.Groomer;
import exception.BookingException;
import util.ConsoleUtil;
import util.DBConnection;

/**
 * Grooming appointments.
 * Key rules: salon hours 09:00-18:00, end time = duration + 10 min buffer, a groomer (or a pet) can
 * never have two overlapping appointments, booking + payment happen in ONE JDBC transaction.
 */
public class AppointmentDAO {
    public static final LocalTime OPEN = LocalTime.of(9, 0);
    public static final LocalTime CLOSE = LocalTime.of(18, 0);
    public static final int BUFFER_MIN = 10;

    public static LocalTime calcEndTime(LocalTime start, int durationMinutes) {
        return start.plusMinutes(durationMinutes + BUFFER_MIN);
    }

    /** Checks date/time against salon hours and the current time. */
    public static void validateSlot(LocalDate date, LocalTime start, LocalTime end) throws BookingException {
        if (date.isBefore(LocalDate.now())) throw new BookingException("Appointment date cannot be in the past.");
        if (date.equals(LocalDate.now()) && !start.isAfter(LocalTime.now()))
            throw new BookingException("Start time has already passed today. Choose a later time.");
        if (start.isBefore(OPEN)) throw new BookingException("Salon opens at " + OPEN + ".");
        if (end.isBefore(start) || end.isAfter(CLOSE))
            throw new BookingException("Service would end at " + end + " but the salon closes at " + CLOSE + ". Choose an earlier start.");
    }

    /** Active groomers with no overlapping booked appointment in the slot. */
    public static List<Groomer> findFreeGroomers(LocalDate date, LocalTime start, LocalTime end) {
        List<Groomer> list = new ArrayList<>();
        String sql = "SELECT * FROM groomer g WHERE g.status = 'Active' AND NOT EXISTS (SELECT 1 FROM appointment a "
                + "WHERE a.groomer_id = g.groomer_id AND a.appointment_date = ? AND a.status = 'Booked' "
                + "AND a.start_time < ? AND a.end_time > ?) ORDER BY g.groomer_id";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setTime(2, Time.valueOf(end));
            ps.setTime(3, Time.valueOf(start));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Groomer(rs.getInt("groomer_id"), rs.getString("full_name"),
                            rs.getString("specialization"), rs.getString("phone"), rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
        }
        return list;
    }

    private static boolean hasOverlap(Connection con, String column, int id, LocalDate date,
                                      LocalTime start, LocalTime end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointment WHERE " + column + " = ? AND appointment_date = ? "
                + "AND status = 'Booked' AND start_time < ? AND end_time > ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(end));
            ps.setTime(4, Time.valueOf(start));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        }
    }

    /**
     * Books an appointment and records the payment atomically.
     * @param paymentOk result of the simulated payment (false = bank declined, nothing is saved)
     * @return new appointment id
     */
    public static int book(Appointment a, String method, boolean paymentOk) throws BookingException {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            // Lock the groomer row so two customers cannot grab the same slot at the same moment.
            try (PreparedStatement ps = con.prepareStatement("SELECT status FROM groomer WHERE groomer_id = ? FOR UPDATE")) {
                ps.setInt(1, a.getGroomerId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !"Active".equals(rs.getString(1))) throw new BookingException("Selected groomer is not available.");
                }
            }
            if (hasOverlap(con, "groomer_id", a.getGroomerId(), a.getAppointmentDate(), a.getStartTime(), a.getEndTime()))
                throw new BookingException("Slot conflict: the groomer was just booked for this time. Pick another slot.");
            if (hasOverlap(con, "pet_id", a.getPetId(), a.getAppointmentDate(), a.getStartTime(), a.getEndTime()))
                throw new BookingException("This pet already has a grooming appointment that overlaps this time.");
            if (!paymentOk)
                throw new BookingException("Payment declined. Appointment was NOT booked and no money was charged.");

            int id;
            String sql = "INSERT INTO appointment (pet_id, service_id, groomer_id, appointment_date, start_time, end_time, "
                    + "price, status, booked_on) VALUES (?,?,?,?,?,?,?,'Booked',?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, a.getPetId());
                ps.setInt(2, a.getServiceId());
                ps.setInt(3, a.getGroomerId());
                ps.setDate(4, Date.valueOf(a.getAppointmentDate()));
                ps.setTime(5, Time.valueOf(a.getStartTime()));
                ps.setTime(6, Time.valueOf(a.getEndTime()));
                ps.setDouble(7, a.getPrice());
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    if (!k.next()) throw new BookingException("Could not create appointment.");
                    id = k.getInt(1);
                }
            }
            PaymentDAO.record(con, id, null, a.getPrice(), method, "Successful");
            con.commit();
            return id;
        } catch (SQLException e) {
            DBConnection.rollbackQuietly(con);
            throw new BookingException("Database error: " + e.getMessage());
        } catch (BookingException e) {
            DBConnection.rollbackQuietly(con);
            throw e;
        } finally {
            DBConnection.closeQuietly(con);
        }
    }

    /**
     * Cancels a 'Booked' appointment and refunds the payment (one transaction).
     * @param customerId owner's id when a customer cancels (ownership + future-time check), null when admin cancels
     * @return refunded amount
     */
    public static double cancel(int appointmentId, Integer customerId) throws BookingException {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            String sql = "SELECT a.status, a.price, a.appointment_date, a.start_time, p.customer_id FROM appointment a "
                    + "JOIN pet p ON a.pet_id = p.pet_id WHERE a.appointment_id = ? FOR UPDATE";
            double amount;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, appointmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new BookingException("Appointment ID " + appointmentId + " not found.");
                    if (customerId != null && rs.getInt("customer_id") != customerId)
                        throw new BookingException("This appointment does not belong to you.");
                    if (!"Booked".equals(rs.getString("status")))
                        throw new BookingException("Only 'Booked' appointments can be cancelled (current status: " + rs.getString("status") + ").");
                    LocalDateTime when = LocalDateTime.of(rs.getDate("appointment_date").toLocalDate(), rs.getTime("start_time").toLocalTime());
                    if (customerId != null && !when.isAfter(LocalDateTime.now()))
                        throw new BookingException("This appointment time has already started/passed and cannot be cancelled.");
                    amount = rs.getDouble("price");
                }
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE appointment SET status = 'Cancelled' WHERE appointment_id = ?")) {
                ps.setInt(1, appointmentId);
                ps.executeUpdate();
            }
            PaymentDAO.refund(con, appointmentId, null);
            con.commit();
            return amount;
        } catch (SQLException e) {
            DBConnection.rollbackQuietly(con);
            throw new BookingException("Database error: " + e.getMessage());
        } catch (BookingException e) {
            DBConnection.rollbackQuietly(con);
            throw e;
        } finally {
            DBConnection.closeQuietly(con);
        }
    }

    /** Admin: mark a booked appointment (today or earlier) as Completed. */
    public static boolean complete(int appointmentId) {
        String sql = "UPDATE appointment SET status = 'Completed' WHERE appointment_id = ? "
                + "AND status = 'Booked' AND appointment_date <= CURDATE()";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }

    private static final String VIEW_BASE =
            "SELECT a.appointment_id AS 'Appt ID', p.pet_name AS 'Pet', c.full_name AS 'Owner', s.service_name AS 'Service', "
          + "g.full_name AS 'Groomer', a.appointment_date AS 'Date', a.start_time AS 'Start', a.end_time AS 'End', "
          + "a.price AS 'Price (Rs.)', a.status AS 'Status' FROM appointment a "
          + "JOIN pet p ON a.pet_id = p.pet_id JOIN customer c ON p.customer_id = c.customer_id "
          + "JOIN service s ON a.service_id = s.service_id JOIN groomer g ON a.groomer_id = g.groomer_id ";

    public static void viewAll() {
        ConsoleUtil.showQuery(VIEW_BASE + "ORDER BY a.appointment_date DESC, a.start_time");
    }

    public static void viewByDate(LocalDate date) {
        ConsoleUtil.showQuery(VIEW_BASE + "WHERE a.appointment_date = ? ORDER BY a.start_time", date);
    }

    public static void viewByCustomer(int customerId) {
        ConsoleUtil.showQuery(VIEW_BASE + "WHERE c.customer_id = ? ORDER BY a.appointment_date DESC, a.start_time", customerId);
    }

    public static void printReceipt(int appointmentId) {
        ConsoleUtil.showQuery("SELECT a.appointment_id AS 'Appt ID', p.pet_name AS 'Pet', s.service_name AS 'Service', "
                + "g.full_name AS 'Groomer', a.appointment_date AS 'Date', CONCAT(a.start_time, ' - ', a.end_time) AS 'Time', "
                + "a.price AS 'Amount (Rs.)', py.payment_method AS 'Method', py.payment_status AS 'Payment', "
                + "py.transaction_reference AS 'Txn Ref' FROM appointment a "
                + "JOIN pet p ON a.pet_id = p.pet_id JOIN service s ON a.service_id = s.service_id "
                + "JOIN groomer g ON a.groomer_id = g.groomer_id JOIN payment py ON py.appointment_id = a.appointment_id "
                + "WHERE a.appointment_id = ?", appointmentId);
    }
}
