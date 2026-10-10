package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import util.ConsoleUtil;

/** Simulated payments. record() and refund() reuse the caller's Connection so they join its transaction. */
public class PaymentDAO {

    public static String generateTxnRef() {
        return "TXN-" + System.currentTimeMillis() + (int) (Math.random() * 900 + 100);
    }

    /** Inserts a payment row for either an appointment or a boarding (the other id is null). Returns the txn reference. */
    public static String record(Connection con, Integer appointmentId, Integer boardingId,
                                double amount, String method, String status) throws SQLException {
        String ref = generateTxnRef();
        String sql = "INSERT INTO payment (appointment_id, boarding_id, payment_date, amount, payment_method, "
                + "payment_status, transaction_reference) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (appointmentId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, appointmentId);
            if (boardingId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, boardingId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setDouble(4, amount);
            ps.setString(5, method);
            ps.setString(6, status);
            ps.setString(7, ref);
            ps.executeUpdate();
        }
        return ref;
    }

    /** Marks the successful payment of a booking as 'Refunded'. */
    public static void refund(Connection con, Integer appointmentId, Integer boardingId) throws SQLException {
        String sql = appointmentId != null
                ? "UPDATE payment SET payment_status = 'Refunded' WHERE appointment_id = ? AND payment_status = 'Successful'"
                : "UPDATE payment SET payment_status = 'Refunded' WHERE boarding_id = ? AND payment_status = 'Successful'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, appointmentId != null ? appointmentId : boardingId);
            ps.executeUpdate();
        }
    }

    private static final String BASE =
            "SELECT p.payment_id AS 'Pay ID', "
          + "CASE WHEN p.appointment_id IS NOT NULL THEN 'Grooming' ELSE 'Boarding' END AS 'Type', "
          + "COALESCE(p.appointment_id, p.boarding_id) AS 'Booking ID', c.full_name AS 'Customer', "
          + "p.amount AS 'Amount (Rs.)', p.payment_method AS 'Method', p.payment_status AS 'Status', "
          + "p.transaction_reference AS 'Txn Ref', p.payment_date AS 'Date' "
          + "FROM payment p "
          + "LEFT JOIN appointment a ON p.appointment_id = a.appointment_id "
          + "LEFT JOIN boarding b ON p.boarding_id = b.boarding_id "
          + "LEFT JOIN pet pt ON pt.pet_id = COALESCE(a.pet_id, b.pet_id) "
          + "LEFT JOIN customer c ON c.customer_id = pt.customer_id ";

    public static void viewAll() {
        ConsoleUtil.showQuery(BASE + "ORDER BY p.payment_id DESC");
    }

    public static void viewByCustomer(int customerId) {
        ConsoleUtil.showQuery(BASE + "WHERE c.customer_id = ? ORDER BY p.payment_id DESC", customerId);
    }
}
