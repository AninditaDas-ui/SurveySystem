import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/survey_system";
    private static final String USER = "root";   // <-- change if needed
    private static final String PASSWORD = "mysqlgalaxy_20"; // <-- change if needed

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
