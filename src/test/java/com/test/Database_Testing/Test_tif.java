		package com.test.Database_Testing;
		
		import org.testng.annotations.Test;
		import java.sql.*;
		import java.util.*;
		
		public class Test_tif {
		
		    @Test
		    public void testDBQueryAndPrintResults() {
		        Map<Integer, Map<String, List<Integer>>> biosampleSeriesSections = connectAndQueryDB();
		    }
		
		    private Map<Integer, Map<String, List<Integer>>> connectAndQueryDB() {
		        Map<Integer, Map<String, List<Integer>>> biosampleSeriesSections = new HashMap<>();
		
		        String url = "jdbc:mysql://apollo2.humanbrain.in:3306/HBA_V2";
		        String username = "root";
		        String password = "Health#123";
		
		        try {
		            Class.forName("com.mysql.cj.jdbc.Driver");
		            System.out.println("Driver loaded");
		
		            try (Connection connection = DriverManager.getConnection(url, username, password)) {
		                System.out.println("MySQL database connected");
		                biosampleSeriesSections = executeAndPrintQuery(connection);
		            }
		        } catch (ClassNotFoundException e) {
		            System.err.println("MySQL Driver not found: " + e.getMessage());
		        } catch (SQLException e) {
		            System.err.println("Database connection error: " + e.getMessage());
		        }
		        return biosampleSeriesSections;
		    }
		
		    private Map<Integer, String> biosampleBrainNames = new HashMap<>(); // Stores biosample -> brain name
		
		    private Map<Integer, Map<String, List<Integer>>> executeAndPrintQuery(Connection connection) {
		        String query = "SELECT b.id AS biosample, sr.name AS series_name, s.positionindex AS section_no, ss.name AS brain_name " +
		                       "FROM section s " +
		                       "INNER JOIN series sr ON s.series = sr.id " +
		                       "INNER JOIN seriesset ss ON sr.seriesset = ss.id " +
		                       "INNER JOIN biosample b ON ss.biosample = b.id " +
		                       "WHERE jp2Path LIKE '%.tif' " ;
		
		        Map<Integer, Map<String, List<Integer>>> biosampleSeriesSections = new HashMap<>();
		
		        try (PreparedStatement statement = connection.prepareStatement(query);
		             ResultSet resultSet = statement.executeQuery()) {
		
		            boolean dataFound = false;
		            System.out.printf("%-20s %-10s %-20s %-10s%n", "Brain Name", "Biosample", "Series Name", "Section No");
		            System.out.println("-".repeat(65));
		
		            while (resultSet.next()) {
		                dataFound = true;
		
		                int biosample = resultSet.getInt("biosample");
		                String seriesName = resultSet.getString("series_name");
		                int sectionNo = resultSet.getInt("section_no");
		                String brainName = resultSet.getString("brain_name");
		
		                System.out.printf("%-50s %-10d %-20s %-10d%n", brainName, biosample, seriesName, sectionNo);
		
		                biosampleBrainNames.put(biosample, brainName);
		
		                String suffix = seriesName.contains("_") ? seriesName.split("_", 2)[1] : seriesName;
		
		                biosampleSeriesSections
		                        .computeIfAbsent(biosample, k -> new HashMap<>())
		                        .computeIfAbsent(suffix, k -> new ArrayList<>())
		                        .add(sectionNo);
		            }
		
		            if (!dataFound) {
		                System.out.println("No records found for the specified date.");
		            }
		
		        } catch (SQLException e) {
		            System.err.println("SQL query execution error: " + e.getMessage());
		        }
		
		        return biosampleSeriesSections;
		    }
		}
