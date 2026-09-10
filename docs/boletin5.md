# Boletín 5 — Tests, cobertura con JaCoCo y calidad visible

- **Fecha:** 2 de septiembre de 2026
- **Duración:** 2 h
- **Rama de trabajo:** `test/cobertura`

## Objetivos de la sesión

1. Escribir tests unitarios de la capa de servicio con JUnit 5 y Mockito.
2. Medir la cobertura con JaCoCo y hacer que la build falle si baja del umbral.
3. Publicar los resultados en la interfaz de GitHub (informe de tests y badge).

## Qué se hizo

### 1. `TaskServiceTest`

Tests de `TaskService` con el repositorio *mockeado*, cubriendo:

- Creación de una tarea con datos válidos.
- `ResourceNotFoundException` al buscar un id inexistente.
- `InvalidTaskDataException` con una fecha límite en el pasado.
- Filtrado por `TaskStatus`, verificando que se delega en `findByStatus`.
- Cambio de prioridad (`TaskPriority`) sobre una tarea existente.

```bash
mvn test
mvn -Dtest=TaskServiceTest test
```

Se insistió en la estructura *given / when / then* y en usar `assertThrows` en lugar de
try/catch con `fail()`.

### 2. JaCoCo

Plugin `jacoco-maven-plugin` 0.8.12 con tres ejecuciones:

| Ejecución | Fase | Para qué |
|---|---|---|
| `prepare-agent` | (previa a test) | Instrumenta las clases |
| `report` | `test` | Genera el informe HTML/XML en `target/site/jacoco` |
| `check-coverage` | `verify` | Falla la build si la cobertura de líneas baja del umbral |

El umbral se parametrizó como propiedad para poder subirlo poco a poco:

```xml
<jacoco.line.coverage>0.60</jacoco.line.coverage>
```

Se excluyó `TaskManagerApplication.class` del cómputo: la clase de arranque no tiene lógica que
merezca cobertura.

```bash
mvn clean verify
xdg-open target/site/jacoco/index.html
```

Primera medición: **41 %**. Tras añadir los tests del punto 1: **68 %**, por encima del umbral.

### 3. Informe de tests y cobertura en CI

En el job `test` del workflow se añadieron dos pasos, ambos con `if: always()`:

- `dorny/test-reporter@v1` publica los resultados JUnit de `target/surefire-reports/*.xml`.
- `upload-artifact` sube `target/site/jacoco/`.

El `always()` es justo lo que importa: el informe interesa **sobre todo** cuando la build falla
por no alcanzar el umbral de cobertura.

### 4. Badge en el README

Se añadió el badge de estado del workflow al principio del README para que el estado del
proyecto se vea sin entrar en la pestaña Actions.

## Commits generados

| Commit | Mensaje |
|---|---|
| `09eae17` | `testing` |
| `af39db2` | `badge` |
| `7df0cd3` | `updated readme` |

## Problemas encontrados

- **`mvn test` no ejecutaba el `check` de JaCoCo.** Está ligado a la fase `verify`; hay que
  ejecutar `mvn verify` (es lo que hace el CI).
- **La cobertura salía al 0 %** en la primera prueba: faltaba la ejecución `prepare-agent`, sin
  la cual las clases no se instrumentan.
- **Discusión sobre el umbral.** Se acordó 60 % como suelo realista y no perseguir el 100 %:
  cubrir DTOs sin lógica infla la métrica sin aportar confianza.

## Estado al cerrar la sesión

Cobertura al 68 %, con la build fallando automáticamente si alguien la deja caer por debajo del
60 %, y resultados visibles en cada PR.

## Pendiente para la próxima sesión

- Publicar versiones y automatizar seguridad y revisión.
