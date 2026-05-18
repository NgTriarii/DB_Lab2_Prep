import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmployeeOrderViewer {

    // Should not put credentials in code
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=Northwind";
    private static final String USER = "sa";
    private static final String PASS = "password";

    public static void main(String[] args) {
        
        // We use try-with-resources to ensure the connection is closed automatically
        try (Connection con = DriverManager.getConnection(DB_URL, USER, PASS)) {
            
            System.out.println("Connection to MS SQL Server established successfully.\n");

            displayAllEmployees(con);

            displayEmployeesForFrenchOrders(con);

        } catch (SQLException e) {
            System.err.println("Database connection or execution error:");
            e.printStackTrace();
        }
    }

    private static void displayAllEmployees(Connection con) {
        System.out.println("--- ALL EMPLOYEES ---");
        String query = "SELECT EmployeeID, FirstName, LastName, Title FROM Employees";
        
        try (PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
             
            while (rs.next()) {
                System.out.printf("ID: %d | Name: %s %s | Title: %s%n", 
                        rs.getInt("EmployeeID"), 
                        rs.getString("FirstName"), 
                        rs.getString("LastName"),
                        rs.getString("Title"));
            }
            System.out.println();
            
        } catch (SQLException e) {
            System.err.println("Error fetching all employees: " + e.getMessage());
        }
    }

    private static void displayEmployeesForFrenchOrders(Connection con) {
        System.out.println("--- EMPLOYEES HANDLING ORDERS FROM FRANCE ---");
        
        String query = "SELECT DISTINCT e.EmployeeID, e.FirstName, e.LastName " +
                       "FROM Employees e " +
                       "JOIN Orders o ON e.EmployeeID = o.EmployeeID " +
                       "WHERE o.ShipCountry = ?";
                       
        try (PreparedStatement ps = con.prepareStatement(query)) {
            
            // Parameterizing the query to prevent SQL injection and improve performance
            ps.setString(1, "France");
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("ID: %d | Name: %s %s%n", 
                            rs.getInt("EmployeeID"), 
                            rs.getString("FirstName"), 
                            rs.getString("LastName"));
                }
                System.out.println();
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching employees for French orders: " + e.getMessage());
        }
    }
}