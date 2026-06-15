/* ============================================================================
   SECTION 1: ADVANCED DATA RETRIEVAL (SELECT)
   ============================================================================ */

-- 1A: String and Date Manipulation
-- Goal: Filter by specific date parts and format text outputs cleanly.
SELECT 
    OrderID,
    UPPER(SUBSTRING(CustomerID, 1, 3)) AS ShortID,
    OrderDate
FROM Orders
WHERE YEAR(OrderDate) = 1996 
  AND MONTH(OrderDate) = 7;
GO

-- 1B: Aggregation and Subqueries (HAVING clause)
-- Goal: Find products ordered in high quantities on average.
SELECT 
    P.ProductName, 
    COUNT(OD.OrderID) AS TotalOrders
FROM Products P
JOIN [Order Details] OD ON P.ProductID = OD.ProductID
GROUP BY P.ProductName
HAVING AVG(OD.Quantity) >= 20
ORDER BY TotalOrders DESC;
GO

-- 1C: Window Functions (Moving Sums & Partitions)
-- Goal: Calculate a moving sum over the two preceding rows and the current row.
SELECT 
    OrderID, 
    ProductID, 
    (UnitPrice * Quantity) AS ProductValue,
    SUM(UnitPrice * Quantity) OVER (
        ORDER BY OrderID, ProductID 
        ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
    ) AS MovingTotalSale
FROM [Order Details];
GO


/* ============================================================================
   SECTION 2: DATA CREATION (INSERT & BULK COPIES)
   ============================================================================ */

-- 2A: Standard INSERT with Built-in Functions
-- Goal: Add a single record utilizing system functions like GETDATE().
INSERT INTO Orders (CustomerID, EmployeeID, OrderDate, ShipCountry)
VALUES ('ALFKI', 4, GETDATE(), 'Poland');
GO

-- 2B: Copying Data Between Existing Tables (Archive pattern)
-- Goal: Insert data from a SELECT statement into a pre-existing archive table.
-- Note: Assumes ArchivedOrders table has already been created (see DDL section).
INSERT INTO ArchivedOrders (OrderID, CustomerID, EmployeeID, ArchiveDate)
SELECT 
    OrderID, 
    CustomerID, 
    EmployeeID, 
    GETDATE()
FROM Orders
WHERE YEAR(OrderDate) = 1996;
GO

-- 2C: Creating and Populating a Table Simultaneously (SELECT INTO)
-- Goal: Duplicate table structures and data on the fly without a CREATE TABLE statement.
SELECT * INTO ArchivedOrderDetails 
FROM [Order Details] 
WHERE OrderID IN (SELECT OrderID FROM Orders WHERE YEAR(OrderDate) = 1996);
GO


/* ============================================================================
   SECTION 3: DATA MODIFICATION (UPDATE)
   ============================================================================ */

-- 3A: Mathematical Operations and Rounding
-- Goal: Apply percentage changes to numerical columns and round the results.
UPDATE [Order Details]
SET Quantity = ROUND(Quantity * 0.8, 0)
WHERE ProductID = (SELECT ProductID FROM Products WHERE ProductName = 'Ikura')
  AND OrderID IN (SELECT OrderID FROM Orders WHERE OrderDate > '1997-05-15');
GO

-- 3B: Flagging/Categorizing Existing Data
-- Goal: Set a flag based on conditions tied to a specific customer.
-- Note: Assumes IsCancelled column has been added (see DDL section).
UPDATE Orders
SET IsCancelled = 1
WHERE CustomerID = 'ALFKI';
GO

-- 3C: Updating via JOINs (SQL Server Specific)
-- Goal: Update a table based on data calculated and aggregated from another table.
-- Note: Assumes TotalOrderCount column has been added.
UPDATE C
SET C.TotalOrderCount = Agg.OrderCount
FROM Customers C
JOIN (
    SELECT CustomerID, COUNT(OrderID) AS OrderCount
    FROM Orders
    GROUP BY CustomerID
) AS Agg ON C.CustomerID = Agg.CustomerID;
GO


/* ============================================================================
   SECTION 4: DATA REMOVAL (DELETE)
   ============================================================================ */

-- 4A: Deleting Unreferenced Data
-- Goal: Remove parent records that have no associated child records.
DELETE FROM Customers
WHERE CustomerID NOT IN (SELECT CustomerID FROM Orders);
GO

-- 4B: Deleting Top N Records
-- Goal: Target a specific number of rows based on a condition.
DELETE TOP (5) FROM [Order Details]
WHERE Quantity < 5;
GO

-- 4C: Transaction-Wrapped Deletions
-- Goal: Safely test destructive operations by defining transaction boundaries.
BEGIN TRANSACTION;

    -- Delete child records first to avoid foreign key constraint violations
    DELETE FROM [Order Details]
    WHERE OrderID IN (SELECT OrderID FROM Orders WHERE CustomerID = 'ALFKI');

    -- Delete parent records
    DELETE FROM Orders
    WHERE CustomerID = 'ALFKI';

-- COMMIT;   -- Uncomment to save changes
-- ROLLBACK; -- Uncomment to revert changes
GO


/* ============================================================================
   SECTION 5: SCHEMA MODIFICATION (DDL)
   ============================================================================ */

-- 5A: Defining a Table with Constraints
-- Goal: Create a table from scratch with primary/foreign keys and identity columns.
CREATE TABLE Pricelist (
    PriceID INT IDENTITY(1,1) PRIMARY KEY,
    ProductID INT NOT NULL,
    Price DECIMAL(10, 2) NOT NULL,
    DateFrom DATE NOT NULL,
    DateTo DATE,
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
);
GO

-- 5B: Adding Columns to an Existing Table
-- Goal: Alter a table structure to accommodate new tracking requirements.
ALTER TABLE Orders
ADD IsCancelled INT DEFAULT 0;
GO

-- 5C: Modifying an Existing Column
-- Goal: Change a column's data type or capacity.
ALTER TABLE Customers
ALTER COLUMN CompanyName VARCHAR(100) NOT NULL;
GO




/* ============================================================================
   SECTION 6: COMPLICATED SELECT
   ============================================================================ */


-- Returns the Employee's Full Name (Combine FirstName and LastName with a space in between).

-- Returns the Customer's CompanyName.

-- Calculates the Total Quantity of all products that specific employee sold to that specific customer across the entire year of 1997.

-- Filter: Only include Employee-Customer pairs where the Total Quantity sold in 1997 was strictly greater than 100.

-- Sort: Order the final results from the highest total quantity to the lowest.

SELECT 
    E.FirstName + ' ' + E.LastName AS EmployeeFullName,
    C.CompanyName,
    SUM(OD.Quantity) AS TotalQuantity
FROM Employees E
JOIN Orders O ON E.EmployeeID = O.EmployeeID
JOIN Customers C ON O.CustomerID = C.CustomerID
JOIN [Order Details] OD ON O.OrderID = OD.OrderID
WHERE YEAR(O.OrderDate) = 1997
GROUP BY 
    E.FirstName, 
    E.LastName, 
    C.CompanyName
HAVING SUM(OD.Quantity) > 100
ORDER BY TotalQuantity DESC;
GO