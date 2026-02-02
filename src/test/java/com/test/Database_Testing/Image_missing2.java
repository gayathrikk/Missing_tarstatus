package com.test.Database_Testing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Scanner;
import javax.mail.*;
import javax.mail.internet.*;

public class Image_missing2 {

    static class Record {
        String consoleRecord;
        String emailRecord;
        String filename;
        Record(String consoleRecord, String emailRecord, String filename) {
            this.consoleRecord = consoleRecord;
            this.emailRecord = emailRecord;
            this.filename = filename;
        }
    }

    public static void main(String[] args) {
        String url = "jdbc:mysql://apollo2.humanbrain.in:3306/HBA_V2";
        String username = "root";
        String password = "Health#123";

        Scanner sc = new Scanner(System.in);
        System.out.print("Enter year (e.g., 2025): ");
        int inputYear = sc.nextInt();
        System.out.print("Enter month number (1-12): ");
        int inputMonth = sc.nextInt();
        System.out.print("Enter starting day (1-31, e.g., 8): ");
        int inputDay = sc.nextInt();
        sc.close();

        // Build query dynamically (from inputDay to today)
        String query = "SELECT sb.id, sb.name, sb.datalocation, sb.process_status, sb.created_ts, s.filename "
                     + "FROM `slidebatch` sb "
                     + "JOIN `slide` s ON sb.id = s.slidebatch "
                     + "WHERE DATE(sb.created_ts) BETWEEN '" + inputYear + "-"
                     + String.format("%02d", inputMonth) + "-" + String.format("%02d", inputDay)
                     + "' AND CURDATE()";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            Map<String, List<Record>> filenameRecordMap = new HashMap<>();
            Map<String, Integer> filenameCountMap = new HashMap<>();

            while (rs.next()) {
                int batchId = rs.getInt("id");
                String name = rs.getString("name");
                String datalocation = rs.getString("datalocation");
                int processStatus = rs.getInt("process_status");
                String createdTs = rs.getString("created_ts");
                String filename = rs.getString("filename");

                if (processStatus == 2 || processStatus == 6 || processStatus == 8) {
                    String consoleRecord = String.format("%-10d %-40s %-30s %-20d %-25s %-30s",
                            batchId, name, datalocation, processStatus, createdTs, filename);

                    String emailRecord = batchId + "|" + name + "|" + datalocation + "|" + createdTs;

                    filenameRecordMap.computeIfAbsent(filename, k -> new ArrayList<>())
                                     .add(new Record(consoleRecord, emailRecord, filename));
                    filenameCountMap.put(filename, filenameCountMap.getOrDefault(filename, 0) + 1);
                }
            }

            Set<String> repeatedFilenames = new HashSet<>();
            for (Map.Entry<String, Integer> entry : filenameCountMap.entrySet()) {
                if (entry.getValue() > 1) {
                    repeatedFilenames.add(entry.getKey());
                }
            }

            if (!repeatedFilenames.isEmpty()) {
                StringBuilder consoleOutput = new StringBuilder();
                StringBuilder emailBody = new StringBuilder();

                consoleOutput.append(String.format("%-10s %-40s %-30s %-20s %-25s %-30s\n",
                        "Batch ID", "Name", "Data Location", "Process Status", "Created TS", "Filename"));
                consoleOutput.append("---------------------------------------------------------------------------------------------------------------------------\n");

                emailBody.append("<html><body>");
                emailBody.append("<p>This is an automatically generated email,<br>For your attention and action:</p>");
                emailBody.append("<p><strong>Alert:</strong> The following images have multiple scans with pending processing (statuses 2,6,8).</p>");
                emailBody.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;'>");
                emailBody.append("<tr><th>Batch ID</th><th>Name</th><th>Data Location</th><th>Created TS</th><th>Filename</th></tr>");

                boolean emailContentExists = false;

                for (String filename : repeatedFilenames) {
                    for (Record rec : filenameRecordMap.get(filename)) {
                        consoleOutput.append(rec.consoleRecord).append("\n");

                        String[] parts = rec.emailRecord.split("\\|");
                        emailBody.append("<tr>")
                                 .append("<td>").append(parts[0]).append("</td>")
                                 .append("<td>").append(parts[1]).append("</td>")
                                 .append("<td>").append(parts[2]).append("</td>")
                                 .append("<td>").append(parts[3]).append("</td>")
                                 .append("<td>").append(rec.filename).append("</td>")
                                 .append("</tr>");
                        emailContentExists = true;
                    }
                }

                emailBody.append("</table>");
                emailBody.append("<p><strong>Note:</strong> Please rescan the images only after the previous ones reach the out stages.</p>");
                emailBody.append("</body></html>");

                System.out.println(consoleOutput.toString());

                if (emailContentExists) {
                    sendEmailAlert(emailBody.toString());
                } else {
                    System.out.println("No filenames to include in the email.\n");
                }

            } else {
                System.out.println("No repeated filenames detected for the given date range with process status 2,6,8.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void sendEmailAlert(String messageBody) {
        String[] to = {"gayuriche26@gmail.com"};
        String from = "automationsoftware25@gmail.com";
        final String emailUser = "automationsoftware25@gmail.com";
        final String emailPassword = "wjzcgaramsqvagxu";

        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailUser, emailPassword);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            for (String recipient : to) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            message.setSubject("Scanning Alert");
            message.setContent(messageBody, "text/html");

            System.out.println("Sending alert email...");
            Transport.send(message);
            System.out.println("Email sent successfully.");

        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}
