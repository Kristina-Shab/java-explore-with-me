package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    private static final String REASON_BAD_REQUEST = "Incorrectly made request.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> String.format("Поле: %s. Ошибка: %s. Значение: %s",
                        fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .toList();
        String message = errors.isEmpty() ? "Ошибка валидации" : errors.getFirst();
        log.warn("400 Валидация тела запроса не пройдена. Найдено ошибок: {}. Первая: {}", errors.size(), message);

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
        log.warn("400 Бизнес-валидация не пройдена: {}", e.getMessage());
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
        log.warn("400 Ошибка типа параметра. Параметр: '{}'. Ожидался тип: '{}'. Получено значение: '{}'",
                e.getName(), e.getRequiredType(), e.getValue());
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
        log.warn("400 Отсутствует обязательный параметр: '{}'", e.getParameterName());
        return new ErrorResponse(
                List.of(message),
                message,
                REASON_BAD_REQUEST,
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("400 Не удалось прочитать тело запроса (JSON). Причина: {}", e.getMessage());
        return new ErrorResponse(
                List.of(),
                "Некорректный формат запроса. Проверьте JSON или значения полей.",
                REASON_BAD_REQUEST,
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException e) {
        log.warn("400 Некорректный запрос: {}", e.getMessage());
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
        log.warn("404 Объект не найден: {}", e.getMessage());
        return new ErrorResponse(
                List.of(),
                e.getMessage(),
                "The required object was not found.",
                HttpStatus.NOT_FOUND.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(ConflictException e) {
        log.warn("409 Бизнес-конфликт: {}", e.getMessage());
        return new ErrorResponse(
                List.of(),
                e.getMessage(),
                "For the requested operation the conditions are not met.",
                HttpStatus.CONFLICT.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("409 Нарушение ограничения БД: {}", e.getMostSpecificCause().getMessage());
        return new ErrorResponse(
                List.of(),
                "Integrity constraint has been violated.",
                e.getMostSpecificCause().getMessage(),
                HttpStatus.CONFLICT.name(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException e) {
        List<String> errors = e.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .toList();
        String message = errors.isEmpty() ? "Ошибка валидации параметров" : errors.getFirst();
        log.warn("400 Нарушение ограничений параметров. Ошибок: {}. Первая: {}", errors.size(), message);
        return new ErrorResponse(errors,
                message,
                REASON_BAD_REQUEST,
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now());
    }
}
