package ru.practicum.ewm.compilation.service;

import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    CompilationDto create(NewCompilationDto dto);

    CompilationDto update(Long compId, UpdateCompilationRequest dto);

    void delete(Long compId);

    List<CompilationDto> getAll(Boolean pinned, int from, int size);

    CompilationDto getById(Long compId);
}
