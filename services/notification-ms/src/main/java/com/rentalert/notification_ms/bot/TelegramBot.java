package com.rentalert.notification_ms.bot;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.util.TextUtils;
import com.rentalert.notification_ms.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

@Slf4j
@Component
@ConditionalOnProperty(value = "app.telegram.bot-enabled", havingValue = "true")
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private static final int ERROR_CODE_FORBIDDEN = 403;
    private static final String BOT_BLOCKED_MARKER = "bot was blocked by the user";

    private final ApplicationProperties applicationProperties;
    private final TelegramMessageService telegramMessageService;
    private final TelegramCallbackService telegramCallbackService;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        log.info("Receive message from bot: {}", update.getMessage().getText());
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String command = message.getText().strip();
        TelegramCommand telegramCommand = TelegramCommand.getCommand(command);
        TelegramCreateMessage tgMessage = TelegramCreateMessage.builder()
            .text(message.getText())
            .command(telegramCommand)
            .chatId(chatId.toString())
            .telegramId(message.getFrom().getId().toString())
            .username(message.getFrom().getUserName())
            .build();
        try {
            SendMessage sendMessage = telegramMessageService.processMessage(tgMessage);
            sendMessage(sendMessage);
        } catch (AppException e) {
            log.warn("Business error processing telegram update: {}", e.getMessage());
            sendMessage(new SendMessage(chatId.toString(), "Упс! Произошла ошибка: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Critical error processing telegram update", e);
            sendMessage(new SendMessage(chatId.toString(), "Внутренняя ошибка сервера."));
        }
    }

    @Override
    public String getBotUsername() {
        return applicationProperties.getTelegram().getBotName();
    }

    @Override
    public String getBotToken() {
        return applicationProperties.getTelegram().getBotToken();
    }

    public Message sendMessage(SendMessage message) {
        if (message == null || message.getText() == null || message.getText().isBlank()) {
            return null;
        }
        message.setText(TextUtils.deleteLastDots(message.getText()));
        try {
            return execute(message);
        } catch (TelegramApiRequestException e) {
            if (isBotBlockedByUser(e)) {
                log.warn("Bot was blocked by user, chatId={}", message.getChatId());
                telegramMessageService.handleBotBlocked(message.getChatId());
                return null;
            }
            log.error("Telegram API error, chatId={}, errorCode={}", message.getChatId(), e.getErrorCode(), e);
            throw new AppException("Telegram error", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (TelegramApiException e) {
            log.error("Telegram send error", e);
            throw new AppException("Telegram error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public void pinMessage(String chatId, Integer messageId, boolean disableNotification) {
        if (chatId == null || messageId == null) {
            return;
        }
        try {
            execute(PinChatMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .disableNotification(disableNotification)
                .build());
        } catch (TelegramApiException e) {
            log.warn("Failed to pin message {} in chat {}: {}", messageId, chatId, e.getMessage());
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        log.info("Receive callback query from bot: {}", callbackQuery.getData());
        TelegramCallbackResult result;
        try {
            result = telegramCallbackService.processCallback(callbackQuery);
        } catch (Exception e) {
            log.warn("Failed to process callback query: {}", callbackQuery.getData(), e);
            result = TelegramCallbackResult.of("Не удалось выполнить действие", false);
        }
        answerCallback(callbackQuery, result);
    }

    private void answerCallback(CallbackQuery callbackQuery, TelegramCallbackResult result) {
        try {
            execute(AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .text(result.answerText())
                .build());
            if (result.deleteMessage() && callbackQuery.getMessage() != null) {
                execute(DeleteMessage.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .build());
            } else if (result.updatedMarkup() != null && callbackQuery.getMessage() != null) {
                execute(EditMessageReplyMarkup.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .replyMarkup(result.updatedMarkup())
                    .build());
            }
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query or delete message", e);
        }
    }

    private boolean isBotBlockedByUser(TelegramApiRequestException e) {
        return e.getErrorCode() != null
            && e.getErrorCode() == ERROR_CODE_FORBIDDEN
            && e.getApiResponse() != null
            && e.getApiResponse().toLowerCase().contains(BOT_BLOCKED_MARKER);
    }
}