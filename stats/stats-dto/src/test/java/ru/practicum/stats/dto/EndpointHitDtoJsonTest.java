package ru.practicum.stats.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointHitDtoJsonTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Test
    void testEndpointHitDtoSerialize() throws Exception {
        EndpointHitDto dto = EndpointHitDto.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 6, 1, 12, 0, 0))
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json)
                .contains("\"app\":\"ewm-main-service\"")
                .contains("\"uri\":\"/events\"")
                .contains("\"ip\":\"192.168.0.1\"")
                .contains("\"timestamp\":\"2024-06-01 12:00:00\"");
    }

    @Test
    void testEndpointHitDtoDeserialize() throws Exception {
        String json = "{\"app\":\"ewm-main-service\",\"uri\":" +
                "\"/events\",\"ip\":\"192.168.0.1\",\"timestamp\":\"2024-06-01 12:00:00\"}";

        EndpointHitDto dto = objectMapper.readValue(json, EndpointHitDto.class);

        assertThat(dto.getApp()).isEqualTo("ewm-main-service");
        assertThat(dto.getUri()).isEqualTo("/events");
        assertThat(dto.getIp()).isEqualTo("192.168.0.1");
        assertThat(dto.getTimestamp()).isEqualTo(LocalDateTime.of(2024, 6, 1, 12, 0, 0));
    }
}
