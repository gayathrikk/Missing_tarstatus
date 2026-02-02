package com.test.Database_Testing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.Scanner;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Missing_BFI {

    private Connection conn;
    private PreparedStatement stmt;
    private ResultSet rs;

    private static final String URL = "jdbc:mysql://apollo2.humanbrain.in:3306/HBA_V2";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "Health#123";

    @BeforeClass
    public void setup() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Database connection established.");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Database connection failed.");
        }
    }

    @Test
    public void displayMissingPositionIndexes() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Biosample id: ");
        String biosampleid = scanner.nextLine();  // Get user input from console

        Set<Integer> retrievedIndexes = new HashSet<>();
        int endIndex = 0;
        int missingCount = 0;

        try {
            // Get the maximum positionindex from the database
            String maxQuery = "SELECT MAX(positionindex) AS maxIndex FROM section WHERE jp2Path LIKE '%/" + biosampleid + "/BFI/%'";
            stmt = conn.prepareStatement(maxQuery);
            rs = stmt.executeQuery();

            if (rs.next()) {
                endIndex = rs.getInt("maxIndex");
            }

            // Query to fetch position indexes based on the user's input
            String query = "SELECT positionindex FROM section WHERE jp2Path LIKE '%/" + biosampleid + "/BFI/%' ORDER BY positionindex ASC";
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();

            // Store all retrieved position indexes
            while (rs.next()) {
                int posIndex = rs.getInt("positionindex");
                retrievedIndexes.add(posIndex);
            }

            // Print missing position indexes and count
            System.out.println("Missing position indexes:");
            for (int i = 1; i <= endIndex; i++) {
                if (!retrievedIndexes.contains(i)) {
                    System.out.println(i);
                    missingCount++;
                }
            }

            System.out.println("\nTotal missing position indexes: " + missingCount);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error executing query.");
        } finally {
            scanner.close();  // Close the scanner
        }
    }

    @AfterClass
    public void tearDown() {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
            System.out.println("Database connection closed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
