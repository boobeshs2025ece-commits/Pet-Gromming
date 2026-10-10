package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import model.Customer;
import util.ConsoleUtil;
import util.DBConnection;
import util.PasswordUtil;

/** Customer (pet owner) registration, login and admin views. */
public class CustomerDAO {

    private static Customer map(ResultSet rs) throws SQLException {
        return new Customer(rs.getInt("customer_id"), rs.getString("full_name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("address"), rs.getString("password_hash"),
                rs.getDate("registration_date").toLocalDate());
    }

    public static boolean emailExists(String email) {
        String sql = "SELECT 1 FROM customer WHERE email = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return true; // be safe: block registration if we cannot verify
        }
    }

    /** The Customer object must already contain the SHA-256 password hash. */
    public static boolean register(Customer c) {
        String sql = "INSERT INTO customer (full_name, email, phone, address, password_hash, registration_date) VALUES (?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getFullName());
            ps.setString(2, c.getEmail());
            ps.setString(3, c.getPhone());
            ps.setString(4, c.getAddress());
            ps.setString(5, c.getPasswordHash());
            ps.setDate(6, Date.valueOf(LocalDate.now()));
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }

    public static Customer login(String email, String rawPassword) {
        String sql = "SELECT * FROM customer WHERE email = ? AND password_hash = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, PasswordUtil.hash(rawPassword));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return null;
        }
    }

    public static Customer getById(int id) {
        String sql = "SELECT * FROM customer WHERE customer_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return null;
        }
    }

    public static void viewAll() {
        ConsoleUtil.showQuery("SELECT customer_id AS 'Cust ID', full_name AS 'Name', email AS 'Email', phone AS 'Phone', "
                + "address AS 'Address', registration_date AS 'Registered' FROM customer ORDER BY customer_id");
    }

    public static void searchByName(String keyword) {
        ConsoleUtil.showQuery("SELECT customer_id AS 'Cust ID', full_name AS 'Name', email AS 'Email', phone AS 'Phone', "
                + "address AS 'Address' FROM customer WHERE full_name LIKE ? ORDER BY full_name", "%" + keyword + "%");
    }

    /** Delete is blocked when the customer owns any pets (history must be preserved). */
    public static boolean delete(int id) {
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM pet WHERE customer_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        ConsoleUtil.error("Cannot delete: this customer has registered pets (booking history is preserved).");
                        return false;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM customer WHERE customer_id = ?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }
}
