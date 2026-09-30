package com.rentalert.notification_ms.service.strategy;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.rentalert.common.exception.AppException;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.notification_ms.service.NotificationStrategy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;


@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationStrategy implements NotificationStrategy {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED,
                UTF_8.name()
            );
            Context context = new Context();
            Map<String, Object> variables = new HashMap<>();
            if (notificationDto.getTemplateParams() != null) {
                variables.putAll(notificationDto.getTemplateParams());
            }
            variables.put("message", notificationDto.getContent());
            context.setVariables(variables);
            mimeMessageHelper.setFrom(from);
            mimeMessageHelper.setTo(notificationDto.getReceiver());
            mimeMessageHelper.setSubject("RentAlert");
            String process = templateEngine.process("email/" + notificationDto.getTemplateName(), context);
            mimeMessageHelper.setText(process, true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new AppException(e);
        }
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.EMAIL;
    }
}
