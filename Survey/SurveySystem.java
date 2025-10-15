import java.sql.*;
import java.util.*;

public class SurveySystem {
    private static Scanner sc = new Scanner(System.in);
    

    public static void main(String[] args) {
    System.out.println("1. Login");
    System.out.println("2. Register as User");
    System.out.print("Choose: ");
    int option = sc.nextInt(); sc.nextLine();

    if (option == 2) {
        registerUser();
    }

    System.out.print("Enter username: ");
    String username = sc.nextLine();

    System.out.print("Enter password: ");
    String password = sc.nextLine();

    String role = authenticate(username, password);
    if (role != null) {
        System.out.println("✅ Login successful! Welcome " + role + " " + username);

        if (role.equals("ADMIN")) {
            adminMenu(username);
        } else {
            userMenu(username);
        }
    } else {
        System.out.println("❌ Invalid username or password.");
    }
}
    

    // ---------------- AUTHENTICATION ----------------
    private static String authenticate(String username, String password) {
    String sql = "SELECT role FROM users WHERE username=? AND password=?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        // ✅ Add this line to see what credentials are being passed
        System.out.println("Trying to authenticate: " + username + " / " + password);

        ps.setString(1, username);
        ps.setString(2, password);

        ResultSet rs = ps.executeQuery();

        // ✅ Add this to confirm if a match was found
        if (rs.next()) {
            String role = rs.getString("role");
            System.out.println("Match found! Role: " + rs.getString("role"));
            return role;
        } else {
            System.out.println("❌ No matching user found.");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
    return null;
}

private static void registerUser() {
    System.out.print("Choose a username: ");
    String username = sc.nextLine();
    System.out.print("Choose a password: ");
    String password = sc.nextLine();

    try (Connection conn = DBConnection.getConnection()) {
        PreparedStatement ps = conn.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, 'USER')");
        ps.setString(1, username);
        ps.setString(2, password);
        ps.executeUpdate();
        System.out.println("✅ Registration successful! You can now log in.");
    } catch (Exception e) {
        System.out.println("❌ Registration failed. Username may already exist.");
    }
}

    // ---------------- ADMIN MENU ----------------
   private static void adminMenu(String username) {
    while (true) {
        System.out.println("\n=== ADMIN MENU ===");
        System.out.println("1. Create Survey");
        System.out.println("2. View Surveys");
        System.out.println("3. View Survey Results");
        System.out.println("4. Export Survey Results to File");
        System.out.println("5. Logout");
        System.out.print("Choose: ");
        int choice = sc.nextInt(); sc.nextLine();

        switch (choice) {
            case 1 -> createSurvey(username);
            case 2 -> viewSurveys();
            case 3 -> viewSurveyResults();
            case 4 -> exportSurveyResults();   // 🔹 New option
            case 5 -> { System.out.println("Logging out..."); return; }
            default -> System.out.println("Invalid choice!");
        }
    }
}

    private static void createSurvey(String username) {
        System.out.print("Enter survey title: ");
        String title = sc.nextLine();

        try (Connection conn = DBConnection.getConnection()) {
            // Get admin ID
            PreparedStatement ps1 = conn.prepareStatement("SELECT user_id FROM users WHERE username=?");
            ps1.setString(1, username);
            ResultSet rs1 = ps1.executeQuery();
            rs1.next();
            int adminId = rs1.getInt("user_id");

            // Insert survey
            PreparedStatement ps2 = conn.prepareStatement("INSERT INTO surveys (title, created_by) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps2.setString(1, title);
            ps2.setInt(2, adminId);
            ps2.executeUpdate();
            ResultSet keys = ps2.getGeneratedKeys();
            keys.next();
            int surveyId = keys.getInt(1);

            // Add questions
            System.out.print("How many questions? ");
            int qCount = sc.nextInt(); sc.nextLine();
            for (int i = 1; i <= qCount; i++) {
                System.out.print("Enter question " + i + ": ");
                String qText = sc.nextLine();
                PreparedStatement ps3 = conn.prepareStatement("INSERT INTO questions (survey_id, question_text) VALUES (?, ?)");
                ps3.setInt(1, surveyId);
                ps3.setString(2, qText);
                ps3.executeUpdate();
            }
            System.out.println("✅ Survey created successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void viewSurveys() {
        try (Connection conn = DBConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM surveys");
            System.out.println("\n--- Surveys ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("survey_id") + ", Title: " + rs.getString("title"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void viewSurveyResults() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT u.username, s.title, q.question_text, r.answer " +
                         "FROM responses r " +
                         "JOIN users u ON r.user_id=u.user_id " +
                         "JOIN surveys s ON r.survey_id=s.survey_id " +
                         "JOIN questions q ON r.question_id=q.question_id " +
                         "ORDER BY s.title, u.username";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n=== Survey Results ===");
            while (rs.next()) {
                System.out.println("User: " + rs.getString("username"));
                System.out.println("Survey: " + rs.getString("title"));
                System.out.println(rs.getString("question_text") + " → " + rs.getString("answer"));
                System.out.println("----");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

private static void exportSurveyResults() {
    String fileName = "survey_results.txt";
    String sql = "SELECT u.username, s.title, q.question_text, r.answer " +
                 "FROM responses r " +
                 "JOIN users u ON r.user_id=u.user_id " +
                 "JOIN surveys s ON r.survey_id=s.survey_id " +
                 "JOIN questions q ON r.question_id=q.question_id " +
                 "ORDER BY s.title, u.username";

    try (Connection conn = DBConnection.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql);
         java.io.FileWriter writer = new java.io.FileWriter(fileName)) {

        writer.write("=== Survey Results ===\n\n");

        while (rs.next()) {
            writer.write("User: " + rs.getString("username") + "\n");
            writer.write("Survey: " + rs.getString("title") + "\n");
            writer.write(rs.getString("question_text") + " → " + rs.getString("answer") + "\n");
            writer.write("-----------------------------\n");
        }

        System.out.println("✅ Survey results exported successfully to " + fileName);

    } catch (Exception e) {
        System.out.println("❌ Error exporting results: " + e.getMessage());
    }
}


    // ---------------- USER MENU ----------------
    private static void userMenu(String username) {
        while (true) {
            System.out.println("\n=== USER MENU ===");
            System.out.println("1. Take Survey");
            System.out.println("2. Logout");
            System.out.print("Choose: ");
            int choice = sc.nextInt(); sc.nextLine();

            switch (choice) {
                case 1 -> takeSurvey(username);
                case 2 -> { System.out.println("Logging out..."); return; }
                default -> System.out.println("Invalid choice!");
            }
        }
    }

    private static void takeSurvey(String username) {
        try (Connection conn = DBConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM surveys");

            System.out.println("\n--- Available Surveys ---");
            while (rs.next()) {
                System.out.println(rs.getInt("survey_id") + ". " + rs.getString("title"));
            }

            System.out.print("Enter survey ID: ");
            int surveyId = sc.nextInt(); sc.nextLine();

            // Get user ID
            PreparedStatement ps1 = conn.prepareStatement("SELECT user_id FROM users WHERE username=?");
            ps1.setString(1, username);
            ResultSet rs1 = ps1.executeQuery();
            rs1.next();
            int userId = rs1.getInt("user_id");

            // Get questions
            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM questions WHERE survey_id=?");
            ps2.setInt(1, surveyId);
            ResultSet rs2 = ps2.executeQuery();

            while (rs2.next()) {
                int qid = rs2.getInt("question_id");
                String qText = rs2.getString("question_text");

                System.out.print(qText + " → ");
                String answer = sc.nextLine();

                PreparedStatement ps3 = conn.prepareStatement("INSERT INTO responses (survey_id, question_id, user_id, answer) VALUES (?, ?, ?, ?)");
                ps3.setInt(1, surveyId);
                ps3.setInt(2, qid);
                ps3.setInt(3, userId);
                ps3.setString(4, answer);
                ps3.executeUpdate();
            }

            System.out.println("✅ Survey completed!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

