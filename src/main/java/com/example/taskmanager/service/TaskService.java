package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.exception.InvalidTaskDataException;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una tarea con id " + id));
    }

    public Task create(TaskRequest request) {
        validateDueDateNotInPast(request.dueDate());

        TaskStatus status = request.status() != null ? request.status() : TaskStatus.PENDING;

        Task task = new Task(
                request.title(),
                request.description(),
                status,
                request.priority(),
                request.dueDate()
        );
        return taskRepository.save(task);
    }

    public Task update(Long id, TaskRequest request) {
        Task existing = findById(id);

        validateDueDateNotInPast(request.dueDate());
        validateStatusTransition(existing.getStatus(), request.status());

        existing.setTitle(request.title());
        existing.setDescription(request.description());
        existing.setPriority(request.priority());
        existing.setDueDate(request.dueDate());
        if (request.status() != null) {
            existing.setStatus(request.status());
        }

        return taskRepository.save(existing);
    }

    public void delete(Long id) {
        Task existing = findById(id);
        taskRepository.delete(existing);
    }

    private void validateDueDateNotInPast(LocalDate dueDate) {
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new InvalidTaskDataException("La fecha limite no puede estar en el pasado");
        }
    }

    private void validateStatusTransition(TaskStatus currentStatus, TaskStatus newStatus) {
        if (newStatus == null) {
            return;
        }
        if (currentStatus == TaskStatus.DONE && newStatus != TaskStatus.DONE) {
            throw new InvalidTaskDataException("Una tarea completada no puede volver a un estado anterior");
        }
    }
}
