package com.example.taskmanager.dto;

import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 100, message = "El titulo no puede superar los 100 caracteres")
        String title,

        @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
        String description,

        TaskStatus status,

        @NotNull(message = "La prioridad es obligatoria")
        TaskPriority priority,

        @NotNull(message = "La fecha limite es obligatoria")
        LocalDate dueDate
) {
}
