package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        Set<Event> events = loadEvents(dto.getEvents());

        Compilation compilation = Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned())
                .events(events)
                .build();

        Compilation saved = compilationRepository.save(compilation);
        log.info("Создана подборка: id={}, title={}", saved.getId(), saved.getTitle());

        return buildDto(saved);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = findCompilationById(compId);
        setIfNotNull(dto.getTitle(), compilation::setTitle);
        setIfNotNull(dto.getPinned(), compilation::setPinned);
        if (dto.getEvents() != null) {
            Set<Event> events = loadEvents(dto.getEvents());
            compilation.setEvents(events);
        }
        log.info("Обновлена подборка: id={}, title={}", compilation.getId(), compilation.getTitle());
        return buildDto(compilation);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        Compilation compilation = findCompilationById(compId);
        compilationRepository.delete(compilation);
        log.info("Удалена подборка: id={}", compId);
    }

    @Override
    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Compilation> page = compilationRepository.findAllByPinned(pinned, pageable);

        List<Compilation> compilations = page.getContent();

        Set<Long> eventIds = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .map(Event::getId)
                .collect(Collectors.toSet());

        Map<Long, Long> views = getViewsForEvents(eventIds);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(eventIds);

        return compilations.stream()
                .map(c -> CompilationMapper.toDto(c, views, confirmedRequests))
                .toList();
    }

    @Override
    public CompilationDto getById(Long compId) {
        Compilation compilation = findCompilationById(compId);
        return buildDto(compilation);
    }

    private Compilation findCompilationById(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));
    }

    private Set<Event> loadEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(eventRepository.findAllById(eventIds));
    }

    private Map<Long, Long> getConfirmedRequestsForEvents(Set<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private Map<Long, Long> getViewsForEvents(Set<Long> eventIds) {
        return Map.of(); // TODO добавить State
    }

    private CompilationDto buildDto(Compilation compilation) {
        Set<Long> eventIds = compilation.getEvents().stream()
                .map(Event::getId)
                .collect(Collectors.toSet());

        Map<Long, Long> views = getViewsForEvents(eventIds);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(eventIds);

        return CompilationMapper.toDto(compilation, views, confirmedRequests);
    }

    private <T> void setIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
