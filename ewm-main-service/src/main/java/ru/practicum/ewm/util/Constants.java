package ru.practicum.ewm.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static LocalDateTime statsRangeStart() {
        return LocalDateTime.now().minusYears(10);
    }

    public static LocalDateTime statsRangeEnd() {
        return LocalDateTime.now().plusYears(1);
    }
}
