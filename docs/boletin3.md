# Boletín 3 — Contenedores: Docker, Compose y Dev Container

- **Fecha:** 19 de agosto de 2026
- **Duración:** 2 h 30 min
- **Rama de trabajo:** `chore/docker`

## Objetivos de la sesión

1. Empaquetar la API en una imagen Docker con build multietapa.
2. Levantar API + PostgreSQL con Docker Compose.
3. Dejar el entorno de desarrollo reproducible con un Dev Container.

## Qué se hizo

### 1. Dockerfile multietapa

Etapa de build sobre `maven:3.9-eclipse-temurin-21` y etapa de runtime sobre
`eclipse-temurin:21-jre`. Dos decisiones importantes:

- Copiar primero `pom.xml` y ejecutar `mvn dependency:go-offline` **antes** de copiar `src/`,
  para que la capa de dependencias se cachee y los rebuilds sean rápidos.
- Crear un usuario `app` (uid 1001) y ejecutar con `USER app`: nunca como root.

Se añadió también un `HEALTHCHECK` que consulta `/actuator/health` (lo que obligó a incorporar
`spring-boot-starter-actuator` a las dependencias).

```bash
docker build -t task-manager .
docker run --rm -p 8080:8080 task-manager
```

Comparativa de tamaño que se midió en clase:

| Imagen | Tamaño |
|---|---|
| Una sola etapa (con Maven dentro) | ~780 MB |
| Multietapa (solo JRE) | ~310 MB |

### 2. `.dockerignore`

Sin él, el contexto de build enviaba `target/` y `.git/` al demonio. Añadirlo redujo el
contexto de ~120 MB a menos de 1 MB.

### 3. Docker Compose

`docker-compose.yml` con dos servicios: `db` (postgres:16, volumen `db-data`, `healthcheck` con
`pg_isready`) y `api`, que espera con `depends_on: condition: service_healthy`.

```bash
docker compose up --build
docker compose logs -f api
docker compose down          # y down -v para borrar también el volumen
```

Se comprobó la persistencia: se crearon tareas, `docker compose down`, `up` otra vez y las
tareas seguían ahí gracias al volumen.

### 4. Dev Container

`.devcontainer/devcontainer.json` con Java 21, Maven y Docker-in-Docker, y reenvío de los
puertos 8080 y 5432. Se documentó en el README el flujo "Reopen in Container".

## Commits generados

| Commit | Mensaje |
|---|---|
| `bcc6817` | `dockeradded` |
| `d3019b3` | `Merge pull request #3 from Antolin1/chore/docker` |
| `cbdd58f` | `devcontainers` |
| `98b0209` | `added readme` |

## Problemas encontrados

- **La API arrancaba antes que la base de datos** y moría con `Connection refused`. Solucionado
  con el `healthcheck` de Postgres y `service_healthy` en `depends_on` (por sí solo,
  `depends_on` únicamente espera a que el contenedor arranque, no a que el servicio esté listo).
- **`localhost` no funciona entre contenedores.** La URL correcta dentro de Compose es
  `jdbc:postgresql://db:5432/tareas`, usando el nombre del servicio.
- **El healthcheck del Dockerfile fallaba** porque la imagen JRE no traía `curl`; se cambió a
  `wget`.

## Estado al cerrar la sesión

`docker compose up --build` levanta el sistema completo desde cero, y el mensaje del commit
`dockeradded` queda como mal ejemplo de mensaje de commit.

## Pendiente para la próxima sesión

- Integración continua: que todo esto se verifique solo en cada push.
