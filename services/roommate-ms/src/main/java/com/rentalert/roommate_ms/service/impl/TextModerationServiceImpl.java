package com.rentalert.roommate_ms.service.impl;

import com.rentalert.roommate_ms.dto.ModerationVerdict;
import com.rentalert.roommate_ms.service.TextModerationService;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TextModerationServiceImpl implements TextModerationService {

    private static final Pattern PHONE = Pattern.compile("(?:\\+375|\\+7|\\+380|8)?[\\s(-]*\\d{2,3}[\\s)-]*\\d{3}[\\s-]*\\d{2}[\\s-]*\\d{2}");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}");
    private static final Pattern URL = Pattern.compile("(https?://|www\\.)\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern HANDLE = Pattern.compile("@[a-zA-Z][a-zA-Z0-9_]{3,}");

    private static final List<String> CONTACT_WORDS = List.of(
        "телеграм", "telegram", "тг ", " тг,", "whatsapp", "вотсап", "ватсап",
        "viber", "вайбер", "инстаграм", "instagram", "vkontakte", "вконтакте", "vk.com");

    private final List<String> bannedWords;

    public TextModerationServiceImpl(
        @Value("#{'${app.moderation.banned-words:казино,заработок без вложений,ставки на спорт}'.split(',')}") List<String> bannedWords
    ) {
        this.bannedWords = bannedWords.stream()
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(word -> word.toLowerCase(Locale.ROOT))
            .toList();
    }

    @Override
    public ModerationVerdict check(String text) {
        if (!StringUtils.hasText(text)) {
            return ModerationVerdict.approved();
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        if (bannedWords.stream().anyMatch(normalized::contains)) {
            return ModerationVerdict.rejected("Текст содержит недопустимые слова или выражения.");
        }
        if (looksLikeContactInfo(text, normalized)) {
            return ModerationVerdict.pending("Похоже на контактные данные — отправлено на проверку модератору.");
        }
        return ModerationVerdict.approved();
    }

    private boolean looksLikeContactInfo(String original, String normalized) {
        return PHONE.matcher(original).find()
            || EMAIL.matcher(original).find()
            || URL.matcher(original).find()
            || HANDLE.matcher(original).find()
            || CONTACT_WORDS.stream().anyMatch(normalized::contains);
    }
}
