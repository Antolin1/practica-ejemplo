package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.exception.InvalidTaskDataException;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository);
    }

    @Test
    void create_conFechaLimiteEnElPasado_lanzaExcepcion() {
        TaskRequest request = new TaskRequest(
                "Comprar material",
                "Descripcion",
                null,
                TaskPriority.MEDIUM,
                LocalDate.now().minusDays(1)
        );

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(InvalidTaskDataException.class)
                .hasMessageContaining("pasado");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void create_sinEstadoIndicado_seAsignaEstadoPendientePorDefecto() {
        TaskRequest request = new TaskRequest(
                "Revisar informe",
                "Descripcion",
                null,
                TaskPriority.HIGH,
                LocalDate.now().plusDays(3)
        );
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task created = taskService.create(request);

        assertThat(created.getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void create_conEstadoIndicado_respetaElEstadoRecibido() {
        TaskRequest request = new TaskRequest(
                "Desplegar version",
                "Descripcion",
                TaskStatus.IN_PROGRESS,
                TaskPriority.LOW,
                LocalDate.now().plusDays(1)
        );
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task created = taskService.create(request);

        assertThat(created.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void findById_conIdInexistente_lanzaResourceNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_conIdInexistente_lanzaResourceNotFoundException() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());
        TaskRequest request = new TaskRequest(
                "Titulo", "Descripcion", TaskStatus.PENDING, TaskPriority.LOW, LocalDate.now().plusDays(1)
        );

        assertThatThrownBy(() -> taskService.update(42L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void update_conFechaLimiteEnElPasado_lanzaExcepcion() {
        Task existing = new Task("Original", "Desc", TaskStatus.PENDING, TaskPriority.MEDIUM, LocalDate.now().plusDays(5));
        existing.setId(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));

        TaskRequest request = new TaskRequest(
                "Original", "Desc", TaskStatus.PENDING, TaskPriority.MEDIUM, LocalDate.now().minusDays(2)
        );

        assertThatThrownBy(() -> taskService.update(1L, request))
                .isInstanceOf(InvalidTaskDataException.class)
                .hasMessageContaining("pasado");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void update_tareaCompletadaNoPuedeVolverAEstadoAnterior() {
        Task existing = new Task("Cerrar sprint", "Desc", TaskStatus.DONE, TaskPriority.HIGH, LocalDate.now().plusDays(10));
        existing.setId(7L);
        when(taskRepository.findById(7L)).thenReturn(Optional.of(existing));

        TaskRequest request = new TaskRequest(
                "Cerrar sprint", "Desc", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, LocalDate.now().plusDays(10)
        );

        assertThatThrownBy(() -> taskService.update(7L, request))
                .isInstanceOf(InvalidTaskDataException.class)
                .hasMessageContaining("completada");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void update_tareaCompletadaPuedePermanecerCompletada() {
        Task existing = new Task("Cerrar sprint", "Desc", TaskStatus.DONE, TaskPriority.HIGH, LocalDate.now().plusDays(10));
        existing.setId(7L);
        when(taskRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskRequest request = new TaskRequest(
                "Cerrar sprint", "Desc actualizada", TaskStatus.DONE, TaskPriority.HIGH, LocalDate.now().plusDays(10)
        );

        Task updated = taskService.update(7L, request);

        assertThat(updated.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(updated.getDescription()).isEqualTo("Desc actualizada");
    }

    @Test
    void delete_conIdInexistente_lanzaResourceNotFoundException() {
        when(taskRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).delete(any());
    }

    @Test
    void delete_conIdExistente_eliminaLaTarea() {
        Task existing = new Task("A borrar", "Desc", TaskStatus.PENDING, TaskPriority.LOW, LocalDate.now().plusDays(1));
        existing.setId(3L);
        when(taskRepository.findById(3L)).thenReturn(Optional.of(existing));

        taskService.delete(3L);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(3L);
    }
}
