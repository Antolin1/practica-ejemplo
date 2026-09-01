package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping
  public List<TaskResponse> getAll(@RequestParam(required = false) TaskStatus status) {
    List<Task> tasks = status != null ? taskService.findByStatus(status) : taskService.findAll();
    return tasks.stream().map(TaskResponse::fromEntity).toList();
  }

  @GetMapping("/{id}")
  public TaskResponse getById(@PathVariable Long id) {
    Task task = taskService.findById(id);
    return TaskResponse.fromEntity(task);
  }

  @PostMapping
  public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
    Task created = taskService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.fromEntity(created));
  }

  @PutMapping("/{id}")
  public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
    Task updated = taskService.update(id, request);
    return TaskResponse.fromEntity(updated);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    taskService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
