package com.rentalert.notification_ms.bot;

import com.rentalert.common.exception.AppException;
import com.rentalert.dto.profile.BotStatus;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.notification_ms.domain.TelegramMessage;
import com.rentalert.notification_ms.messaging.UserEventPublisher;
import com.rentalert.notification_ms.repository.TelegramMessageRepository;
import com.rentalert.notification_ms.service.BotBlockedService;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageServiceImpl implements TelegramMessageService {

    private static final String AUTH_REQUIRED_TEXT = "Перейдите в miniapp";

    private final TelegramMessageRepository telegramMessageRepository;
    private final IdentityMsService identityMsService;
    private final UserEventPublisher userEventPublisher;
    private final BotBlockedService botBlockedService;
    private final OpenAppKeyboardFactory openAppKeyboardFactory;
    private final TransactionTemplate transactionTemplate;
    private final List<TelegramCommandExecutor> commands;

    private Map<TelegramCommand, TelegramCommandExecutor> executorsMap;

    @PostConstruct
    void init() {
        executorsMap = commands.stream().collect(Collectors.toMap(
            TelegramCommandExecutor::getCommand,
            Function.identity()
        ));
    }

    @Override
    public SendMessage processMessage(TelegramCreateMessage telegramCreateMessage) {
        Optional<UserProfileResponse> profile = identityMsService.findByTelegramId(telegramCreateMessage.getTelegramId());
        if (profile.isEmpty()) {
            log.debug("Unregistered telegram user attempted command {}", telegramCreateMessage.getCommand());
            return SendMessage.builder()
                .chatId(telegramCreateMessage.getChatId())
                .text(AUTH_REQUIRED_TEXT)
                .replyMarkup(openAppKeyboardFactory.build())
                .build();
        }

        TelegramCommandExecutor executor = executorsMap.get(telegramCreateMessage.getCommand());
        if (executor == null) {
            throw new AppException("Unsupported command", HttpStatus.BAD_REQUEST);
        }

        Long userId = profile.get().userId();
        TelegramMessage telegramMessage = transactionTemplate.execute(status -> {
            if (profile.get().botStatus() == BotStatus.BLOCKED) {
                userEventPublisher.publishBotUnblocked(userId);
                log.info("Bot was unblocked by user, userId={}", userId);
            }
            userEventPublisher.publishBotCommand(userId, telegramCreateMessage.getCommand());
            return telegramMessageRepository.save(TelegramMessage.builder()
                .text(telegramCreateMessage.getText())
                .userId(userId)
                .command(telegramCreateMessage.getCommand())
                .build());
        });
        return executor.execute(telegramMessage, telegramCreateMessage.getChatId());
    }

    @Override
    public void handleBotBlocked(@NonNull String chatId) {
        Optional<UserProfileResponse> profile = identityMsService.findByTelegramId(chatId);
        if (profile.isEmpty()) {
            log.warn("Bot blocked by unregistered chat");
            return;
        }
        if (profile.get().botStatus() != BotStatus.BLOCKED) {
            botBlockedService.handle(profile.get().userId());
        }
    }
}
