package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {
    private Set<Long> events = new HashSet<>();
    private Boolean pinned = false;

    @NotBlank
    @Size(min = 1, max = 50, message = "должно быть от 1 до 50 символов")
    private String title;
}
