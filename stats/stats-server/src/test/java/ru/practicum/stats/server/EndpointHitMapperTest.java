package ru.practicum.stats.server;

import org.junit.jupiter.api.Test;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.server.mapper.EndpointHitMapper;
import ru.practicum.stats.server.model.EndpointHit;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointHitMapperTest {

    @Test
    void testEndpointHitMapperToEntity() {
        LocalDateTime timestamp = LocalDateTime.of(2024, 6, 1, 12, 0, 0);
        EndpointHitDto dto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events")
                .ip("192.168.0.1")
                .timestamp(timestamp)
                .build();

        EndpointHit entity = EndpointHitMapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getApp()).isEqualTo("ewm-main-service");
        assertThat(entity.getUri()).isEqualTo("/events");
        assertThat(entity.getIp()).isEqualTo("192.168.0.1");
        assertThat(entity.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void testEndpointHitMapperIgnoreId() {
        EndpointHitDto dto = EndpointHitDto.builder()
                .id(999L)
                .app("ewm-main-service")
                .uri("/events")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 6, 1, 12, 0, 0))
                .build();

        EndpointHit entity = EndpointHitMapper.toEntity(dto);

        assertThat(entity.getId()).isNull();
    }
}
