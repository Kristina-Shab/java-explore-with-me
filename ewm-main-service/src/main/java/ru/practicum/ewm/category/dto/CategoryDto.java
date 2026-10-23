package ru.practicum.ewm.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;

    @NotBlank
    @Size(min = 1, max = 50, message = "должно быть от 1 до 50 символов")
    private String name;
}
