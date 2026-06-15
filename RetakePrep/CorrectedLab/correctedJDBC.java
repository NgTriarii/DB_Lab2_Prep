import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Task1Corrected {

    public static void main(String[] args) {
        // Driver Inclusion
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found.");
            return;
        }

        String url = "jdbc:sqlserver://vl53\\MSSQLSERVER:1433;databaseName=Northwind;user=sa;password=SAstudent1;encrypt=true;trustServerCertificate=true";

        try (Connection con = DriverManager.getConnection(url)) {
            // Disable auto-commit
            con.setAutoCommit(false);
            System.out.println("Connected to the database successfully.\n");

        
            // TRANSACTION A: Delete highest GROSS_AMT
            
            // FIX: Incorrect search of a row to delete resolved (using TOP 1 and a subquery)
            String deleteSql = "DELETE TOP (1) FROM INVOICE WHERE GROSS_AMT = (SELECT MAX(GROSS_AMT) FROM INVOICE)";
            try (PreparedStatement psDel = con.prepareStatement(deleteSql)) {
                // Call executeUpdate()
                int rowsDeleted = psDel.executeUpdate();
                System.out.println("Transaction A: Deleted " + rowsDeleted + " row(s).");
                
                con.commit();
                printTable(con, "Transaction A (Delete Max Gross)");
            } catch (SQLException e) {
                con.rollback();
                System.err.println("Transaction A failed: " + e.getMessage());
            }

            // TRANSACTION B: Insert 3 new rows

            String insertSql = "INSERT INTO INVOICE (INVOICE_DATE, DESCRIPTION_TXT, NET_AMT, GROSS_AMT, TAX_PCT) VALUES (GETDATE(), ?, ?, ?, ?)";
            try (PreparedStatement psInsert = con.prepareStatement(insertSql)) {
                for (int i = 0; i < 3; i++) {
                    int netAmt = 100 * (i + 1);
                    float taxPct = 0.20f;
                    int grossAmt = (int) (netAmt * (1 + taxPct));

                    psInsert.setString(1, "Auto-generated invoice " + (i + 1));
                    psInsert.setInt(2, netAmt);
                    psInsert.setInt(3, grossAmt);
                    psInsert.setFloat(4, taxPct);
                    
                    psInsert.executeUpdate();
                }
                
                con.commit();
                printTable(con, "Transaction B (Insert 3 Invoices)");
            } catch (SQLException e) {
                con.rollback();
                System.err.println("Transaction B failed: " + e.getMessage());
            }

            // TRANSACTION C: Update all rows & Insert 1
            
            try {
                // 1. Double the GROSS_AMT for all rows
                String updateSql = "UPDATE INVOICE SET GROSS_AMT = GROSS_AMT * 2";
                try (Statement stmtUpdate = con.createStatement()) {
                    stmtUpdate.executeUpdate(updateSql);
                }

                // 2. Add new specific invoice
                String insertSingleSql = "INSERT INTO INVOICE (INVOICE_DATE, DESCRIPTION_TXT, NET_AMT, GROSS_AMT, TAX_PCT) VALUES (GETDATE(), 'Zero Tax Promo', 1000, 1000, 0.0)";
                try (Statement stmtInsert = con.createStatement()) {
                    stmtInsert.executeUpdate(insertSingleSql);
                }

                con.commit();
                printTable(con, "Transaction C (Double Gross & Insert 1)");
            } catch (SQLException e) {
                con.rollback();
                System.err.println("Transaction C failed: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
    }

    // Printing Helper function
    private static void printTable(Connection con, String stage) {
    System.out.println("--- Table State: " + stage + " ---");
    String query = "SELECT * FROM INVOICE";
    
    try (Statement stmt = con.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {
         
        // Get the metadata to dynamically read columns
        java.sql.ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        
        while (rs.next()) {
            // Loop through all columns automatically (JDBC indexes start at 1)
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) System.out.print(" | ");
                
                // Print "ColumnName: Value" 
                System.out.print(rsmd.getColumnName(i) + ": " + rs.getString(i));
            }
            System.out.println(); // Move to the next row
        }
        System.out.println();
        
    } catch (SQLException e) {
        System.err.println("Error fetching table data: " + e.getMessage());
    }
}
}