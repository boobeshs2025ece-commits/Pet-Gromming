package app;

import java.sql.SQLException;
import model.Customer;
import dao.AdminDAO;
import dao.CustomerDAO;
import util.ConsoleUtil;
import util.DatabaseSetup;
import util.InputUtil;
import util.PasswordUtil;

/** Entry point: initialises the database, then shows the main menu (Admin / Customer / Register). */
public class Main {

    public static void main(String[] args) {
        System.out.println("+----------------------------------------------------------+");
        System.out.println("|     PET GROOMING AND BOARDING MANAGEMENT SYSTEM          |");
        System.out.println("|            Core Java + JDBC + MySQL                      |");
        System.out.println("+----------------------------------------------------------+");

        try {
            System.out.println("Connecting to MySQL and preparing database...");
            DatabaseSetup.initialize();
            ConsoleUtil.success("Database 'pet_care_db' is ready.");
        } catch (SQLException e) {
            ConsoleUtil.error("Could not initialise the database: " + e.getMessage());
            System.out.println("  Checklist: 1) MySQL service is running  2) db.properties has the right user/password");
            System.out.println("             3) mysql-connector-j-8.0.33.jar is in lib/ and on the Eclipse Build Path");
            return;
        }

        boolean running = true;
        while (running) {
            ConsoleUtil.header("MAIN MENU");
            System.out.println("   1. Admin Login");
            System.out.println("   2. Customer Login");
            System.out.println("   3. New Customer Registration");
            System.out.println("   4. Exit");
            int choice = InputUtil.readInt("  Enter choice: ", 1, 4);
            switch (choice) {
                case 1: adminLogin(); break;
                case 2: customerLogin(); break;
                case 3: registerCustomer(); break;
                default:
                    running = false;
                    System.out.println("\nThank you for using the Pet Grooming and Boarding System. Goodbye!");
            }
        }
    }

    private static void adminLogin() {
        ConsoleUtil.header("ADMIN LOGIN");
        for (int attempt = 1; attempt <= 3; attempt++) {
            String user = InputUtil.readString("  Username: ", 50);
            String pass = InputUtil.readString("  Password: ", 100);
            if (AdminDAO.login(user, pass)) {
                ConsoleUtil.success("Welcome, " + user + "!");
                AdminMenu.start();
                return;
            }
            ConsoleUtil.error("Invalid credentials. Attempt " + attempt + " of 3.");
        }
        ConsoleUtil.error("Too many failed attempts. Returning to main menu.");
    }

    private static void customerLogin() {
        ConsoleUtil.header("CUSTOMER LOGIN");
        for (int attempt = 1; attempt <= 3; attempt++) {
            String email = InputUtil.readString("  Email: ", 100).toLowerCase();
            String pass = InputUtil.readString("  Password: ", 100);
            Customer c = CustomerDAO.login(email, pass);
            if (c != null) {
                ConsoleUtil.success("Welcome back, " + c.getFullName() + "!");
                CustomerMenu.start(c);
                return;
            }
            ConsoleUtil.error("Invalid email or password. Attempt " + attempt + " of 3.");
        }
        ConsoleUtil.error("Too many failed attempts. Returning to main menu.");
    }

    private static void registerCustomer() {
        ConsoleUtil.header("NEW CUSTOMER REGISTRATION");
        String name = InputUtil.readString("  Full name: ", 100);
        String email;
        while (true) {
            email = InputUtil.readEmail("  Email: ");
            if (!CustomerDAO.emailExists(email)) break;
            ConsoleUtil.error("This email is already registered. Use another email or log in.");
        }
        String phone = InputUtil.readPhone("  Phone (10 digits): ");
        String address = InputUtil.readOptional("  Address (optional): ", 200);
        String pass;
        while (true) {
            pass = InputUtil.readString("  Password (min 6 characters): ", 100);
            if (pass.length() < 6) { ConsoleUtil.error("Password must be at least 6 characters."); continue; }
            String again = InputUtil.readString("  Confirm password: ", 100);
            if (pass.equals(again)) break;
            ConsoleUtil.error("Passwords do not match. Try again.");
        }
        Customer c = new Customer();
        c.setFullName(name);
        c.setEmail(email);
        c.setPhone(phone);
        c.setAddress(address);
        c.setPasswordHash(PasswordUtil.hash(pass));
        if (CustomerDAO.register(c)) ConsoleUtil.success("Registration successful! You can now log in with your email.");
        else ConsoleUtil.error("Registration failed.");
    }
}
