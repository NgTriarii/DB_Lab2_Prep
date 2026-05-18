public void runDatabaseTransactions(DataSource ds) {
    // try-with-resources handles closing the connection automatically
    try (Connection con = ds.getConnection()) {
        
        // Disable auto-commit to manage transactions manually
        con.setAutoCommit(false); 

        // TRANSACTION 1: INSERT (Create Dev and assign a task)
        try (PreparedStatement psDev = con.prepareStatement("INSERT INTO Developers (DevId, DevName) VALUES (?, ?)");
             PreparedStatement psTask = con.prepareStatement("INSERT INTO Assignments (AssignmentId, DevId, TaskName, Status) VALUES (?, ?, ?, ?)")) {
            
            psDev.setInt(1, 101);
            psDev.setString(2, "Alice");
            psDev.executeUpdate();

            psTask.setInt(1, 5001);
            psTask.setInt(2, 101);
            psTask.setString(3, "Optimize Backend");
            psTask.setString(4, "Pending");
            psTask.executeUpdate();

            con.commit(); // Commit Transaction 1
            System.out.println("Transaction 1 committed.");
        } catch (SQLException e) {
            con.rollback(); // Rollback on failure to prevent partial inserts
        }

        // TRANSACTION 2: UPDATE & SELECT (Update status and verify)

        try (PreparedStatement psUpdate = con.prepareStatement("UPDATE Assignments SET Status = ? WHERE AssignmentId = ?");
             PreparedStatement psSelect = con.prepareStatement("SELECT TaskName, Status FROM Assignments WHERE DevId = ?")) {
            
            psUpdate.setString(1, "In Progress");
            psUpdate.setInt(2, 5001);
            psUpdate.executeUpdate();

            psSelect.setInt(1, 101);
            ResultSet rs = psSelect.executeQuery();
            while(rs.next()) {
                System.out.println("Task: " + rs.getString("TaskName") + " is now " + rs.getString("Status"));
            }

            con.commit(); // Commit Transaction 2
        } catch (SQLException e) {
            con.rollback();
        }

        // TRANSACTION 3: DELETE (Remove records while respecting FKs)

        // Must delete the child record (Assignment) before the parent (Developer) 
        // to avoid referential integrity violations.

        try (PreparedStatement psDelTask = con.prepareStatement("DELETE FROM Assignments WHERE DevId = ?");
             PreparedStatement psDelDev = con.prepareStatement("DELETE FROM Developers WHERE DevId = ?")) {
            
            psDelTask.setInt(1, 101);
            psDelTask.executeUpdate();

            psDelDev.setInt(1, 101);
            psDelDev.executeUpdate();

            con.commit(); // Commit Transaction 3
        } catch (SQLException e) {
            con.rollback(); 
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}