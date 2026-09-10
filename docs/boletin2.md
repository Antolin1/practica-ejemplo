# Boletín 2 — Colaboración en GitHub: issues, PRs y protección de `main`

- **Fecha:** 12 de agosto de 2026
- **Duración:** 2 h
- **Rama de trabajo:** `feat/buscar-por-status`

## Objetivos de la sesión

1. Publicar el repositorio en GitHub y configurar el flujo de trabajo del equipo.
2. Añadir plantillas de issues y de pull request, y un fichero `CODEOWNERS`.
3. Resolver una issue real a través de una pull request revisada.
4. Comprobar en la práctica qué hace una regla de protección de rama.

## Qué se hizo

### 1. Carpeta `.github`

Se creó `.github/` con:

- `ISSUE_TEMPLATE/` — plantillas de *bug report* y *feature request*.
- `PULL_REQUEST_TEMPLATE.md` — checklist: tests pasando, formateo aplicado, issue enlazada.
- `CODEOWNERS` — el equipo docente como revisor obligatorio de `/src` y `/.github`.

El `CODEOWNERS` tuvo que corregirse: la primera versión usaba una ruta relativa que GitHub no
reconocía y no asignaba revisor automáticamente.

### 2. Issue #2 y su pull request

Issue: *"Poder filtrar las tareas por estado"*. Se implementó en `TaskService` y
`TaskController` un endpoint `GET /tasks?status=PENDING`, apoyado en un método derivado del
repositorio:

```java
List<Task> findByStatus(TaskStatus status);
```

Flujo seguido:

```bash
git checkout -b feat/buscar-por-status
# ... implementación ...
git push -u origin feat/buscar-por-status
gh pr create --fill
```

La PR recibió dos comentarios de revisión (nombrar mejor el parámetro y validar el valor del
enum) que se atendieron con commits adicionales sobre la misma rama, y se mezcló con
*squash merge*.

### 3. Protección de `main`

Se activó la regla: prohibido el push directo, PR obligatoria y al menos una aprobación. Se
comprobó intentando saltársela:

```bash
git checkout main
git commit --allow-empty -m "chore: intento de push directo"
git push
# ! [remote rejected] main -> main (protected branch hook declined)
```

El commit local quedó en el historial como recordatorio del experimento.

## Commits generados

| Commit | Mensaje |
|---|---|
| `5f6df4f` | `chore: añadido .github con los templates de issues, PR, etc.` |
| `967a078` | `chore: fixing codeowners` |
| `6221173` | `feat: encontrar tarea por status (#2)` |
| `612c12f` | `chore: intento de push directo` |

## Problemas encontrados

- **La PR no asignaba revisor.** Causa: sintaxis incorrecta en `CODEOWNERS`. Las rutas deben
  empezar por `/` o ser globs (`*.java`).
- **Squash vs merge commit.** Se decidió *squash* para las ramas de feature, dejando el
  historial de `main` legible con un commit por funcionalidad.

## Estado al cerrar la sesión

`main` está protegida, todo cambio entra por PR revisada y el repositorio tiene plantillas que
guían las contribuciones.

## Pendiente para la próxima sesión

- Contenerizar la aplicación y su base de datos.
