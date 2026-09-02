# task-manager

API REST de gestión de tareas con Spring Boot 3 y Java 21.

## Requisitos

- Docker y Docker Compose (opción recomendada), **o**
- JDK 21 y Maven, si prefieres ejecutarlo en local sin contenedores

## Ejecutar con Docker Compose (recomendado)

Levanta la API junto con la base de datos PostgreSQL:

```bash
docker compose up --build
```

La API queda disponible en [http://localhost:8080](http://localhost:8080) y PostgreSQL en el puerto `5432`.

Para pararla:

```bash
docker compose down
```

## Ejecutar en local con Maven

Necesitas una base de datos PostgreSQL accesible (por ejemplo, levantando solo el servicio `db` del compose):

```bash
docker compose up db
```

Y en otra terminal, arranca la aplicación indicando la conexión a la base de datos:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tareas \
SPRING_DATASOURCE_USERNAME=app \
SPRING_DATASOURCE_PASSWORD=secret_local_dev \
mvn spring-boot:run
```

## Ejecutar en el Dev Container

El repo incluye una configuración de [Dev Container](.devcontainer/devcontainer.json) con Java 21, Maven y Docker-in-Docker. Basta con abrir la carpeta en VS Code y seleccionar "Reopen in Container"; los puertos `8080` (API) y `5432` (PostgreSQL) se reenvían automáticamente.

## Tests

```bash
mvn test
```

## Build del jar

```bash
mvn clean package
java -jar target/task-manager-0.0.1-SNAPSHOT.jar
```

## Healthcheck

Una vez arrancada, se puede comprobar el estado en:

```
GET http://localhost:8080/actuator/health
```
