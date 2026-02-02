package com.test.Database_Testing;

import org.testng.annotations.Test;

import javax.mail.*;
import javax.mail.internet.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Date;

public class Backup_TestVerification {

    @Test
    public void checkStatus6AndSendAlert() {
        String url = "jdbc:mysql://apollo2.humanbrain.in:3306/HBA_V2";
        String username = "root";
        String password = "Health#123";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL JDBC Driver Registered");

            try (Connection conn = DriverManager.getConnection(url, username, password)) {

                String query = "SELECT sd.bio_id, sd.bio_name, sb.tap_handover " +
                               "FROM storage_backup sb " +
                               "JOIN storage_details sd ON sb.storage_details = sd.id " +
                               "WHERE sb.status = 6";

                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {

                    List<String> biosampleIds = new ArrayList<>();
                    List<String> brainNames = new ArrayList<>();
                    List<Date> handoverDates = new ArrayList<>();

                    while (rs.next()) {
                        biosampleIds.add(rs.getString("bio_id"));
                        brainNames.add(rs.getString("bio_name"));
                        handoverDates.add(rs.getDate("tap_handover"));
                    }

                    if (!biosampleIds.isEmpty()) {
                        System.out.println("⚠ Records with status 6 found. Sending verification pending email...");
                        sendEmailVerificationPending(biosampleIds, brainNames, handoverDates);
                    } else {
                        System.out.println("✅ No records with status 6. No action needed.");
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendEmailVerificationPending(List<String> biosampleIds, List<String> brainNames, List<Date> handoverDates) {
        String from = "automationsoftware25@gmail.com";
        String password = "wjzcgaramsqvagxu";
        String to = "gayuriche26@gmail.com";

        String[] bcc = {
        };

        String subject = "⚠ Backup Verification Pending Alert";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("<html><body>");
        bodyBuilder.append("<p>Dear Team,</p>");
        bodyBuilder.append("<p>Please note that backup verification is pending for the following brain samples:</p>");

        bodyBuilder.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse;'>");
        bodyBuilder.append("<tr style='background-color: #f2f2f2;'>")
                   .append("<th>Biosample ID</th>")
                   .append("<th>Brain Name</th>")
                   .append("<th>Tape Handover Date</th>")
                   .append("<th>Verification Pending</th>")
                   .append("</tr>");

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        for (int i = 0; i < biosampleIds.size(); i++) {
            String bioId = biosampleIds.get(i);
            String brainName = brainNames.get(i);
            Date handoverDate = handoverDates.get(i);
            String formattedDate = sdf.format(handoverDate);

            LocalDate handover = new java.sql.Date(handoverDate.getTime()).toLocalDate();
            long daysPending = ChronoUnit.DAYS.between(handover, LocalDate.now());

            bodyBuilder.append("<tr>")
                       .append("<td>").append(bioId).append("</td>")
                       .append("<td>").append(brainName).append("</td>")
                       .append("<td>").append(formattedDate).append("</td>")
                       .append("<td>").append(daysPending).append(" days</td>")
                       .append("</tr>");
        }

        bodyBuilder.append("</table>");
        bodyBuilder.append("<p>Kindly prioritize and complete the verification process for the above entries at the earliest.</p>");
        bodyBuilder.append("<p>Best regards,<br>Software Team</p>");
        bodyBuilder.append("<p style='color:gray;font-size:small;'>This is an automatically generated email. Please do not reply to this message.</p>");
        bodyBuilder.append("</body></html>");

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            for (String bccRecipient : bcc) {
                message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bccRecipient));
            }

            message.setSubject(subject);
            message.setContent(bodyBuilder.toString(), "text/html");

            Transport.send(message);
            System.out.println("📧 Email with tape handover and pending days sent successfully!");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
