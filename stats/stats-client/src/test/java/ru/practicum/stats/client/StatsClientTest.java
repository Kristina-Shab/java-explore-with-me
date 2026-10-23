package ru.practicum.stats.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsClientTest {

    @Mock
    private RestTemplate restTemplate;

    private StatsClient client;

    @BeforeEach
    void setUp() {
        client = new StatsClient(restTemplate);
    }

    @Test
    void testStatsClientHit() {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 6, 1, 12, 0, 0))
                .build();

        client.hit(hit);

        ArgumentCaptor<HttpEntity> entityCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).postForEntity(
                eq("/hit"),
                entityCaptor.capture(),
                eq(Void.class)
        );

        HttpEntity captured = entityCaptor.getValue();
        EndpointHitDto body = (EndpointHitDto) captured.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getApp()).isEqualTo("ewm-main-service");
        assertThat(body.getUri()).isEqualTo("/events");
        assertThat(body.getIp()).isEqualTo("192.168.0.1");
    }

    @Test
    void testStatsClientGetStatsBuildUrl() {
        ViewStats[] mockedResponse = {new ViewStats("ewm-main-service", "/events", 5L)};
        when(restTemplate.getForObject(anyString(), eq(ViewStats[].class)))
                .thenReturn(mockedResponse);

        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 2, 0, 0, 0);

        List<ViewStats> result = client.getStats(start, end, List.of("/events"), false);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForObject(uriCaptor.capture(), eq(ViewStats[].class));

        String url = uriCaptor.getValue();
        assertThat(url).startsWith("/stats?");
        assertThat(url).contains("start=2024-06-01 00:00:00");
        assertThat(url).contains("end=2024-06-02 00:00:00");
        assertThat(url).contains("uris=/events");
        assertThat(url).contains("unique=false");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getApp()).isEqualTo("ewm-main-service");
        assertThat(result.getFirst().getUri()).isEqualTo("/events");
        assertThat(result.getFirst().getHits()).isEqualTo(5L);
    }

    @Test
    void testStatsClientGetStatsUnique() {
        ViewStats[] mockedResponse = {
                new ViewStats("ewm-main-service", "/events", 2L)
        };
        when(restTemplate.getForObject(anyString(), eq(ViewStats[].class)))
                .thenReturn(mockedResponse);

        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 2, 0, 0, 0);

        client.getStats(start, end, null, true);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForObject(uriCaptor.capture(), eq(ViewStats[].class));

        String url = uriCaptor.getValue();
        assertThat(url).contains("unique=true");
        assertThat(url).doesNotContain("uris=/events");
    }

    @Test
    void testStatsClientGetStatsEmptyUrisOmitted() {
        ViewStats[] mockedResponse = {new ViewStats("ewm-main-service", "/events", 2L)};
        when(restTemplate.getForObject(anyString(), eq(ViewStats[].class)))
                .thenReturn(mockedResponse);

        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 2, 0, 0, 0);

        client.getStats(start, end, List.of(), false);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForObject(uriCaptor.capture(), eq(ViewStats[].class));

        String url = uriCaptor.getValue();
        assertThat(url).doesNotContain("/events");
    }

    @Test
    void testStatsClientGetStatsNullResponse() {
        when(restTemplate.getForObject(anyString(), eq(ViewStats[].class)))
                .thenReturn(null);

        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 2, 0, 0, 0);

        List<ViewStats> result = client.getStats(start, end, List.of("/events"), false);

        assertThat(result).isEmpty();
    }
}
