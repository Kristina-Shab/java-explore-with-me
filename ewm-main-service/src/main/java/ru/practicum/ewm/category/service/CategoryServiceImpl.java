package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryDto create(NewCategoryDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Категория с именем '" + dto.getName() + "' уже существует");
        }

        Category category = CategoryMapper.toEntity(dto);
        Category savedCategory = categoryRepository.save(category);
        log.info("Создана категория: id={}, name={}", savedCategory.getId(), savedCategory.getName());

        return CategoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto update(Long catId, CategoryDto dto) {
        Category category = findCategoryById(catId);

        if (!category.getName().equals(dto.getName())
                && categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Категория с именем '" + dto.getName() + "' уже существует");
        }

        CategoryMapper.updateEntity(category, dto);
        log.info("Обновлена категория: id={}, name={}", category.getId(), category.getName());

        return CategoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public void delete(Long catId) {
        Category category = findCategoryById(catId);

        // TODO: добавить проверку на наличие событий:

        categoryRepository.delete(category);
        log.info("Удалена категория: id={}", catId);
    }

    @Override
    public List<CategoryDto> getAll(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);

        return categoryRepository.findAll(pageable).stream()
                .map(CategoryMapper::toDto)
                .toList();
    }

    @Override
    public CategoryDto getById(Long catId) {
        Category category = findCategoryById(catId);

        return CategoryMapper.toDto(category);
    }

    private Category findCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));
    }
}
