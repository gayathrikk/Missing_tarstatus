package com.test.Database_Testing;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.sql.*;

public class BFI_NISL_Rotation {

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
            System.out.println("✅ Database connection established.");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Database connection failed.");
        }
    }

    @Test
    public void fetchRotationComparison() {
        String query = "SELECT " +
                "s1.name AS section_name, " +
                "s1.positionindex AS position_index, " +
                "s1.rigidrotation AS rotation_bfiw, " +
                "s2.trsdata AS trsdata_nisl " +
                "FROM section s1 " +
                "JOIN series se1 ON se1.id = s1.series " +
                "JOIN section s2 ON s1.name = s2.name AND s1.positionindex = s2.positionindex " +
                "JOIN series se2 ON se2.id = s2.series " +
                "WHERE se1.name = '222_BFIW' AND se2.name = '222_NISL' " +
                "ORDER BY s1.positionindex";

        try {
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();

            System.out.printf("%-30s %-20s %-20s %-20s%n",
                    "Section Name", "Position Index", "Rotation (BFIW)", "Rotation (NISL °)");
            System.out.println("---------------------------------------------------------------------------------------");

            while (rs.next()) {
                String sectionName = rs.getString("section_name");
                int positionIndex = rs.getInt("position_index");
                int rotationBFIW = rs.getInt("rotation_bfiw");

                String trsdata = rs.getString("trsdata_nisl");
                double rotationNISLDegrees = extractRotationInDegrees(trsdata);

                System.out.printf("%-30s %-20d %-20d %-20d%n",
                        sectionName, positionIndex, rotationBFIW, (int) rotationNISLDegrees);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper to extract "rotation" from trsdata and convert to degrees
    private double extractRotationInDegrees(String trsdata) {
        if (trsdata == null || !trsdata.contains("\"rotation\"")) {
            return 0.0;
        }

        try {
            // Extract the rotation value from JSON-like string
            int rotationIndex = trsdata.indexOf("\"rotation\":");
            if (rotationIndex == -1) return 0.0;

            int start = rotationIndex + 11;
            int end = trsdata.indexOf("}", start); // or comma if other fields exist
            String sub = trsdata.substring(start, end).trim();

            // Clean in case of trailing comma
            if (sub.endsWith(",")) {
                sub = sub.substring(0, sub.length() - 1);
            }

            double radians = Double.parseDouble(sub);
            return radians * (180.0 / Math.PI);

        } catch (Exception e) {
            System.err.println("⚠️ Error parsing trsdata: " + trsdata);
            return 0.0;
        }
    }

    @AfterClass
    public void tearDown() {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
            System.out.println("✅ Database connection closed.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
