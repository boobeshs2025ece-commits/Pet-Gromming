package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import model.Boarding;
import exception.BookingException;
import util.ConsoleUtil;
import util.DBConnection;

/**
 * Boarding (pet hotel) bookings.
 * Key rules: nights = check-out minus check-in, kennel must suit the species and be free for the whole
 * stay, stays of 7+ nights get 10% off, booking + payment happen in ONE JDBC transaction.
 */
public class BoardingDAO {
    public static final int LONG_STAY_DAYS = 7;
    public static final double LONG_STAY_DISCOUNT = 0.10;

    public static int calcDays(LocalDate in, LocalDate out) {
        return (int) ChronoUnit.DAYS.between(in, out);
    }

    public static double calcTotal(int days, double dailyRate) {
        double total = days * dailyRate;
        if (days >= LONG_STAY_DAYS) total *= (1 - LONG_STAY_DISCOUNT);
        return Math.round(total * 100.0) / 100.0;
    }

    private static boolean hasOverlap(Connection con, String column, int id, LocalDate in, LocalDate out) throws SQLException {
        String sql = "SELECT COUNT(*) FROM boarding WHERE " + column + " = ? AND status IN ('Booked','Checked-In') "
                + "AND check_in_date < ? AND check_out_date > ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setDate(2, Date.valueOf(out));
            ps.setDate(3, Date.valueOf(in));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        }
    }

    /**
     * Books a boarding stay and records the payment atomically.
     * Caller sets petId, kennelId, checkInDate, checkOutDate, specialInstructions.
     * Days, rate and total are calculated here from the kennel's real rate.
     * @return new boarding id
     */
    public static int book(Boarding b, String method, boolean paymentOk) throws BookingException {
        if (b.getCheckInDate().isBefore(LocalDate.now())) throw new BookingException("Check-in date cannot be in the past.");
        if (!b.getCheckOutDate().isAfter(b.getCheckInDate())) throw new BookingException("Check-out must be after check-in.");

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            double rate;
            try (PreparedStatement ps = con.prepareStatement("SELECT status, pet_type, daily_rate FROM kennel WHERE kennel_id = ? FOR UPDATE")) {
                ps.setInt(1, b.getKennelId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new BookingException("Kennel not found.");
                    if (!"Available".equals(rs.getString("status"))) throw new BookingException("This kennel is under maintenance.");
                    String kennelFor = rs.getString("pet_type");
                    rate = rs.getDouble("daily_rate");
                    String species = null;
                    try (PreparedStatement ps2 = con.prepareStatement("SELECT species FROM pet WHERE pet_id = ?")) {
                        ps2.setInt(1, b.getPetId());
                        try (ResultSet r2 = ps2.executeQuery()) { if (r2.next()) species = r2.getString(1); }
                    }
                    if (species == null) throw new BookingException("Pet not found.");
                    if (!"Any".equals(kennelFor) && !kennelFor.equals(species))
                        throw new BookingException("This kennel is only for " + kennelFor + "s.");
                }
            }
            if (hasOverlap(con, "kennel_id", b.getKennelId(), b.getCheckInDate(), b.getCheckOutDate()))
                throw new BookingException("Kennel is already booked for some of these dates. Choose another kennel or dates.");
            if (hasOverlap(con, "pet_id", b.getPetId(), b.getCheckInDate(), b.getCheckOutDate()))
                throw new BookingException("This pet already has a boarding stay overlapping these dates.");
            if (!paymentOk)
                throw new BookingException("Payment declined. Boarding was NOT booked and no money was charged.");

            int days = calcDays(b.getCheckInDate(), b.getCheckOutDate());
            double total = calcTotal(days, rate);
            b.setTotalDays(days);
            b.setDailyRate(rate);
            b.setTotalAmount(total);

            int id;
            String sql = "INSERT INTO boarding (pet_id, kennel_id, check_in_date, check_out_date, total_days, daily_rate, "
                    + "total_amount, status, special_instructions, booked_on) VALUES (?,?,?,?,?,?,?,'Booked',?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, b.getPetId());
                ps.setInt(2, b.getKennelId());
                ps.setDate(3, Date.valueOf(b.getCheckInDate()));
                ps.setDate(4, Date.valueOf(b.getCheckOutDate()));
                ps.setInt(5, days);
                ps.setDouble(6, rate);
                ps.setDouble(7, total);
                ps.setString(8, b.getSpecialInstructions());
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    if (!k.next()) throw new BookingException("Could not create boarding booking.");
                    id = k.getInt(1);
                }
            }
            PaymentDAO.record(con, null, id, total, method, "Successful");
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
     * Cancels a 'Booked' stay and refunds the payment (one transaction).
     * @param customerId owner's id for customer cancellation (must be before check-in day), null for admin
     * @return refunded amount
     */
    public static double cancel(int boardingId, Integer customerId) throws BookingException {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            String sql = "SELECT b.status, b.total_amount, b.check_in_date, p.customer_id FROM boarding b "
                    + "JOIN pet p ON b.pet_id = p.pet_id WHERE b.boarding_id = ? FOR UPDATE";
            double amount;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, boardingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new BookingException("Boarding ID " + boardingId + " not found.");
                    if (customerId != null && rs.getInt("customer_id") != customerId)
                        throw new BookingException("This boarding booking does not belong to you.");
                    if (!"Booked".equals(rs.getString("status")))
                        throw new BookingException("Only 'Booked' stays can be cancelled (current status: " + rs.getString("status") + ").");
                    if (customerId != null && !rs.getDate("check_in_date").toLocalDate().isAfter(LocalDate.now()))
                        throw new BookingException("Cancellation is allowed only until the day before check-in.");
                    amount = rs.getDouble("total_amount");
                }
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE boarding SET status = 'Cancelled' WHERE boarding_id = ?")) {
                ps.setInt(1, boardingId);
                ps.executeUpdate();
            }
            PaymentDAO.refund(con, null, boardingId);
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

    /** Admin: guest arrives. Allowed only for 'Booked' stays whose check-in date has arrived. */
    public static boolean checkIn(int boardingId) {
        return exec("UPDATE boarding SET status = 'Checked-In' WHERE boarding_id = ? AND status = 'Booked' AND check_in_date <= CURDATE()", boardingId);
    }

    /** Admin: guest leaves. Allowed only for 'Checked-In' stays. */
    public static boolean checkOut(int boardingId) {
        return exec("UPDATE boarding SET status = 'Checked-Out' WHERE boarding_id = ? AND status = 'Checked-In'", boardingId);
    }

    private static boolean exec(String sql, int id) {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }

    private static final String VIEW_BASE =
            "SELECT b.boarding_id AS 'Board ID', p.pet_name AS 'Pet', c.full_name AS 'Owner', k.kennel_name AS 'Kennel', "
          + "b.check_in_date AS 'Check-In', b.check_out_date AS 'Check-Out', b.total_days AS 'Days', "
          + "b.total_amount AS 'Total (Rs.)', b.status AS 'Status' FROM boarding b "
          + "JOIN pet p ON b.pet_id = p.pet_id JOIN customer c ON p.customer_id = c.customer_id "
          + "JOIN kennel k ON b.kennel_id = k.kennel_id ";

    public static void viewAll() {
        ConsoleUtil.showQuery(VIEW_BASE + "ORDER BY b.check_in_date DESC");
    }

    public static void viewCurrentGuests() {
        ConsoleUtil.showQuery(VIEW_BASE + "WHERE b.status = 'Checked-In' ORDER BY b.check_out_date");
    }

    public static void viewByCustomer(int customerId) {
        ConsoleUtil.showQuery(VIEW_BASE + "WHERE c.customer_id = ? ORDER BY b.check_in_date DESC", customerId);
    }

    public static void printReceipt(int boardingId) {
        ConsoleUtil.showQuery("SELECT b.boarding_id AS 'Board ID', p.pet_name AS 'Pet', k.kennel_name AS 'Kennel', "
                + "b.check_in_date AS 'Check-In', b.check_out_date AS 'Check-Out', b.total_days AS 'Days', "
                + "b.daily_rate AS 'Rate/Day', b.total_amount AS 'Total (Rs.)', py.payment_method AS 'Method', "
                + "py.payment_status AS 'Payment', py.transaction_reference AS 'Txn Ref' FROM boarding b "
                + "JOIN pet p ON b.pet_id = p.pet_id JOIN kennel k ON b.kennel_id = k.kennel_id "
                + "JOIN payment py ON py.boarding_id = b.boarding_id WHERE b.boarding_id = ?", boardingId);
    }
}
