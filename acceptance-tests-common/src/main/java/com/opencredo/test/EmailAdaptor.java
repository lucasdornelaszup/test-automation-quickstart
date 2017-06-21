package com.opencredo.test;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class EmailAdaptor {

    private static final String IMAP_HOST = "imap.gmail.com";
    private static final String IMAP_PROTOCOL = "imap";

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private static final String EMAIL_ADDRESS = "INSERT_TEST_EMAIL_HERE";
    private static final String PASSWORD = "INSERT_PASSWORD_HERE";
    private static final int MAX_RECENT_MESSAGES_TO_SEARCH = 1000;
    private static final String INBOX_FOLDER = "INBOX";

    private static final String TEST_EMAIL_SUBJECT = "OpenCredo Test Email";

    private final Session session;
    private Store store;

    public EmailAdaptor() throws NoSuchProviderException {
        final Properties props = new Properties();
        props.setProperty("mail.imap.ssl.enable", "true");
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.user", EMAIL_ADDRESS);
        props.put("mail.smtp.password", PASSWORD);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", true);

        session = Session.getDefaultInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication  getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                EMAIL_ADDRESS, PASSWORD);
                    }
                });

    }

    public EmailAdaptor connect() throws MessagingException {
        try {
            store = session.getStore(IMAP_PROTOCOL);
            store.connect(IMAP_HOST, EMAIL_ADDRESS, PASSWORD);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to connect with provided email account credentials ("
                    + EMAIL_ADDRESS
                    + ". Please check config in EmailAdaptor.java and try again");
        }

        return this;
    }

    public void disconnect() throws MessagingException {
        store.close();
    }

    public void sendTestEmail() throws MessagingException, UnsupportedEncodingException {
        String msgBody = "This is a test email generated by the Opencredo Test Framework!";

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("test@opencredo-testing.com", "OpenCredo tester"));
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(EMAIL_ADDRESS, "Test email recipient"));
        msg.setSubject(TEST_EMAIL_SUBJECT);
        msg.setText(msgBody);
        Transport.send(msg);
    }

    public void deleteTestEmails() throws MessagingException {
        final Folder inbox = store.getFolder(INBOX_FOLDER);
        inbox.open(Folder.READ_WRITE);

        Arrays.asList(inbox.getMessages()).stream()
                .filter(this::isTestEmail)
                .forEach(this::deleteMessage);

        inbox.close(true);
    }

    public List<Message> getTestEmails() throws MessagingException {
        final Folder inbox = store.getFolder(INBOX_FOLDER);
        inbox.open(Folder.READ_ONLY);

        List<Message> messages = Arrays.asList(inbox.getMessages());

        messages = messages.stream()
                .limit(MAX_RECENT_MESSAGES_TO_SEARCH)
                .filter(this::isTestEmail)
                .collect(Collectors.toList());

        inbox.close(false);

        return messages;
    }


    private void deleteMessage(final Message message) {
        try {
            message.setFlag(Flags.Flag.DELETED, true);
        } catch (final MessagingException e) {
            e.printStackTrace();
        }
    }


    private boolean isTestEmail(Message message) {
        try {
            return message.getSubject().equals(TEST_EMAIL_SUBJECT);
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}
