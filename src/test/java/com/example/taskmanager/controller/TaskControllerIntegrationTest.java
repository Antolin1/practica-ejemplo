package com.example.taskmanager.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TaskRepository taskRepository;

  @BeforeEach
  void cleanDatabase() {
    taskRepository.deleteAll();
  }

  @Test
  void crearTarea_conFechaLimiteEnElPasado_devuelve400ConMensajeDeError() throws Exception {
    TaskRequest request =
        new TaskRequest(
            "Tarea invalida", "desc", null, TaskPriority.MEDIUM, LocalDate.now().minusDays(3));

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("La fecha limite no puede estar en el pasado"));
  }

  @Test
  void crearTarea_conTituloEnBlanco_devuelve400DeValidacion() throws Exception {
    TaskRequest request =
        new TaskRequest("   ", "desc", null, TaskPriority.LOW, LocalDate.now().plusDays(1));

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details", hasSize(greaterThan(0))));
  }

  @Test
  void crearTarea_conDatosValidos_devuelve201YPersiste() throws Exception {
    TaskRequest request =
        new TaskRequest(
            "Nueva tarea", "desc", null, TaskPriority.HIGH, LocalDate.now().plusDays(5));

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value("PENDING"));

    org.assertj.core.api.Assertions.assertThat(taskRepository.findAll()).hasSize(1);
  }

  @Test
  void obtenerTarea_conIdInexistente_devuelve404ConCuerpoJsonNoTraza() throws Exception {
    mockMvc
        .perform(get("/api/tasks/{id}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("No existe una tarea con id 999"));
  }

  @Test
  void actualizarTarea_conIdInexistente_devuelve404() throws Exception {
    TaskRequest request =
        new TaskRequest(
            "Titulo", "desc", TaskStatus.PENDING, TaskPriority.LOW, LocalDate.now().plusDays(1));

    mockMvc
        .perform(
            put("/api/tasks/{id}", 999L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void borrarTarea_yLuegoConsultarla_devuelve404() throws Exception {
    TaskRequest request =
        new TaskRequest(
            "Tarea temporal", "desc", null, TaskPriority.LOW, LocalDate.now().plusDays(2));
    String response =
        mockMvc
            .perform(
                post("/api/tasks")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(delete("/api/tasks/{id}", id)).andExpect(status().isNoContent());

    mockMvc.perform(get("/api/tasks/{id}", id)).andExpect(status().isNotFound());
  }

  private Long crearTarea(TaskRequest request) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/tasks")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }

  @Test
  void crearTarea_conDependenciaExistente_devuelve201ConLaDependenciaAsociada() throws Exception {
    Long baseId =
        crearTarea(
            new TaskRequest(
                "Preparar entorno",
                "desc",
                null,
                TaskPriority.MEDIUM,
                LocalDate.now().plusDays(1)));

    TaskRequest request =
        new TaskRequest(
            "Desplegar",
            "desc",
            null,
            TaskPriority.HIGH,
            LocalDate.now().plusDays(2),
            Set.of(baseId));

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.dependencyIds", hasSize(1)))
        .andExpect(jsonPath("$.dependencyIds[0]").value(baseId));
  }

  @Test
  void crearTarea_conDependenciaInexistente_devuelve404() throws Exception {
    TaskRequest request =
        new TaskRequest(
            "Desplegar",
            "desc",
            null,
            TaskPriority.HIGH,
            LocalDate.now().plusDays(2),
            Set.of(999L));

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void actualizarTarea_conDependenciaCircular_devuelve400() throws Exception {
    Long taskAId =
        crearTarea(
            new TaskRequest(
                "Tarea A", "desc", null, TaskPriority.MEDIUM, LocalDate.now().plusDays(3)));
    Long taskBId =
        crearTarea(
            new TaskRequest(
                "Tarea B",
                "desc",
                null,
                TaskPriority.MEDIUM,
                LocalDate.now().plusDays(3),
                Set.of(taskAId)));

    TaskRequest circularUpdate =
        new TaskRequest(
            "Tarea A",
            "desc",
            TaskStatus.PENDING,
            TaskPriority.MEDIUM,
            LocalDate.now().plusDays(3),
            Set.of(taskBId));

    mockMvc
        .perform(
            put("/api/tasks/{id}", taskAId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(circularUpdate)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ciclo")));
  }

  @Test
  void borrarTarea_conOtrasTareasQueDependenDeElla_devuelve400() throws Exception {
    Long baseId =
        crearTarea(
            new TaskRequest(
                "Tarea base", "desc", null, TaskPriority.MEDIUM, LocalDate.now().plusDays(1)));
    crearTarea(
        new TaskRequest(
            "Tarea dependiente",
            "desc",
            null,
            TaskPriority.MEDIUM,
            LocalDate.now().plusDays(2),
            Set.of(baseId)));

    mockMvc
        .perform(delete("/api/tasks/{id}", baseId))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("dependen")));
  }
}
