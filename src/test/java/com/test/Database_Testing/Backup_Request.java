	package com.test.Database_Testing;
	
	import org.testng.annotations.Test;
	import javax.mail.*;
	import javax.mail.internet.*;
	import java.sql.*;
	
	import java.util.*;
	
	public class Backup_Request {
	
	    @Test
	    public void checkBackupStatusAndSendAlert() {
	        // Database credentials
	        String url = "jdbc:mysql://apollo2.humanbrain.in:3306/HBA_V2";
	        String username = "root";
	        String password = "Health#123";
	
	        // SQL query to check today's backup status
	        String query = "SELECT COUNT(*) FROM storage_backup WHERE status IN (1, 2, 3)";
	        try {
	            Class.forName("com.mysql.cj.jdbc.Driver");
	            System.out.println("MySQL JDBC Driver Registered");
	
	            try (Connection conn = DriverManager.getConnection(url, username, password);
	            	     PreparedStatement stmt = conn.prepareStatement(query);
	            	     ResultSet rs = stmt.executeQuery()) {

	            	    if (rs.next() && rs.getInt(1) > 0) {
	            	        System.out.println("✅ Backup Request record found with status 1, 2 or 3. No email alert sent.");
	            	    } else {
	            	        System.out.println("❌ No backup Request records with status 1, 2 or 3. Sending email alert...");
	            	        sendEmailAlert();
	            	    }

	            	}
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	
	    private void sendEmailAlert() {
	        String from = "automationsoftware25@gmail.com";
	        String password = "wjzcgaramsqvagxu"; // App Password
	        String[] to = {"gayuriche26@gmail.com"};
	        //String[] to = {"sindhu.r@htic.iitm.ac.in"};
	        //String[] cc = {"richavermaj@gmail.com","supriti@htic.iitm.ac.in", "azizahammed.a@htic.iitm.ac.in", "satheskumar@htic.iitm.ac.in"};
	        String subject = "⚠ Backup Request Alert";
	        
	
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
	
	        try {
	            MimeMessage message = new MimeMessage(session);
	            message.setFrom(new InternetAddress(from));
	
	            for (String recipient : to) {
	                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
	            }
//	            
//	            for (String ccRecipient : cc) {
//	                message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccRecipient));
//	            }
	
	            message.setSubject(subject);
	            String body = "<html><body>" +
	                    "<p><b>Please provide backup request for three additional brains.\r\n"
	                    + "We will await your update.</b></p>" +
	                    "<p style='color:gray;font-size:small;'>This is an automatically generated email. Please do not reply to this message.</p>" +
	                    "</body></html>";
	            message.setContent(body, "text/html");
	
	            Transport.send(message);
	            System.out.println("📧 Alert email sent successfully!");
	
	        } catch (MessagingException e) {
	            e.printStackTrace();
	        }
	    }
	}
