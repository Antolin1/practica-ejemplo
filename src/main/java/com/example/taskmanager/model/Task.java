package com.example.taskmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "El titulo es obligatorio")
  @Size(max = 100, message = "El titulo no puede superar los 100 caracteres")
  @Column(nullable = false, length = 100)
  private String title;

  @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
  @Column(length = 500)
  private String description;

  @NotNull(message = "El estado es obligatorio")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status;

  @NotNull(message = "La prioridad es obligatoria")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskPriority priority;

  @NotNull(message = "La fecha limite es obligatoria")
  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @ManyToMany
  @JoinTable(
      name = "task_dependencies",
      joinColumns = @JoinColumn(name = "task_id"),
      inverseJoinColumns = @JoinColumn(name = "depends_on_task_id"))
  private Set<Task> dependencies = new HashSet<>();

  public Task() {}

  public Task(
      String title,
      String description,
      TaskStatus status,
      TaskPriority priority,
      LocalDate dueDate) {
    this.title = title;
    this.description = description;
    this.status = status;
    this.priority = priority;
    this.dueDate = dueDate;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public TaskPriority getPriority() {
    return priority;
  }

  public void setPriority(TaskPriority priority) {
    this.priority = priority;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public Set<Task> getDependencies() {
    return dependencies;
  }

  public void setDependencies(Set<Task> dependencies) {
    this.dependencies = dependencies;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Task task)) return false;
    return Objects.equals(id, task.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
