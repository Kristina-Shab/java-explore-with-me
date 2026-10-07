package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.server.mapper.EndpointHitMapper;
import ru.practicum.stats.server.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final EndpointHitRepository repository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hit) {
        repository.save(EndpointHitMapper.toEntity(hit));
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Начало интервала не может быть позже окончания");
        }
        List<String> normalizedUris =
                (uris == null || uris.isEmpty()) ? null : uris;

        return unique
                ? repository.findUniqueStats(start, end, normalizedUris)
                : repository.findAllStats(start, end, normalizedUris);
    }
}
