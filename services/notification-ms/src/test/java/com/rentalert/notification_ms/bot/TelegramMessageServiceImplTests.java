package com.rentalert.notification_ms.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.dto.profile.BotStatus;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.notification_ms.config.ApplicationProperties;
import com.rentalert.notification_ms.domain.TelegramMessage;
import com.rentalert.notification_ms.messaging.UserEventPublisher;
import com.rentalert.notification_ms.repository.TelegramMessageRepository;
import com.rentalert.notification_ms.service.BotBlockedService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

class TelegramMessageServiceImplTests {

    private final TelegramMessageRepository repository = mock(TelegramMessageRepository.class);
    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final UserEventPublisher userEventPublisher = mock(UserEventPublisher.class);
    private final BotBlockedService botBlockedService = mock(BotBlockedService.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private TelegramMessageServiceImpl service;

    private static UserProfileResponse profile(BotStatus botStatus) {
        return new UserProfileResponse(5L, null, null, botStatus, null, null, null, null, null,
            null, null, null, null, null, null);
    }

    private static TelegramCreateMessage message(TelegramCommand command) {
        return TelegramCreateMessage.builder()
            .chatId("100")
            .telegramId("100")
            .text("/" + command.name().toLowerCase())
            .command(command)
            .build();
    }

    @BeforeEach
    void setUp() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getTelegram().setWebAppUrl("https://app.example");
        OpenAppKeyboardFactory openApp = new OpenAppKeyboardFactory(properties);
        service = new TelegramMessageServiceImpl(repository, identityMsService, userEventPublisher, botBlockedService,
            openApp, transactionTemplate, List.of(new TelegramHelpCommandExecutor(), new TelegramAppCommandExecutor(openApp)));
        service.init();
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
            invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void commandIsSavedAndAnswered() {
        when(identityMsService.findByTelegramId("100")).thenReturn(Optional.of(profile(BotStatus.ACTIVE)));

        SendMessage reply = service.processMessage(message(TelegramCommand.HELP));

        assertThat(reply.getChatId()).isEqualTo("100");
        assertThat(reply.getText()).contains("/start");
        ArgumentCaptor<TelegramMessage> saved = ArgumentCaptor.forClass(TelegramMessage.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(5L);
        assertThat(saved.getValue().getCommand()).isEqualTo(TelegramCommand.HELP);
        verify(userEventPublisher).publishBotCommand(5L, TelegramCommand.HELP);
        verify(userEventPublisher, never()).publishBotUnblocked(any());
    }

    @Test
    void messageFromBlockedUserUnblocksBot() {
        when(identityMsService.findByTelegramId("100")).thenReturn(Optional.of(profile(BotStatus.BLOCKED)));

        service.processMessage(message(TelegramCommand.HELP));

        verify(userEventPublisher).publishBotUnblocked(5L);
    }

    @Test
    void unregisteredUserIsSentToMiniApp() {
        when(identityMsService.findByTelegramId("100")).thenReturn(Optional.empty());

        SendMessage reply = service.processMessage(message(TelegramCommand.HELP));

        assertThat(reply.getText()).isEqualTo("Перейдите в miniapp");
        assertThat(((InlineKeyboardMarkup) reply.getReplyMarkup()).getKeyboard().getFirst().getFirst().getWebApp().getUrl())
            .isEqualTo("https://app.example");
        verifyNoInteractions(repository, userEventPublisher);
    }

    @Test
    void appCommandOpensMiniApp() {
        when(identityMsService.findByTelegramId("100")).thenReturn(Optional.of(profile(BotStatus.ACTIVE)));

        SendMessage reply = service.processMessage(message(TelegramCommand.APP));

        assertThat(((InlineKeyboardMarkup) reply.getReplyMarkup()).getKeyboard().getFirst().getFirst().getWebApp().getUrl())
            .isEqualTo("https://app.example");
    }

    @Test
    void blockedBotIsReportedOnce() {
        when(identityMsService.findByTelegramId("100")).thenReturn(Optional.of(profile(BotStatus.ACTIVE)));
        when(identityMsService.findByTelegramId("200")).thenReturn(Optional.of(profile(BotStatus.BLOCKED)));

        service.handleBotBlocked("100");
        service.handleBotBlocked("200");

        verify(botBlockedService).handle(5L);
    }
}
