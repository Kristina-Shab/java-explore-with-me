package ru.practicum.ewm.user.dto;

import jakarta.validation.constraints.Email;
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
public class NewUserRequest {

    @NotBlank
    @Email
    @Size(min = 6, max = 254, message = "должен быть от 6 до 254 символов")
    private String email;

    @NotBlank
    @Size(min = 2, max = 250, message = "должно быть от 2 до 250 символов")
    private String name;
}
