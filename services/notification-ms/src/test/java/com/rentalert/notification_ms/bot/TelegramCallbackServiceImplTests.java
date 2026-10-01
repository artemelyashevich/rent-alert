package com.rentalert.notification_ms.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.profile.BotStatus;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.notification_ms.messaging.UserEventPublisher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;

class TelegramCallbackServiceImplTests {

    private final TelegramCallbackExecutor executor = mock(TelegramCallbackExecutor.class);
    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final UserEventPublisher userEventPublisher = mock(UserEventPublisher.class);
    private TelegramCallbackServiceImpl service;

    private static CallbackQuery callback(String data) {
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData(data);
        callbackQuery.setFrom(new User(100L, "name", false));
        return callbackQuery;
    }

    @BeforeEach
    void setUp() {
        when(executor.getPrefix()).thenReturn("hide");
        service = new TelegramCallbackServiceImpl(List.of(executor), identityMsService, userEventPublisher);
        service.init();
    }

    @Test
    void callbackIsRoutedWithUserId() {
        TelegramCallbackResult expected = TelegramCallbackResult.of("ok", true);
        when(identityMsService.findByTelegramId("100")).thenReturn(Optional.of(
            new UserProfileResponse(5L, null, null, BotStatus.ACTIVE, null, null, null, null, null, null, null, null, null, null, null)));
        when(executor.execute(5L, "7")).thenReturn(expected);

        assertThat(service.processCallback(callback("hide:7"))).isSameAs(expected);
        verify(userEventPublisher).publishBotAction(UserEvent.TELEGRAM_CALLBACK, 5L);
    }

    @Test
    void unregisteredUserIsRejected() {
        when(identityMsService.findByTelegramId("100")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processCallback(callback("hide:7"))).isInstanceOf(AppException.class);
        verifyNoInteractions(userEventPublisher);
        verify(executor).getPrefix();
    }

    @Test
    void unknownPrefixIsRejected() {
        assertThatThrownBy(() -> service.processCallback(callback("unknown:7"))).isInstanceOf(AppException.class);
        verifyNoInteractions(identityMsService);
    }
}
