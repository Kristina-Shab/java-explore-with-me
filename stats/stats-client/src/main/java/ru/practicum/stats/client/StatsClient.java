package ru.practicum.stats.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatsClient {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate rest;
    private final String serverUrl;

    @Autowired
    public StatsClient(@Value("${stats-server.url}") String serverUrl) {
        this.rest = new RestTemplate();
        this.serverUrl = serverUrl;
    }

    public StatsClient(String serverUrl, RestTemplate rest) {
        this.serverUrl = serverUrl;
        this.rest = rest;
    }

    public void hit(EndpointHitDto hit) {
        HttpEntity<EndpointHitDto> request = new HttpEntity<>(hit);
        rest.postForEntity(serverUrl + "/hit", request, Void.class);
    }

    public List<ViewStats> getStats(LocalDateTime start,
                                    LocalDateTime end,
                                    List<String> uris,
                                    boolean unique) {
        String url = UriComponentsBuilder.fromUriString(serverUrl + "/stats")
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER))
                .queryParam("uris", uris)
                .queryParam("unique", unique)
                .build()
                .encode()
                .toUriString();

        ViewStats[] response = rest.getForObject(URI.create(url), ViewStats[].class);

        if (response == null) {
            return List.of();
        }
        return List.of(response);
    }
}
