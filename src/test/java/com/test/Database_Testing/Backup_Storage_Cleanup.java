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

public class Backup_Storage_Cleanup {

    @Test
    public void checkStatus1AndSendAlert() {
        String url = "jdbc:mysql://apollo2.humanbrain.in:3306/HBA_V2";
        String username = "root";
        String password = "Health#123";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL JDBC Driver Registered");

            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                String query = "SELECT sd.bio_id, sd.bio_name, sb.assign_date " +
                        "FROM storage_backup sb " +
                        "JOIN storage_details sd ON sb.storage_details = sd.id " +
                        "WHERE sb.status = 1";

                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {

                    List<String> biosampleIds = new ArrayList<>();
                    List<String> brainNames = new ArrayList<>();
                    List<Date> assignDates = new ArrayList<>();

                    while (rs.next()) {
                        biosampleIds.add(rs.getString("bio_id"));
                        brainNames.add(rs.getString("bio_name"));
                        assignDates.add(rs.getDate("assign_date"));
                    }

                    if (!biosampleIds.isEmpty()) {
                        System.out.println("⚠ Records with status 1 found. Sending storage cleanup pending email...");
                        sendEmailStorageCleanupPending(biosampleIds, brainNames, assignDates);
                    } else {
                        System.out.println("✅ No records with status 1. No action needed.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendEmailStorageCleanupPending(List<String> biosampleIds, List<String> brainNames, List<Date> assignDates) {
        String from = "automationsoftware25@gmail.com";
        String password = "wjzcgaramsqvagxu";
        String[] to = {"nathan.i@htic.iitm.ac.in", "azizahammed.a@htic.iitm.ac.in"};

        String[] cc = {"ramananv2024@gmail.com", "gayathri@htic.iitm.ac.in", "supriti@htic.iitm.ac.in"};

        String subject = "🧠 Storage cleanup needed – Your brain samples are losing patience!";

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
        bodyBuilder.append("<p>Hi Nathan/Aziz bro,</p>");
        bodyBuilder.append("<p>We, the brain samples, are patiently waiting for our storage cleanup...</p>");
        bodyBuilder.append("<p>Here’s our lineup:</p>");

        bodyBuilder.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse;'>");
        bodyBuilder.append("<tr style='background-color: #f2f2f2;'>")
                .append("<th>Biosample ID</th>")
                .append("<th>Brain Name</th>")
                .append("<th>Assign Date</th>")
                .append("<th>Pending Since</th>")
                .append("</tr>");

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        for (int i = 0; i < biosampleIds.size(); i++) {
            String bioId = biosampleIds.get(i);
            String brainName = brainNames.get(i);
            Date assignDate = assignDates.get(i);
            String formattedDate = sdf.format(assignDate);

            LocalDate assignLocal = new java.sql.Date(assignDate.getTime()).toLocalDate();
            long daysPending = ChronoUnit.DAYS.between(assignLocal, LocalDate.now());

            bodyBuilder.append("<tr>")
                    .append("<td>").append(bioId).append("</td>")
                    .append("<td>").append(brainName).append("</td>")
                    .append("<td>").append(formattedDate).append("</td>")
                    .append("<td>").append(daysPending).append(" days</td>")
                    .append("</tr>");
        }

        bodyBuilder.append("</table>");
        bodyBuilder.append("<p>Please help us out soon – only then can we proudly proceed to the backup process! 😅</p>");
        bodyBuilder.append("<p>Kindly complete the storage cleanup before we start organizing a brain strike! 🧠✊</p>");
        bodyBuilder.append("<p style='color:gray;font-size:small;'>This is an automatically generated email. Please do not reply to this message.</p>");
        bodyBuilder.append("</body></html>");

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));

            // Add all "TO" recipients
            for (String recipient : to) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }

            // Add BCC recipients if any
            for (String bccRecipient : cc) {
                message.addRecipient(Message.RecipientType.CC, new InternetAddress(bccRecipient));
            }

            message.setSubject(subject);
            message.setContent(bodyBuilder.toString(), "text/html; charset=UTF-8");

            Transport.send(message);
            System.out.println("📧 Email with assign date and pending days sent successfully!");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
