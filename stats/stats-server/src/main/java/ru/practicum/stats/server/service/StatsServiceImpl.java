package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.server.mapper.EndpointHitMapper;
import ru.practicum.stats.server.model.EndpointHit;
import ru.practicum.stats.server.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final EndpointHitRepository repository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hit) {
        log.debug("Сохранение записи: app={}, uri={}", hit.getApp(), hit.getUri());
        EndpointHit endpointHit = EndpointHitMapper.toEntity(hit);
        repository.save(endpointHit);
        log.info("Запись сохранена: uri={}", hit.getUri());
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Некорректный интервал статистики: start= " + start + ", end=" + end);
        }

        List<String> normalizedUris = normalizeUris(uris);

        List<ViewStats> stats = unique
                ? repository.findUniqueStats(start, end, normalizedUris)
                : repository.findAllStats(start, end, normalizedUris);
        log.info("Статистика сформирована: {} записей (unique={})", stats.size(), unique);
        return stats;
    }

    private List<String> normalizeUris(List<String> uris) {
        if (uris == null || uris.isEmpty()) {
            return null;
        }
        List<String> normalized = uris.stream()
                .filter(StringUtils::hasText)
                .toList();
        return normalized.isEmpty() ? null : normalized;
    }
}
