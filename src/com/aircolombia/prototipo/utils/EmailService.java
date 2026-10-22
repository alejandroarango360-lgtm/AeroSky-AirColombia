package com.aircolombia.prototipo.utils;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {

    // ⚠️ CAMBIA ESTOS DATOS POR LOS TUYOS
    private static final String REMITENTE = "arangoalejandro360@gmail.com"; 
    private static final String PASSWORD = "qdvo qfrr khzw tysq"; // Contraseña de aplicación de Google

    public static boolean enviarCodigoConfirmacion(String destinatario, String pnr, String codigo) {
        // --- VALIDACIÓN PARA EVITAR EL ERROR DE NULL ---
        if (destinatario == null || destinatario.trim().isEmpty() || !destinatario.contains("@")) {
            System.out.println("Error: El correo del destinatario es nulo o inválido.");
            return false; 
        }
        // Configuración de Gmail
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(REMITENTE, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(REMITENTE));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject("Código de Confirmación de Pago - AeroSky");

            String htmlContent = "<div style='font-family: Arial, sans-serif; text-align: center; padding: 20px;'>"
                    + "<h2 style='color: #C8102E;'>AeroSky</h2>"
                    + "<p>Hemos recibido una solicitud de pago para tu reserva con PNR: <b>" + pnr + "</b></p>"
                    + "<p>Para completar la transacción, ingresa el siguiente código en la aplicación:</p>"
                    + "<h1 style='color: #15803D; letter-spacing: 5px; font-size: 36px;'>" + codigo + "</h1>"
                    + "<p style='color: #888;'>Si no solicitaste este pago, puedes ignorar este correo.</p>"
                    + "</div>";

            message.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("Correo de confirmación enviado a: " + destinatario);
            return true;

        } catch (MessagingException e) {
            System.out.println("Error al enviar correo: " + e.getMessage());
            return false;
        }
    }
}