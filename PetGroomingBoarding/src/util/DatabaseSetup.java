package util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
* Creates the database, all 9 tables and sample data automatically (idempotent).
* The same SQL is available in database.sql for manual import in MySQL Workbench.
*/
public class DatabaseSetup {

    private static final String[] TABLES = {
        "CREATE TABLE IF NOT EXISTS admin ( username VARCHAR(50) PRIMARY KEY, password VARCHAR(100) NOT NULL )",
        "CREATE TABLE IF NOT EXISTS customer ( customer_id INT AUTO_INCREMENT PRIMARY KEY, full_name VARCHAR(100) NOT NULL, email VARCHAR(100) NOT NULL UNIQUE, phone VARCHAR(20) NOT NULL, address VARCHAR(200), password_hash VARCHAR(128) NOT NULL, registration_date DATE NOT NULL )",
        "CREATE TABLE IF NOT EXISTS pet ( pet_id INT AUTO_INCREMENT PRIMARY KEY, customer_id INT NOT NULL, pet_name VARCHAR(50) NOT NULL, species VARCHAR(20) NOT NULL, breed VARCHAR(50), age INT NOT NULL DEFAULT 0, gender VARCHAR(10) NOT NULL, weight_kg DOUBLE NOT NULL, medical_notes VARCHAR(255), status VARCHAR(20) NOT NULL DEFAULT 'Active', FOREIGN KEY (customer_id) REFERENCES customer(customer_id) )",
        "CREATE TABLE IF NOT EXISTS service ( service_id INT AUTO_INCREMENT PRIMARY KEY, service_name VARCHAR(80) NOT NULL UNIQUE, description VARCHAR(255), price DOUBLE NOT NULL, duration_minutes INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'Active' )",
        "CREATE TABLE IF NOT EXISTS groomer ( groomer_id INT AUTO_INCREMENT PRIMARY KEY, full_name VARCHAR(100) NOT NULL, specialization VARCHAR(80), phone VARCHAR(20), status VARCHAR(20) NOT NULL DEFAULT 'Active' )",
        "CREATE TABLE IF NOT EXISTS kennel ( kennel_id INT AUTO_INCREMENT PRIMARY KEY, kennel_name VARCHAR(50) NOT NULL UNIQUE, kennel_type VARCHAR(20) NOT NULL, pet_type VARCHAR(20) NOT NULL DEFAULT 'Any', daily_rate DOUBLE NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'Available' )",
        "CREATE TABLE IF NOT EXISTS appointment ( appointment_id INT AUTO_INCREMENT PRIMARY KEY, pet_id INT NOT NULL, service_id INT NOT NULL, groomer_id INT NOT NULL, appointment_date DATE NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL, price DOUBLE NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'Booked', booked_on DATETIME NOT NULL, FOREIGN KEY (pet_id) REFERENCES pet(pet_id), FOREIGN KEY (service_id) REFERENCES service(service_id), FOREIGN KEY (groomer_id) REFERENCES groomer(groomer_id) )",
        "CREATE TABLE IF NOT EXISTS boarding ( boarding_id INT AUTO_INCREMENT PRIMARY KEY, pet_id INT NOT NULL, kennel_id INT NOT NULL, check_in_date DATE NOT NULL, check_out_date DATE NOT NULL, total_days INT NOT NULL, daily_rate DOUBLE NOT NULL, total_amount DOUBLE NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'Booked', special_instructions VARCHAR(255), booked_on DATETIME NOT NULL, FOREIGN KEY (pet_id) REFERENCES pet(pet_id), FOREIGN KEY (kennel_id) REFERENCES kennel(kennel_id) )",
        "CREATE TABLE IF NOT EXISTS payment ( payment_id INT AUTO_INCREMENT PRIMARY KEY, appointment_id INT NULL, boarding_id INT NULL, payment_date DATETIME NOT NULL, amount DOUBLE NOT NULL, payment_method VARCHAR(20) NOT NULL, payment_status VARCHAR(20) NOT NULL, transaction_reference VARCHAR(50) NOT NULL UNIQUE, FOREIGN KEY (appointment_id) REFERENCES appointment(appointment_id) ON DELETE CASCADE, FOREIGN KEY (boarding_id) REFERENCES boarding(boarding_id) ON DELETE CASCADE )",
    };

    private static final String[] SEED = {
        "INSERT IGNORE INTO admin (username, password) VALUES ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9')",
        "INSERT IGNORE INTO customer (customer_id, full_name, email, phone, address, password_hash, registration_date) VALUES (1, 'Ravi Kumar', 'ravi.kumar@example.com', '9876543210', '12 Gandhi Road, Tiruppur', '4f21b18a4c743a5da01bb3a4955dea0a0294a0b4f7977b454c7259e37b2e6c19', CURDATE()), (2, 'Priya Sharma', 'priya.sharma@example.com', '9123456789', '45 Anna Nagar, Coimbatore', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', CURDATE())",
        "INSERT IGNORE INTO pet (pet_id, customer_id, pet_name, species, breed, age, gender, weight_kg, medical_notes, status) VALUES (1, 1, 'Bruno', 'Dog', 'Labrador Retriever', 4, 'Male', 28.5, 'Vaccinated; sensitive to strong shampoos', 'Active'), (2, 1, 'Coco', 'Dog', 'Pomeranian', 2, 'Female', 3.2, 'None', 'Active'), (3, 2, 'Whiskers', 'Cat', 'Persian', 3, 'Female', 4.1, 'Needs regular coat brushing', 'Active')",
        "INSERT IGNORE INTO service (service_id, service_name, description, price, duration_minutes, status) VALUES (1, 'Basic Bath', 'Bath, blow-dry and ear cleaning', 500.00, 45, 'Active'), (2, 'Full Grooming', 'Bath, haircut, nail trim, ear cleaning and styling', 1200.00, 90, 'Active'), (3, 'Haircut and Styling', 'Breed-specific haircut and styling', 800.00, 60, 'Active'), (4, 'Nail Trimming', 'Nail clipping and filing', 200.00, 20, 'Active'), (5, 'Teeth Cleaning', 'Dental scaling and fresh-breath treatment', 350.00, 30, 'Active'), (6, 'De-shedding Treatment', 'Special shampoo and brushing to reduce shedding', 900.00, 60, 'Active'), (7, 'Flea and Tick Treatment', 'Medicated bath to remove fleas and ticks', 700.00, 45, 'Active')",
        "INSERT IGNORE INTO groomer (groomer_id, full_name, specialization, phone, status) VALUES (1, 'Anitha Raj', 'Dog styling and haircuts', '9000011111', 'Active'), (2, 'Karthik M', 'Cat grooming and de-shedding', '9000022222', 'Active'), (3, 'Suresh Babu', 'Large breed bathing', '9000033333', 'Active')",
        "INSERT IGNORE INTO kennel (kennel_id, kennel_name, kennel_type, pet_type, daily_rate, status) VALUES (1, 'K1 - Dog Standard', 'Standard', 'Dog', 400.00, 'Available'), (2, 'K2 - Dog Standard', 'Standard', 'Dog', 400.00, 'Available'), (3, 'K3 - Dog Deluxe', 'Deluxe', 'Dog', 700.00, 'Available'), (4, 'C1 - Cat Condo', 'Standard', 'Cat', 300.00, 'Available'), (5, 'C2 - Cat Condo', 'Standard', 'Cat', 300.00, 'Available'), (6, 'S1 - Luxury Suite', 'Suite', 'Any', 1200.00, 'Available')",
        "INSERT IGNORE INTO appointment (appointment_id, pet_id, service_id, groomer_id, appointment_date, start_time, end_time, price, status, booked_on) VALUES (1, 1, 2, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00', '11:40:00', 1200.00, 'Booked', NOW())",
        "INSERT IGNORE INTO boarding (boarding_id, pet_id, kennel_id, check_in_date, check_out_date, total_days, daily_rate, total_amount, status, special_instructions, booked_on) VALUES (1, 3, 4, DATE_ADD(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 3, 300.00, 900.00, 'Booked', 'Feed twice a day; prefers a quiet corner', NOW())",
        "INSERT IGNORE INTO payment (payment_id, appointment_id, boarding_id, payment_date, amount, payment_method, payment_status, transaction_reference) VALUES (1, 1, NULL, NOW(), 1200.00, 'UPI', 'Successful', 'TXN-DEMO-10001'), (2, NULL, 1, NOW(), 900.00, 'Card', 'Successful', 'TXN-DEMO-10002')",
    };

    public static void initialize() throws SQLException {
        try (Connection con = DBConnection.getServerConnection(); Statement st = con.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS " + DBConnection.DB_NAME);
        }
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement()) {
            for (String sql : TABLES) st.execute(sql);
            for (String sql : SEED) st.execute(sql);
        }
    }
}
