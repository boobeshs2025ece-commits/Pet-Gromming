package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.Service;
import util.ConsoleUtil;
import util.DBConnection;

/** Grooming service catalogue (price list). */
public class ServiceDAO {

    private static Service map(ResultSet rs) throws SQLException {
        return new Service(rs.getInt("service_id"), rs.getString("service_name"), rs.getString("description"),
                rs.getDouble("price"), rs.getInt("duration_minutes"), rs.getString("status"));
    }

    public static boolean add(Service s) {
        String sql = "INSERT INTO service (service_name, description, price, duration_minutes, status) VALUES (?,?,?,?,'Active')";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getServiceName());
            ps.setString(2, s.getDescription());
            ps.setDouble(3, s.getPrice());
            ps.setInt(4, s.getDurationMinutes());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) ConsoleUtil.error("A service with this name already exists.");
            else ConsoleUtil.dbError(e);
            return false;
        }
    }

    public static Service getById(int id) {
        String sql = "SELECT * FROM service WHERE service_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return null;
        }
    }

    public static int viewAll(boolean activeOnly) {
        String sql = "SELECT service_id AS 'Service ID', service_name AS 'Service', description AS 'Description', "
                + "price AS 'Price (Rs.)', duration_minutes AS 'Minutes', status AS 'Status' FROM service "
                + (activeOnly ? "WHERE status = 'Active' " : "") + "ORDER BY service_id";
        return ConsoleUtil.showQuery(sql);
    }

    public static boolean updatePrice(int id, double price) {
        return exec("UPDATE service SET price = ? WHERE service_id = ?", price, id);
    }

    public static boolean toggleStatus(int id) {
        return exec("UPDATE service SET status = IF(status = 'Active', 'Inactive', 'Active') WHERE service_id = ?", id);
    }

    public static boolean delete(int id) {
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM appointment WHERE service_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        ConsoleUtil.error("Cannot delete: appointments exist for this service. Mark it 'Inactive' instead.");
                        return false;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM service WHERE service_id = ?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }

    private static boolean exec(String sql, Object... params) {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ConsoleUtil.bind(ps, params);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }
}
