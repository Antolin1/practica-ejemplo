# Boletín 4 — Integración continua con GitHub Actions

- **Fecha:** 26 de agosto de 2026
- **Duración:** 2 h 30 min
- **Rama de trabajo:** `test-ci-rule`

## Objetivos de la sesión

1. Escribir un workflow de CI que se ejecute en cada push a `main` y en cada PR.
2. Encadenar trabajos: formateo → tests → empaquetado.
3. Aprender a depurar un workflow que falla sin volverse loco en el intento.

## Qué se hizo

### 1. Primer `ci.yml`

Tres jobs en `.github/workflows/ci.yml`:

- **`lint`** — `mvn -B spotless:check`, el Spotless configurado en el Boletín 1.
- **`test`** — `needs: lint`, ejecuta `mvn -B verify`.
- **`package`** — `needs: test`, ejecuta `mvn -B package -DskipTests` y sube el JAR como
  artefacto.

### 2. Detalles que no son opcionales

```yaml
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read
```

- `concurrency` cancela ejecuciones antiguas de la misma rama: no tiene sentido validar código
  ya obsoleto (y ahorra minutos de runner).
- `permissions: contents: read` aplica mínimo privilegio; por defecto el token tiene más
  permisos de los que este workflow necesita.
- `cache: maven` en `actions/setup-java@v4` bajó el tiempo del job de test de ~3 min a ~50 s.
- `timeout-minutes` en cada job para que un cuelgue no consuma seis horas de runner.

### 3. Artefactos

El JAR se sube con `actions/upload-artifact@v4` y `retention-days: 7`, y se descargó desde la
interfaz de GitHub para comprobar que se ejecuta:

```bash
java -jar task-manager-0.0.1-SNAPSHOT.jar
```

### 4. El ciclo de depuración

Los cuatro commits seguidos (`actions`, `test-actions`, `fix actions`, `fix tests`) son el
registro honesto de la depuración: cada arreglo del YAML exige un push para probarlo. Se
comentó `act` como forma de reducir esas iteraciones ejecutando workflows en local.

## Commits generados

| Commit | Mensaje |
|---|---|
| `9c9f941` | `actions` |
| `ac0b602` | `test-actions` |
| `04cadae` | `fix actions` |
| `67305e6` | `fix tests` |
| `3f30987` | `new job` |
| `ea4ae77` | `Merge pull request #5 from Antolin1/test-ci-rule` |

## Problemas encontrados

- **`spotless:check` falló en CI pero pasaba en local:** alguien había hecho commit sin ejecutar
  `spotless:apply`. Se propuso un hook de pre-commit en `.githooks/` (se retoma en el Boletín 6).
- **Indentación del YAML.** Un job colgaba del nivel equivocado y GitHub lo ignoraba en silencio.
  El validador del editor lo detectó al instante.
- **`needs:` no es paralelismo gratis.** Se discutió cuándo conviene encadenar jobs y cuándo
  dejarlos correr en paralelo.

## Estado al cerrar la sesión

Cada PR queda validada automáticamente: formateo, tests y build. La regla de protección de
`main` del Boletín 2 se amplió para exigir que el CI esté en verde antes de mezclar.

## Pendiente para la próxima sesión

- Los tests son escasos: toca medir y exigir cobertura.
