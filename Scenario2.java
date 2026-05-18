import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ScenarioTwo {

    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=Northwind";
    private static final String USER = "sa"; 
    private static final String PASS = "password"; 

    public static void main(String[] args) {
        
        try (Connection con = DriverManager.getConnection(DB_URL, USER, PASS)) {
            
            // Turn off auto-commit!!!
            con.setAutoCommit(false);
            System.out.println("Connection established. Auto-commit disabled.\n");

            // Clean up from previous runs
            cleanupTestRecords(con);

            // 1. First Transaction: Employee Management
            manageEmployees(con);

            // 2. Second Transaction: Batch Orders
            insertOrders(con);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void manageEmployees(Connection con) {
        System.out.println("--- TRANSACTION 1: EMPLOYEES ---");
        
        String insertSql = "INSERT INTO Employees (FirstName, LastName) VALUES (?, ?)";
        String updateSql = "UPDATE Employees SET LastName = ? WHERE FirstName = ? AND LastName = ?";
        
        try (PreparedStatement insertStmt = con.prepareStatement(insertSql);
             PreparedStatement updateStmt = con.prepareStatement(updateSql)) {
            
            // Insert Employee 1
            insertStmt.setString(1, "John");
            insertStmt.setString(2, "Doe");
            insertStmt.executeUpdate();
            
            // Insert Employee 2
            insertStmt.setString(1, "Jane");
            insertStmt.setString(2, "Smith");
            insertStmt.executeUpdate();
            System.out.println("Inserted 2 new employees.");

            // Update Employee 1's surname
            updateStmt.setString(1, "Doe-Johnson"); // New Surname
            updateStmt.setString(2, "John");        // Search condition
            updateStmt.setString(3, "Doe");         // Search condition
            updateStmt.executeUpdate();
            System.out.println("Updated surname for John.");

            con.commit();
            System.out.println("Transaction 1 committed successfully.\n");
            
        } catch (SQLException e) {
            System.err.println("Transaction 1 failed. Rolling back.");
            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private static void insertOrders(Connection con) {
        System.out.println("--- TRANSACTION 2: ORDERS ---");
        
        String insertOrderSql = "INSERT INTO Orders (CustomerID, EmployeeID, ShipCity) VALUES (?, ?, ?)";
        
        try (PreparedStatement ps = con.prepareStatement(insertOrderSql)) {
            
            // 10 INSERT statements
            for (int i = 1; i <= 10; i++) {
                ps.setString(1, "ALFKI");
                ps.setInt(2, 1);
                ps.setString(3, "Warsaw"); 
                
                ps.executeUpdate();
            }
            
            con.commit();

            System.out.println("Inserted 10 new orders.");
            System.out.println("Transaction 2 committed successfully.\n");
            
        } catch (SQLException e) {
            System.err.println("Transaction 2 failed. Rolling back.");
            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private static void cleanupTestRecords(Connection con) {
        System.out.println("--- CLEANUP ---");
        String deleteOrders = "DELETE FROM Orders WHERE ShipCity = 'Warsaw'";
        String deleteEmployees = "DELETE FROM Employees WHERE FirstName IN ('John', 'Jane')";
        
        try (PreparedStatement psOrd = con.prepareStatement(deleteOrders);
             PreparedStatement psEmp = con.prepareStatement(deleteEmployees)) {
             
            int ordersDeleted = psOrd.executeUpdate();
            int empsDeleted = psEmp.executeUpdate();
            
            con.commit(); // Commit the cleanup
            System.out.println("Cleaned up " + ordersDeleted + " orders and " + empsDeleted + " employees from previous runs.\n");
            
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}