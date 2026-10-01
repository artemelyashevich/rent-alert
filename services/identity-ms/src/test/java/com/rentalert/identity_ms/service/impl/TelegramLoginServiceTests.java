package com.rentalert.identity_ms.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.util.auth.JwtService;
import com.rentalert.common.util.auth.JwtType;
import com.rentalert.dto.auth.TelegramAuthRequest;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.profile.TelegramUser;
import com.rentalert.dto.user.UserSession;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.domain.UserSettings;
import com.rentalert.identity_ms.messaging.UserEventPublisher;
import com.rentalert.identity_ms.service.TelegramInitDataParser;
import com.rentalert.identity_ms.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class TelegramLoginServiceTests {

    private final UserProfileService userProfileService = mock(UserProfileService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final TelegramInitDataParser parser = mock(TelegramInitDataParser.class);
    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final UserEventPublisher userEventPublisher = mock(UserEventPublisher.class);
    private final TelegramLoginService service = new TelegramLoginService(
        userProfileService, jwtService, parser, tokenIssuer, transactionTemplate, userEventPublisher);

    @Test
    void loginIsSentToStatistics() {
        TelegramUser telegramUser = TelegramUser.builder().id(100L).build();
        UserProfile profile = new UserProfile();
        profile.setId(5L);
        profile.setSettings(UserSettings.builder().build());
        UserSession session = UserSession.builder().userId(5L).build();
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
            invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        when(parser.verify("init")).thenReturn(telegramUser);
        when(userProfileService.findOrRegister(telegramUser, "100")).thenReturn(profile);
        when(tokenIssuer.toSession(profile)).thenReturn(session);
        when(jwtService.generateToken(session, JwtType.REFRESH)).thenReturn("refresh");

        service.auth(new TelegramAuthRequest("init"));

        verify(userEventPublisher).publish(UserEvent.LOGIN_SUCCESS, 5L);
    }
}
