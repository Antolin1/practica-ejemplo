# Boletín 1 — Arranque del proyecto y fundamentos de Git

- **Fecha:** 5 de agosto de 2026
- **Duración:** 2 h
- **Rama de trabajo:** `main`, `rama-1`, `rama-2`

## Objetivos de la sesión

1. Crear el esqueleto del proyecto Maven de la API de tareas.
2. Practicar el ciclo básico de Git: `init`, `add`, `commit`, `log`.
3. Trabajar con ramas y resolver un conflicto de merge provocado a propósito.
4. Automatizar el formateo del código para que no dependa de cada persona.

## Qué se hizo

### 1. Proyecto Maven inicial

Se generó la estructura `com.example.taskmanager` con Spring Boot 3.3.4 y Java 21, con las
dependencias mínimas: `spring-boot-starter-web`, `spring-boot-starter-data-jpa` y
`spring-boot-starter-validation`.

```bash
mvn -B clean package
mvn spring-boot:run
```

Se comprobó que la aplicación arranca y que `GET /tasks` responde con una lista vacía.

### 2. Primer commit y `.gitignore`

Antes del primer commit se añadió un `.gitignore` para Java/Maven, de modo que `target/`
nunca entrara en el repositorio. Se revisó `git status` antes y después para ver el efecto.

```bash
git init
git add .
git commit -m "chore: proyecto Maven inicial de la API de tareas"
```

### 3. `.editorconfig`

Se añadió `.editorconfig` (indentación de 4 espacios, UTF-8, fin de línea LF, salto final de
fichero) para unificar el estilo entre editores antes de tocar más código.

### 4. Ramas y conflicto de merge

Se crearon `rama-1` y `rama-2` y cada una modificó la misma zona de `Task.java` (una añadió el
campo de dependencias entre tareas, la otra tocó las anotaciones de validación). Al mezclar la
segunda apareció el conflicto esperado:

```bash
git checkout -b rama-1
# ... cambios ...
git checkout main && git merge rama-1     # fast-forward, sin problema
git merge rama-2                          # CONFLICT en Task.java
git status                                # ver los ficheros en conflicto
# edición manual eliminando los marcadores <<<<<<< ======= >>>>>>>
git add src/main/java/com/example/taskmanager/model/Task.java
git commit
```

### 5. Spotless

Se configuró el plugin `spotless-maven-plugin` (2.43.0) con `googleJavaFormat`,
`removeUnusedImports` y `trimTrailingWhitespace`. Primera pasada de formateo sobre todo el
código:

```bash
mvn spotless:apply
mvn spotless:check
```

## Commits generados

| Commit | Mensaje |
|---|---|
| `9833606` | `chore: proyecto Maven inicial de la API de tareas` |
| `06e2804` | `chore(editor): añade .editorconfig` |
| `4fc7daa` | `feat(task): task dependencies` |
| `d85a889` | `chore(build): añade Spotless para formateo automático` |
| `bd589af` | `Merge branch 'rama-2'` |

## Problemas encontrados

- **`target/` apareció en el primer `git add .`** porque el `.gitignore` se creó después. Se
  resolvió con `git rm -r --cached target/` y rehaciendo el commit.
- **`mvn spotless:check` falló en la primera ejecución** con cientos de líneas mal formateadas.
  Aprendizaje: `apply` primero, `check` después; en CI solo se ejecutará `check`.

## Estado al cerrar la sesión

La API compila, arranca y el formateo está automatizado. El historial es lineal salvo el merge
de `rama-2`, que queda como ejemplo de resolución de conflictos.

## Pendiente para la próxima sesión

- Trabajo con repositorio remoto: pull requests, revisiones y plantillas.
