package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.model.UserStateAction;

import java.time.LocalDateTime;

import static ru.practicum.ewm.util.Constants.DATE_TIME_PATTERN;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventUserRequest {
    @Size(min = 20, max = 2000, message = "должно быть от 20 до 2000 символов")
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000, message = "должно быть от 20 до 7000 символов")
    private String description;

    @JsonFormat(pattern = DATE_TIME_PATTERN)
    private LocalDateTime eventDate;

    private Location location;

    private Boolean paid;

    @PositiveOrZero
    private Integer participantLimit;

    private Boolean requestModeration;

    private UserStateAction stateAction;

    @Size(min = 3, max = 120, message = "должно быть от 3 до 120 символов")
    private String title;
}
