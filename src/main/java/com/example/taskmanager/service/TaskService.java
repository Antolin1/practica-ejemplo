package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.exception.InvalidTaskDataException;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

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
    return taskRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("No existe una tarea con id " + id));
  }

  public Task create(TaskRequest request) {
    validateDueDateNotInPast(request.dueDate());
    Set<Task> dependencies = resolveDependencies(request.dependencyIds());

    TaskStatus status = request.status() != null ? request.status() : TaskStatus.PENDING;

    Task task =
        new Task(
            request.title(), request.description(), status, request.priority(), request.dueDate());
    task.setDependencies(dependencies);
    return taskRepository.save(task);
  }

  public Task update(Long id, TaskRequest request) {
    Task existing = findById(id);

    validateDueDateNotInPast(request.dueDate());
    validateStatusTransition(existing.getStatus(), request.status());

    Set<Task> dependencies = resolveDependencies(request.dependencyIds());
    validateNoCyclicDependency(id, dependencies);

    existing.setTitle(request.title());
    existing.setDescription(request.description());
    existing.setPriority(request.priority());
    existing.setDueDate(request.dueDate());
    existing.setDependencies(dependencies);
    if (request.status() != null) {
      existing.setStatus(request.status());
    }

    return taskRepository.save(existing);
  }

  public void delete(Long id) {
    Task existing = findById(id);
    if (taskRepository.existsByDependenciesId(id)) {
      throw new InvalidTaskDataException(
          "No se puede eliminar una tarea de la que dependen otras tareas");
    }
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
      throw new InvalidTaskDataException(
          "Una tarea completada no puede volver a un estado anterior");
    }
  }

  private Set<Task> resolveDependencies(Set<Long> dependencyIds) {
    if (dependencyIds == null || dependencyIds.isEmpty()) {
      return new HashSet<>();
    }
    Set<Task> dependencies = new HashSet<>();
    for (Long dependencyId : dependencyIds) {
      Task dependency =
          taskRepository
              .findById(dependencyId)
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "No existe una tarea con id "
                              + dependencyId
                              + " para usar como dependencia"));
      dependencies.add(dependency);
    }
    return dependencies;
  }

  private void validateNoCyclicDependency(Long taskId, Set<Task> newDependencies) {
    Set<Long> visited = new HashSet<>();
    Deque<Task> pending = new ArrayDeque<>(newDependencies);
    while (!pending.isEmpty()) {
      Task current = pending.poll();
      if (current.getId().equals(taskId)) {
        throw new InvalidTaskDataException(
            "La dependencia introduce un ciclo: una tarea no puede depender, directa o indirectamente, de si misma");
      }
      if (!visited.add(current.getId())) {
        continue;
      }
      pending.addAll(current.getDependencies());
    }
  }
}
