package util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Console input helpers - every method loops until the user enters a valid value. */
public class InputUtil {
    private static final Scanner SC = new Scanner(System.in);

    public static String readLine(String prompt) {
        System.out.print(prompt);
        if (!SC.hasNextLine()) {
            System.out.println("\nInput closed. Exiting.");
            System.exit(0);
        }
        return SC.nextLine().trim();
    }

    /** Required text field. */
    public static String readString(String prompt, int maxLen) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty()) { System.out.println("  [!] This field is required."); continue; }
            if (s.length() > maxLen) { System.out.println("  [!] Maximum " + maxLen + " characters allowed."); continue; }
            return s;
        }
    }

    /** Optional text field (empty allowed). */
    public static String readOptional(String prompt, int maxLen) {
        while (true) {
            String s = readLine(prompt);
            if (s.length() > maxLen) { System.out.println("  [!] Maximum " + maxLen + " characters allowed."); continue; }
            return s;
        }
    }

    public static int readInt(String prompt, int min, int max) {
        while (true) {
            String s = readLine(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) { System.out.println("  [!] Enter a number between " + min + " and " + max + "."); continue; }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid number. Please try again.");
            }
        }
    }

    public static double readDouble(String prompt, double min, double max) {
        while (true) {
            String s = readLine(prompt);
            try {
                double v = Double.parseDouble(s);
                if (v < min || v > max) { System.out.println("  [!] Enter a value between " + min + " and " + max + "."); continue; }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid number. Please try again.");
            }
        }
    }

    /** Date in yyyy-MM-dd format. */
    public static LocalDate readDate(String prompt) {
        while (true) {
            String s = readLine(prompt + " (YYYY-MM-DD): ");
            try { return LocalDate.parse(s); }
            catch (DateTimeParseException e) { System.out.println("  [!] Invalid date. Example: 2026-12-25"); }
        }
    }

    /** Time in 24-hour HH:mm format. */
    public static LocalTime readTime(String prompt) {
        while (true) {
            String s = readLine(prompt + " (HH:mm, 24-hour): ");
            try { return LocalTime.parse(s); }
            catch (DateTimeParseException e) { System.out.println("  [!] Invalid time. Example: 14:30"); }
        }
    }

    public static String readEmail(String prompt) {
        while (true) {
            String s = readString(prompt, 100);
            if (s.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return s.toLowerCase();
            System.out.println("  [!] Invalid email format.");
        }
    }

    public static String readPhone(String prompt) {
        while (true) {
            String s = readString(prompt, 20);
            if (s.matches("\\d{10}")) return s;
            System.out.println("  [!] Phone number must be exactly 10 digits.");
        }
    }

    /** Shows a numbered list and returns the chosen option text. */
    public static String readChoice(String prompt, String... options) {
        System.out.println(prompt);
        for (int i = 0; i < options.length; i++) System.out.println("   " + (i + 1) + ". " + options[i]);
        int c = readInt("  Enter choice: ", 1, options.length);
        return options[c - 1];
    }

    public static boolean confirm(String prompt) {
        while (true) {
            String s = readLine(prompt + " (Y/N): ").toLowerCase();
            if (s.equals("y") || s.equals("yes")) return true;
            if (s.equals("n") || s.equals("no")) return false;
            System.out.println("  [!] Please enter Y or N.");
        }
    }

    public static void pause() {
        readLine("\nPress Enter to continue...");
    }
}
