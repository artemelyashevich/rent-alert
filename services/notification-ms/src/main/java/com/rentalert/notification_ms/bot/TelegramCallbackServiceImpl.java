package com.rentalert.notification_ms.bot;

import com.rentalert.common.exception.AppException;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramCallbackServiceImpl implements TelegramCallbackService {

    private final List<TelegramCallbackExecutor> executors;

    private Map<String, TelegramCallbackExecutor> executorsMap;

    @PostConstruct
    void init() {
        executorsMap = executors.stream().collect(Collectors.toMap(
            TelegramCallbackExecutor::getPrefix,
            Function.identity()
        ));
    }

    @Override
    public TelegramCallbackResult processCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        if (data == null || data.isBlank()) {
            throw new AppException("Empty callback data", HttpStatus.BAD_REQUEST);
        }

        String[] parts = data.split(":", 2);
        String prefix = parts[0];
        String payload = parts.length > 1 ? parts[1] : "";

        TelegramCallbackExecutor executor = executorsMap.get(prefix);
        if (executor == null) {
            throw new AppException("Unsupported callback", HttpStatus.BAD_REQUEST);
        }

        return executor.execute(callbackQuery, payload);
    }
}
