package ru.practicum.stats.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ViewStatsDtoJsonTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Test
    void testViewStatsDtoSerialize() throws Exception {
        ViewStats dto = new ViewStats("ewm-main-service", "/events", 5L);

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json)
                .contains("\"app\":\"ewm-main-service\"")
                .contains("\"uri\":\"/events\"")
                .contains("\"hits\":5");
    }

    @Test
    void testViewStatsDtoDeserialize() throws Exception {
        String json = "{\"app\":\"ewm-main-service\",\"uri\":\"/events\",\"hits\":5}";

        ViewStats dto = objectMapper.readValue(json, ViewStats.class);

        assertThat(dto.getApp()).isEqualTo("ewm-main-service");
        assertThat(dto.getUri()).isEqualTo("/events");
        assertThat(dto.getHits()).isEqualTo(5L);
    }
}
