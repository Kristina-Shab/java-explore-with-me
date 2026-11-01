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
import ru.practicum.ewm.event.service.EventCommentService;
import ru.practicum.ewm.event.service.EventStatsService;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventStatsService eventStatsService;
    private final EventCommentService eventCommentService;

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
            if (!dto.getEvents().isEmpty()) {
                List<Event> events = eventRepository.findAllById(dto.getEvents());
                if (events.size() != dto.getEvents().size()) {
                    throw new NotFoundException("Некоторые события не найдены");
                }
                compilation.setEvents(new HashSet<>(events));
            } else {
                compilation.setEvents(new HashSet<>());
            }
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

        List<Long> eventIds = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .map(Event::getId)
                .distinct()
                .toList();

        Map<Long, Long> views = eventStatsService.getViewsForEvents(eventIds, false);
        Map<Long, Long> confirmedRequests = eventStatsService.getConfirmedRequestsForEvents(eventIds);
        Map<Long, Long> commentCounts = eventCommentService.getCommentCountsForEvents(eventIds);

        return compilations.stream()
                .map(c -> CompilationMapper.toDto(c, views, confirmedRequests, commentCounts))
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
        List<Event> events = eventRepository.findAllById(eventIds);
        if (events.size() != eventIds.size()) {
            throw new ValidationException("Переданы несуществующие события");
        }
        return new HashSet<>(events);
    }

    private CompilationDto buildDto(Compilation compilation) {
        List<Long> eventIds = compilation.getEvents().stream()
                .map(Event::getId)
                .distinct()
                .toList();

        Map<Long, Long> views = eventStatsService.getViewsForEvents(eventIds, false);
        Map<Long, Long> confirmedRequests = eventStatsService.getConfirmedRequestsForEvents(eventIds);
        Map<Long, Long> commentCounts = eventCommentService.getCommentCountsForEvents(eventIds);

        return CompilationMapper.toDto(compilation, views, confirmedRequests, commentCounts);
    }

    private <T> void setIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
