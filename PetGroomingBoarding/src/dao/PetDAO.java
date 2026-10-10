package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import model.Pet;
import util.ConsoleUtil;
import util.DBConnection;

/** Pet profiles. Pets are soft-deleted (status = 'Inactive') so booking history is never lost. */
public class PetDAO {

    private static Pet map(ResultSet rs) throws SQLException {
        return new Pet(rs.getInt("pet_id"), rs.getInt("customer_id"), rs.getString("pet_name"),
                rs.getString("species"), rs.getString("breed"), rs.getInt("age"), rs.getString("gender"),
                rs.getDouble("weight_kg"), rs.getString("medical_notes"), rs.getString("status"));
    }

    /** Returns the new pet id, or -1 on failure. */
    public static int add(Pet p) {
        String sql = "INSERT INTO pet (customer_id, pet_name, species, breed, age, gender, weight_kg, medical_notes, status) "
                + "VALUES (?,?,?,?,?,?,?,?,'Active')";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getCustomerId());
            ps.setString(2, p.getPetName());
            ps.setString(3, p.getSpecies());
            ps.setString(4, p.getBreed());
            ps.setInt(5, p.getAge());
            ps.setString(6, p.getGender());
            ps.setDouble(7, p.getWeightKg());
            ps.setString(8, p.getMedicalNotes());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) return k.getInt(1); }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
        }
        return -1;
    }

    public static Pet getById(int petId) {
        String sql = "SELECT * FROM pet WHERE pet_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return null;
        }
    }

    /** Prints the customer's active pets; returns how many were found. */
    public static int viewByCustomer(int customerId) {
        return ConsoleUtil.showQuery("SELECT pet_id AS 'Pet ID', pet_name AS 'Name', species AS 'Species', breed AS 'Breed', "
                + "age AS 'Age (yrs)', gender AS 'Gender', weight_kg AS 'Weight (kg)', medical_notes AS 'Medical Notes' "
                + "FROM pet WHERE customer_id = ? AND status = 'Active' ORDER BY pet_id", customerId);
    }

    public static void viewAll() {
        ConsoleUtil.showQuery("SELECT p.pet_id AS 'Pet ID', p.pet_name AS 'Name', p.species AS 'Species', p.breed AS 'Breed', "
                + "p.age AS 'Age', p.weight_kg AS 'Weight (kg)', c.full_name AS 'Owner', p.status AS 'Status' "
                + "FROM pet p JOIN customer c ON p.customer_id = c.customer_id ORDER BY p.pet_id");
    }

    public static boolean update(int petId, int age, double weight, String notes) {
        String sql = "UPDATE pet SET age = ?, weight_kg = ?, medical_notes = ? WHERE pet_id = ? AND status = 'Active'";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, age);
            ps.setDouble(2, weight);
            ps.setString(3, notes);
            ps.setInt(4, petId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }

    /** Soft delete - refused while the pet has an active grooming appointment or boarding stay. */
    public static boolean remove(int petId) {
        String check = "SELECT (SELECT COUNT(*) FROM appointment WHERE pet_id = ? AND status = 'Booked') "
                + "+ (SELECT COUNT(*) FROM boarding WHERE pet_id = ? AND status IN ('Booked','Checked-In'))";
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(check)) {
                ps.setInt(1, petId);
                ps.setInt(2, petId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        ConsoleUtil.error("Cannot remove: this pet has active appointments or boarding. Cancel/complete them first.");
                        return false;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE pet SET status = 'Inactive' WHERE pet_id = ?")) {
                ps.setInt(1, petId);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }
}
