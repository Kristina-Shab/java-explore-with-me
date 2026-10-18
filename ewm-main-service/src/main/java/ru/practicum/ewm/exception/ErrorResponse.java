package ru.practicum.ewm.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.ewm.util.Constants.DATE_TIME_PATTERN;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {
    private final List<String> errors;
    private final String message;
    private final String reason;
    private final String status;
    @JsonFormat(pattern = DATE_TIME_PATTERN)
    private final LocalDateTime timestamp;
}
