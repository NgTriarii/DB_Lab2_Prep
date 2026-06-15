import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class App {
    public static void main(String[] args) {

            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            } catch (ClassNotFoundException e) {
                System.err.println("JDBC Driver not found.");
                return;
            }

            String url = "jdbc:sqlserver://vl53\\MSSQLSERVER:1433;databaseName=Northwind;user=sa;password=SAstudent1;encrypt=true;trustServerCertificate=true";


        try (Connection con = DriverManager.getConnection(url)) {

            con.setAutoCommit(false);
                System.out.println("Connected to the database successfully.\n");
            
            String insertSql = "INSERT INTO LIBRARY_LOAN (STUDENT_INDEX, BOOK_TITLE, LOAN_DATE, OVERDUE_FEE) VALUES (?, ?, GETDATE(), ?)";
                try (PreparedStatement psInsert = con.prepareStatement(insertSql)) {
                    for (int i = 0; i < 3; i++) {
                        psInsert.setInt(1, i * 1000 + (i * 56 % 43));
                        psInsert.setString(2, "Book " + (i));
                        psInsert.setFloat(3, 0.00f);
                        
                        psInsert.executeUpdate();
                    }
                    
                    con.commit();
                    printLoans(con, "Transaction A (Insert 3 book loans)");
                } catch (SQLException e) {
                    con.rollback();
                    System.err.println("Transaction A failed: " + e.getMessage());
                }

            try {
                    String updateSql = "UPDATE LIBRARY_LOAN SET OVERDUE_FEE = OVERDUE_FEE + 15.50 WHERE STUDENT_INDEX % 2 = 0";
                    try (Statement stmtUpdate = con.createStatement()) {
                        stmtUpdate.executeUpdate(updateSql);
                    }

                    con.commit();
                    printLoans(con, "Transaction B");
                } catch (SQLException e) {
                    con.rollback();
                    System.err.println("Transaction B failed: " + e.getMessage());
                }

            try {
                    String deleteSql = "DELETE TOP(1) FROM LIBRARY_LOAN WHERE OVERDUE_FEE = (select MAX(OVERDUE_FEE) from LIBRARY_LOAN) ";
                    try (Statement stmt = con.createStatement()) {
                        stmt.executeUpdate(deleteSql);
                    }

                    con.commit();
                    printLoans(con, "Transaction C");
                } catch (SQLException e) {
                    con.rollback();
                    System.err.println("Transaction C failed: " + e.getMessage());
                }

        } 
        catch (SQLException e) {
                System.err.println("Database connection error: " + e.getMessage());
        }
    }

    private static void printLoans(Connection con, String stage) {
        System.out.println("--- Table State: " + stage + " ---");
        String query = "SELECT * FROM LIBRARY_LOAN";
        
        try (Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query)) {
            
            java.sql.ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) System.out.print(" | ");
                    System.out.print(rsmd.getColumnName(i) + ": " + rs.getString(i));
                }
                System.out.println();
            }
            System.out.println();
            
        } catch (SQLException e) {
            System.err.println("Error fetching table data: " + e.getMessage());
        }
        }
}
