package ru.practicum.stats.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.server.controller.StatsController;
import ru.practicum.stats.server.exception.ErrorHandler;
import ru.practicum.stats.server.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {StatsController.class, ErrorHandler.class})
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StatsService service;

    @Test
    void testStatsControllerHitCreated() throws Exception {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 6, 1, 12, 0, 0))
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hit)))
                .andExpect(status().isCreated());

        verify(service).saveHit(any(EndpointHitDto.class));
    }

    @Test
    void testStatsControllerHitInvalid() throws Exception {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app("")
                .uri("/events")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 6, 1, 12, 0, 0))
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    void testStatsControllerGetStats() throws Exception {
        List<ViewStats> stats = List.of(
                new ViewStats("ewm-main-service", "/events", 5L)
        );
        when(service.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(stats);

        mockMvc.perform(get("/stats")
                        .param("start", "2024-06-01 00:00:00")
                        .param("end", "2024-06-02 00:00:00")
                        .param("uris", "/events")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events"))
                .andExpect(jsonPath("$[0].hits").value(5));

        verify(service).getStats(any(), any(), anyList(), anyBoolean());
    }

    @Test
    void testStatsControllerGetStatsInvalidRange() throws Exception {
        doThrow(new IllegalArgumentException("start must not be after end"))
                .when(service).getStats(any(), any(), any(), anyBoolean());

        mockMvc.perform(get("/stats")
                        .param("start", "2024-06-02 00:00:00")
                        .param("end", "2024-06-01 00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.description").exists());
    }
}
