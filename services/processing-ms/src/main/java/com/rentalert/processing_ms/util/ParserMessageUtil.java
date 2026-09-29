package com.rentalert.processing_ms.util;


import com.rentalert.processing_ms.dto.ParserResponseDto;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ParserMessageUtil {

    private static final String SEPARATOR = " · ";
    private static final String LINK_LABEL = "Смотреть объявление →";

    public static String buildMessage(ParserResponseDto dto) {
        List<String> header = new ArrayList<>();
        String headline = joinWith(" // ",
                dto.getLink() != null ? "<b>" + escape(dto.getSource().name()) + "</b>" : null,
                escape(dto.getAddress())
        );
        if (!headline.isEmpty()) {
            header.add(headline);
        }

        List<String> details = new ArrayList<>();
        String infoLine = joinWith(SEPARATOR,
                dto.getRooms() != null ? dto.getRooms() + "-комнатная" : null,
                dto.getArea() != null ? dto.getArea() + " м²" : null,
                (dto.getFloor() != null && dto.getTotalFloors() != null)
                        ? dto.getFloor() + "/" + dto.getTotalFloors() + " эт." : null
        );
        if (!infoLine.isEmpty()) {
            details.add("• " + infoLine);
        }

        String featuresLine = joinWith(SEPARATOR,
                escape(dto.getHouseType()),
                Boolean.TRUE.equals(dto.getFurnished()) ? "Мебель" : null
        );
        if (!featuresLine.isEmpty()) {
            details.add("• " + featuresLine);
        }

        List<String> summary = new ArrayList<>();
        String priceLine = joinWith(SEPARATOR,
                dto.getPriceUsd() != null ? dto.getPriceUsd().setScale(2, RoundingMode.HALF_UP) + " $" : null,
                dto.getPriceByn() != null ? dto.getPriceByn().setScale(2, RoundingMode.HALF_UP) + " BYN" : null
        );
        if (!priceLine.isEmpty()) {
            summary.add("<b>" + priceLine + "</b>");
        }
        if (dto.getContactName() != null) {
            summary.add(escape(dto.getContactName()));
        }
        if (dto.getLink() != null) {
            summary.add("<a href=\"" + escapeAttribute(dto.getLink()) + "\">" + LINK_LABEL + "</a>");
        }

        List<String> sections = new ArrayList<>();
        for (List<String> block : List.of(header, details, summary)) {
            if (!block.isEmpty()) {
                sections.add(String.join("\n", block));
            }
        }

        return String.join("\n\n", sections);
    }

    private static String joinWith(String separator, Object... values) {
        List<String> nonNull = new ArrayList<>();
        for (Object value : values) {
            if (value != null) {
                String s = value.toString();
                if (!s.isBlank()) {
                    nonNull.add(s);
                }
            }
        }
        return String.join(separator, nonNull);
    }

    private static String escape(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String escapeAttribute(String value) {
        return escape(value).replace("\"", "&quot;");
    }
}