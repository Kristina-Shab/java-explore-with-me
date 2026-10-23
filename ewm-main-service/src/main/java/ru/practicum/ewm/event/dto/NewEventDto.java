package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.ewm.event.model.Location;

import java.time.LocalDateTime;

import static ru.practicum.ewm.util.Constants.DATE_TIME_PATTERN;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {
    @NotBlank
    @Size(min = 20, max = 2000, message = "должно быть от 20 до 2000 символов")
    private String annotation;

    @NotNull
    private Long category;

    @NotBlank
    @Size(min = 20, max = 7000, message = "должно быть от 20 до 7000 символов")
    private String description;

    @NotNull
    @JsonFormat(pattern = DATE_TIME_PATTERN)
    private LocalDateTime eventDate;

    @NotNull
    private Location location;
    private Boolean paid = false;

    @PositiveOrZero
    private Integer participantLimit = 0;
    private Boolean requestModeration = true;

    @NotBlank
    @Size(min = 3, max = 120, message = "должно быть от 3 до 120 символов")
    private String title;
}
