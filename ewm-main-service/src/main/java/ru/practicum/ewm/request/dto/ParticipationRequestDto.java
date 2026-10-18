package ru.practicum.ewm.request.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.ewm.request.model.RequestStatus;

import java.time.LocalDateTime;

import static ru.practicum.ewm.util.Constants.DATE_TIME_PATTERN;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {
    private Long id;
    private Long event;
    private Long requester;
    private RequestStatus status;

    @JsonFormat(pattern = DATE_TIME_PATTERN)
    private LocalDateTime created;
}
