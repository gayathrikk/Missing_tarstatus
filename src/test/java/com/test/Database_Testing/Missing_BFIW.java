package com.test.Database_Testing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Missing_BFIW{

    private Connection conn;
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
        String biosampleid = System.getProperty("biosampleid");

        if (biosampleid == null || biosampleid.trim().isEmpty()) {
            throw new RuntimeException("biosampleid system property is missing or empty.");
        }

        System.out.println("Running for biosampleid: " + biosampleid);

        Set<Integer> retrievedIndexes = new HashSet<>();
        int actualEndIndex = 0;
        int missingCount = 0;
        int gapThreshold = 50;

        try {
            String query = "SELECT positionindex FROM section WHERE jp2Path LIKE ? ORDER BY positionindex ASC";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, "%/" + biosampleid + "/BFIW/%");  // <-- Updated folder name
                try (ResultSet rs = stmt.executeQuery()) {
                    int prev = 0;
                    while (rs.next()) {
                        int current = rs.getInt("positionindex");
                        retrievedIndexes.add(current);

                        if (prev != 0 && current - prev >= gapThreshold) {
                            actualEndIndex = prev;
                            break;
                        }

                        prev = current;
                        actualEndIndex = current;
                    }
                }
            }

            System.out.println("Missing position indexes (up to " + actualEndIndex + "):");
            for (int i = 1; i <= actualEndIndex; i++) {
                if (!retrievedIndexes.contains(i)) {
                    System.out.println(i);
                    missingCount++;
                }
            }

            System.out.println("\nTotal missing position indexes: " + missingCount);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error executing query.");
        }
    }

    @AfterClass
    public void tearDown() {
        try {
            if (conn != null) conn.close();
            System.out.println("Database connection closed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
