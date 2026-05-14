package top.xihale.xdocs.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import top.xihale.xdocs.config.WebConfig;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 邮件发送工具类
 */
public class EmailUtils {
    private static final Logger LOGGER = Logger.getLogger(EmailUtils.class.getName());

    private static final Properties SMTP_PROPS = new Properties();
    private static final Session SMTP_SESSION;

    static {
        SMTP_PROPS.put("mail.smtp.host", WebConfig.getMailSmtpHost());
        SMTP_PROPS.put("mail.smtp.port", String.valueOf(WebConfig.getMailSmtpPort()));
        SMTP_PROPS.put("mail.smtp.auth", "true");
        SMTP_PROPS.put("mail.smtp.connectiontimeout", "10000");
        SMTP_PROPS.put("mail.smtp.timeout", "10000");
        SMTP_PROPS.put("mail.smtp.writetimeout", "10000");
        if (WebConfig.isMailSslEnable()) {
            SMTP_PROPS.put("mail.smtp.ssl.enable", "true");
            SMTP_PROPS.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        SMTP_SESSION = Session.getInstance(SMTP_PROPS, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(WebConfig.getMailSmtpUsername(), WebConfig.getMailSmtpPassword());
            }
        });
    }

    private static final ExecutorService executor = Executors.newFixedThreadPool(2, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "email-sender-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    public static void sendCode(String toEmail, String code) {
        String subject = "【XDocs】验证码";
        String content = "您的验证码为：<b>" + code + "</b>，有效期为 5 分钟。请勿告诉他人。";
        sendEmailAsync(toEmail, subject, content);
    }

    private static void sendEmailAsync(String to, String subject, String content) {
        executor.submit(() -> {
            try {
                sendEmail(to, subject, content);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "发送邮件失败: to=" + to, e);
            }
        });
    }

    private static void sendEmail(String to, String subject, String content) throws MessagingException {
        Message message = new MimeMessage(SMTP_SESSION);
        message.setFrom(new InternetAddress(WebConfig.getMailFrom()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setContent(content, "text/html;charset=UTF-8");

        Transport.send(message);
    }
}
