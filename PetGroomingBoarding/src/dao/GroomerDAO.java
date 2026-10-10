package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.Groomer;
import util.ConsoleUtil;
import util.DBConnection;

/** Groomer (staff) management. */
public class GroomerDAO {

    public static boolean add(Groomer g) {
        String sql = "INSERT INTO groomer (full_name, specialization, phone, status) VALUES (?,?,?,'Active')";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, g.getFullName());
            ps.setString(2, g.getSpecialization());
            ps.setString(3, g.getPhone());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }

    public static Groomer getById(int id) {
        String sql = "SELECT * FROM groomer WHERE groomer_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Groomer(rs.getInt("groomer_id"), rs.getString("full_name"),
                        rs.getString("specialization"), rs.getString("phone"), rs.getString("status")) : null;
            }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return null;
        }
    }

    public static void viewAll() {
        ConsoleUtil.showQuery("SELECT groomer_id AS 'Groomer ID', full_name AS 'Name', specialization AS 'Specialization', "
                + "phone AS 'Phone', status AS 'Status' FROM groomer ORDER BY groomer_id");
    }

    public static boolean toggleStatus(int id) {
        String sql = "UPDATE groomer SET status = IF(status = 'Active', 'Inactive', 'Active') WHERE groomer_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }

    public static boolean delete(int id) {
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM appointment WHERE groomer_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        ConsoleUtil.error("Cannot delete: this groomer has appointment history. Mark as 'Inactive' instead.");
                        return false;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM groomer WHERE groomer_id = ?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }
}
