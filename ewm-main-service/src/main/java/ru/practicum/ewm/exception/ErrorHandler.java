package ru.practicum.ewm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class ErrorHandler {
    private static final String REASON_BAD_REQUEST = "Запрос составлен некорректно.";
    private static final String REASON_NOT_FOUND = "Объект не найден.";
    private static final String REASON_CONFLICT = "Нарушение целостности данных.";
    private static final String REASON_INTERNAL = "Внутренняя ошибка сервера.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> String.format("Поле: %s. Ошибка: %s. Значение: %s",
                        fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .toList();

        String message = errors.isEmpty() ? "Ошибка валидации" : errors.getFirst();

        return new ErrorResponse(
                errors,
                message,
                REASON_BAD_REQUEST,
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(ValidationException e) {
        return new ErrorResponse(
                List.of(),
                e.getMessage(),
                REASON_BAD_REQUEST,
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format(
                "Не удалось преобразовать значение типа '%s' к требуемому типу '%s'",
                e.getValue(), e.getRequiredType());
        return new ErrorResponse(
                List.of(message),
                message,
                REASON_BAD_REQUEST,
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParams(MissingServletRequestParameterException e) {
        String message = String.format("Обязательный параметр '%s' отсутствует", e.getParameterName());
        return new ErrorResponse(
                List.of(message),
                message,
                REASON_BAD_REQUEST,
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException e) {
        return new ErrorResponse(
                List.of(),
                e.getMessage(),
                REASON_BAD_REQUEST,
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NotFoundException e) {
        return new ErrorResponse(
                List.of(),
                e.getMessage(),
                REASON_NOT_FOUND,
                HttpStatus.NOT_FOUND.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(ConflictException e) {
        return new ErrorResponse(
                List.of(),
                e.getMessage(),
                REASON_CONFLICT,
                HttpStatus.CONFLICT.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception e) {

        String message = e.getMessage() == null ? "Неизвестная ошибка" : e.getMessage();
        return new ErrorResponse(
                List.of(message),
                message,
                REASON_INTERNAL,
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                LocalDateTime.now()
        );
    }
}
