package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.validation.EventValidatorRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestRepository requestRepository;
    private final EventValidatorRequest validator;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User requester = validator.findUserById(userId);
        Event event = validator.findEventById(eventId);
        checkCreateRequest(event, userId, eventId);
        RequestStatus initialStatus = determineInitialStatus(event);
        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(requester)
                .status(initialStatus)
                .created(LocalDateTime.now())
                .build();
        ParticipationRequest saved = requestRepository.save(request);
        log.info("Создана заявка: id={}, eventId={}, userId={}, status={}",
                saved.getId(), eventId, userId, initialStatus);
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        validator.findUserById(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository
                .findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Заявка с id=" + requestId + " не найдена"));
        request.setStatus(RequestStatus.CANCELED);
        log.info("Отменена заявка: id={}, userId={}", requestId, userId);
        return ParticipationRequestMapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        validator.findUserById(userId);
        Event event = validator.findEventById(eventId);
        validator.checkEventInitiator(event, userId);
        return requestRepository.findAllByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId,
                                                              Long eventId,
                                                              EventRequestStatusUpdateRequest dto) {
        validator.findUserById(userId);
        Event event = validator.findEventById(eventId);
        validator.checkEventInitiator(event, userId);
        checkUpdateStatus(dto.getStatus());
        List<ParticipationRequest> requests = findAllRequestedByEvent(eventId, dto.getRequestIds());
        checkAllPending(requests);
        EventRequestStatusUpdateResult result = dto.getStatus() == RequestStatus.CONFIRMED
                ? confirmRequests(event, requests)
                : rejectRequests(requests);
        log.info("Обновлены статусы заявок: eventId={}, requested={}, confirmed={}, rejected={}",
                eventId, requests.size(),
                result.getConfirmedRequests().size(), result.getRejectedRequests().size());
        return result;
    }

    private void checkCreateRequest(Event event, Long userId, Long eventId) {
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Нельзя подать заявку на своё событие");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Заявка на это событие уже существует");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (event.getParticipantLimit() > 0) {
            long confirmed = requestRepository.countByEventIdAndStatus(
                    eventId, RequestStatus.CONFIRMED);
            if (confirmed >= event.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит участников");
            }
        }
    }

    private RequestStatus determineInitialStatus(Event event) {
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            return RequestStatus.CONFIRMED;
        }
        return RequestStatus.PENDING;
    }

    private void checkUpdateStatus(RequestStatus status) {
        if (status != RequestStatus.CONFIRMED && status != RequestStatus.REJECTED) {
            throw new ValidationException("Статус может быть только CONFIRMED или REJECTED");
        }
    }

    private void checkAllPending(List<ParticipationRequest> requests) {
        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException(
                        "Заявка с id=" + request.getId() + " не находится в статусе PENDING");
            }
        }
    }

    private EventRequestStatusUpdateResult confirmRequests(Event event, List<ParticipationRequest> requests) {

        if (event.getParticipantLimit() == 0) {
            return doConfirm(event, requests, false);
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);

        if (confirmedCount + requests.size() > event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        boolean willReachLimit = (confirmedCount + requests.size()) == event.getParticipantLimit();
        return doConfirm(event, requests, willReachLimit);
    }

    private EventRequestStatusUpdateResult doConfirm(Event event,
                                                     List<ParticipationRequest> requests,
                                                     boolean autoRejectRemaining) {
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            request.setStatus(RequestStatus.CONFIRMED);
            confirmed.add(ParticipationRequestMapper.toDto(request));
        }

        if (autoRejectRemaining) {
            List<ParticipationRequest> pending = requestRepository
                    .findAllByEventIdAndStatus(event.getId(), RequestStatus.PENDING);

            for (ParticipationRequest request : pending) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(ParticipationRequestMapper.toDto(request));
            }
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    private EventRequestStatusUpdateResult rejectRequests(List<ParticipationRequest> requests) {
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            request.setStatus(RequestStatus.REJECTED);
            rejected.add(ParticipationRequestMapper.toDto(request));
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of())
                .rejectedRequests(rejected)
                .build();
    }

    private List<ParticipationRequest> findAllRequestedByEvent(Long eventId, List<Long> requestIds) {
        List<ParticipationRequest> requests = requestRepository.findAllByIdInAndEventId(requestIds, eventId);
        if (requests.size() != requestIds.size()) {
            throw new NotFoundException("Некоторые заявки не найдены или не принадлежат событию");
        }
        return requests;
    }
}
