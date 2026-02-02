package com.test.Database_Testing;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Download_MissingBFI {

    private Connection conn;
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
    public void generateMissingIndexesReport() {
        List<String> biosampleIds = new ArrayList<>();

        // Fetch biosample IDs from biosample.id table
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM biosample")) {
            while (rs.next()) {
                biosampleIds.add(rs.getString("id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Failed to fetch biosample IDs.");
        }

        // Create output folder if it doesn't exist
        File outputDir = new File("output");
        if (!outputDir.exists()) outputDir.mkdirs();

        String outputFile = "output/missing_indexes_all.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            for (String biosampleId : biosampleIds) {
                Set<Integer> indexSet = new HashSet<>();
                int actualEndIndex = 0;

                // Query to get positionindexes for each biosample
                String query = "SELECT positionindex FROM section WHERE jp2Path LIKE ? ORDER BY positionindex ASC";

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, "%/" + biosampleId + "/BFI/%");
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            int index = rs.getInt("positionindex");
                            if (index < 10000) { // Ignore dummy records
                                indexSet.add(index);
                                if (index > actualEndIndex) actualEndIndex = index;
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    continue;
                }

                // Write missing indexes for this biosample
                writer.write("=== Biosample ID: " + biosampleId + " ===\n");
                int missingCount = 0;
                for (int i = 1; i <= actualEndIndex; i++) {
                    if (!indexSet.contains(i)) {
                        writer.write(i + "\n");
                        missingCount++;
                    }
                }
                writer.write("Total Missing: " + missingCount + "\n\n");
            }

            System.out.println("✅ Report generated at: " + outputFile);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Failed to write output file.");
        }
    }

    @AfterClass
    public void tearDown() {
        try {
            if (conn != null) conn.close();
            System.out.println("🔒 Database connection closed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
