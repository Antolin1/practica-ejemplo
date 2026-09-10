#!/usr/bin/env python3
"""Genera un sitio HTML estatico a partir de los ficheros Markdown de docs/.

Convierte cada .md en un .html equivalente y crea un index.html que enlaza a
todos ellos. No necesita dependencias externas: solo la biblioteca estandar.

Uso:
    python3 docs/generar_html.py                 # docs/*.md -> docs/site/
    python3 docs/generar_html.py -o docs         # genera junto a los .md
    python3 docs/generar_html.py -i docs -o /tmp/web
"""

from __future__ import annotations

import argparse
import html
import re
import sys
from pathlib import Path

# --------------------------------------------------------------------------
# Conversion de Markdown a HTML (subconjunto: el que usan los boletines)
# --------------------------------------------------------------------------

VINETA_RE = re.compile(r"^(\s*)([-*+]|\d+[.)])\s+(.*)$")
ENCABEZADO_RE = re.compile(r"^(#{1,6})\s+(.*?)\s*#*$")
VALLA_RE = re.compile(r"^\s*(```+|~~~+)\s*([\w+-]*)\s*$")
SEPARADOR_TABLA_RE = re.compile(r"^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$")
REGLA_RE = re.compile(r"^\s*(-{3,}|\*{3,}|_{3,})\s*$")

MARCA = "\x00{}\x00"  # hueco temporal para los `code spans`


def _slug(texto: str) -> str:
    """Identificador para anclar un encabezado (#mi-seccion)."""
    limpio = re.sub(r"[`*_\[\]()]", "", texto).strip().lower()
    limpio = (limpio.replace("á", "a").replace("é", "e").replace("í", "i")
                    .replace("ó", "o").replace("ú", "u").replace("ñ", "n"))
    limpio = re.sub(r"[^a-z0-9]+", "-", limpio)
    return limpio.strip("-") or "seccion"


def _reescribe_enlace(url: str) -> str:
    """Los enlaces relativos a .md apuntan al .html generado."""
    if re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*:", url) or url.startswith("//"):
        return url  # absoluto o con esquema: se deja tal cual
    destino, _, ancla = url.partition("#")
    if destino.endswith(".md"):
        destino = destino[:-3] + ".html"
    return destino + (("#" + ancla) if ancla else "")


def _inline(texto: str) -> str:
    """Convierte el marcado dentro de una linea (codigo, negrita, enlaces...)."""
    trozos: list[str] = []

    def _guarda(match: re.Match) -> str:
        trozos.append(match.group(2))
        return MARCA.format(len(trozos) - 1)

    # 1. El codigo en linea se aparta antes de tocar nada mas
    texto = re.sub(r"(`+)(.+?)\1", _guarda, texto, flags=re.DOTALL)

    # 2. Escapado: a partir de aqui el texto ya es HTML seguro
    texto = html.escape(texto, quote=False)

    # 3. Imagenes y enlaces
    texto = re.sub(
        r"!\[([^\]]*)\]\(([^)\s]+)\)",
        lambda m: f'<img src="{html.escape(_reescribe_enlace(m.group(2)), quote=True)}"'
                  f' alt="{html.escape(m.group(1), quote=True)}">',
        texto,
    )
    texto = re.sub(
        r"\[([^\]]+)\]\(([^)\s]+)\)",
        lambda m: f'<a href="{html.escape(_reescribe_enlace(m.group(2)), quote=True)}">'
                  f"{m.group(1)}</a>",
        texto,
    )

    # 4. Enfasis y tachado
    # DOTALL: en un parrafo el marcado puede abrirse en una linea y cerrarse en la siguiente
    texto = re.sub(r"\*\*\*(.+?)\*\*\*", r"<strong><em>\1</em></strong>", texto, flags=re.DOTALL)
    texto = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", texto, flags=re.DOTALL)
    texto = re.sub(r"(?<!\w)_([^_]+)_(?!\w)", r"<em>\1</em>", texto)
    texto = re.sub(r"(?<![\w*])\*([^*]+?)\*(?![\w*])", r"<em>\1</em>", texto, flags=re.DOTALL)
    texto = re.sub(r"~~(.+?)~~", r"<del>\1</del>", texto)

    # 5. Se devuelve el codigo en linea, ya escapado
    for i, trozo in enumerate(trozos):
        texto = texto.replace(MARCA.format(i), f"<code>{html.escape(trozo, quote=False)}</code>")
    return texto


def _celdas(linea: str) -> list[str]:
    linea = linea.strip()
    if linea.startswith("|"):
        linea = linea[1:]
    if linea.endswith("|") and not linea.endswith("\\|"):
        linea = linea[:-1]
    return [c.strip().replace("\\|", "|") for c in re.split(r"(?<!\\)\|", linea)]


def _alineaciones(separador: str) -> list[str]:
    resultado = []
    for celda in _celdas(separador):
        izq, der = celda.startswith(":"), celda.endswith(":")
        resultado.append("center" if izq and der else "right" if der else "left" if izq else "")
    return resultado


def _lista(lineas: list[str], i: int) -> tuple[str, int]:
    """Construye una lista (con sus posibles sublistas) desde lineas[i]."""
    primera = VINETA_RE.match(lineas[i])
    sangria = len(primera.group(1))
    ordenada = primera.group(2)[0].isdigit()
    items: list[list[str]] = []

    while i < len(lineas):
        linea = lineas[i]
        if not linea.strip():
            # Una linea en blanco solo continua la lista si despues sigue habiendo lista
            j = i
            while j < len(lineas) and not lineas[j].strip():
                j += 1
            siguiente = VINETA_RE.match(lineas[j]) if j < len(lineas) else None
            if siguiente and len(siguiente.group(1)) >= sangria:
                if items:
                    items[-1].append("")
                i = j
                continue
            break
        marca = VINETA_RE.match(linea)
        if marca and len(marca.group(1)) == sangria:
            items.append([marca.group(3)])
            i += 1
        elif marca and len(marca.group(1)) > sangria and items:
            items[-1].append(linea[sangria + 2:] if len(linea) > sangria + 2 else linea.strip())
            i += 1
        elif items and not marca:
            items[-1].append(linea.strip())  # continuacion perezosa del item
            i += 1
        else:
            break

    partes = []
    for contenido in items:
        cuerpo = _bloques(contenido).strip()
        # Un item de una sola linea no necesita envolverse en <p>
        if cuerpo.startswith("<p>") and cuerpo.endswith("</p>") and cuerpo.count("<p>") == 1:
            cuerpo = cuerpo[3:-4]
        partes.append(f"  <li>{cuerpo}</li>")
    etiqueta = "ol" if ordenada else "ul"
    return f"<{etiqueta}>\n" + "\n".join(partes) + f"\n</{etiqueta}>", i


def _bloques(lineas: list[str]) -> str:
    """Convierte una lista de lineas Markdown en HTML."""
    salida: list[str] = []
    i = 0
    while i < len(lineas):
        linea = lineas[i]

        if not linea.strip():
            i += 1
            continue

        # Bloque de codigo delimitado
        valla = VALLA_RE.match(linea)
        if valla:
            cierre, lenguaje = valla.group(1)[0] * 3, valla.group(2)
            i += 1
            codigo = []
            while i < len(lineas) and not lineas[i].strip().startswith(cierre):
                codigo.append(lineas[i])
                i += 1
            i += 1  # salta la valla de cierre
            clase = f' class="language-{html.escape(lenguaje, quote=True)}"' if lenguaje else ""
            texto = html.escape("\n".join(codigo), quote=False)
            salida.append(f"<pre><code{clase}>{texto}</code></pre>")
            continue

        if REGLA_RE.match(linea):
            salida.append("<hr>")
            i += 1
            continue

        encabezado = ENCABEZADO_RE.match(linea)
        if encabezado:
            nivel = len(encabezado.group(1))
            titulo = encabezado.group(2)
            salida.append(f'<h{nivel} id="{_slug(titulo)}">{_inline(titulo)}</h{nivel}>')
            i += 1
            continue

        # Tabla: cabecera + fila separadora
        if "|" in linea and i + 1 < len(lineas) and SEPARADOR_TABLA_RE.match(lineas[i + 1]):
            cabecera = _celdas(linea)
            alineacion = _alineaciones(lineas[i + 1])
            i += 2
            filas = []
            while i < len(lineas) and lineas[i].strip() and "|" in lineas[i]:
                filas.append(_celdas(lineas[i]))
                i += 1

            def _td(valor: str, col: int, etiqueta: str) -> str:
                estilo = ""
                if col < len(alineacion) and alineacion[col]:
                    estilo = f' style="text-align:{alineacion[col]}"'
                return f"<{etiqueta}{estilo}>{_inline(valor)}</{etiqueta}>"

            tabla = ['<div class="tabla"><table>', "<thead><tr>"]
            tabla += [_td(c, n, "th") for n, c in enumerate(cabecera)]
            tabla.append("</tr></thead>")
            if filas:
                tabla.append("<tbody>")
                for fila in filas:
                    tabla.append("<tr>" + "".join(_td(c, n, "td") for n, c in enumerate(fila)) + "</tr>")
                tabla.append("</tbody>")
            tabla.append("</table></div>")
            salida.append("\n".join(tabla))
            continue

        # Cita
        if linea.lstrip().startswith(">"):
            cita = []
            while i < len(lineas) and lineas[i].lstrip().startswith(">"):
                cita.append(re.sub(r"^\s*>\s?", "", lineas[i]))
                i += 1
            salida.append(f"<blockquote>\n{_bloques(cita)}\n</blockquote>")
            continue

        # Lista
        if VINETA_RE.match(linea):
            bloque, i = _lista(lineas, i)
            salida.append(bloque)
            continue

        # Parrafo: hasta la proxima linea en blanco o inicio de otro bloque
        parrafo = []
        while i < len(lineas) and lineas[i].strip():
            actual = lineas[i]
            if (VALLA_RE.match(actual) or ENCABEZADO_RE.match(actual)
                    or VINETA_RE.match(actual) or REGLA_RE.match(actual)
                    or actual.lstrip().startswith(">")):
                break
            parrafo.append(actual.strip())
            i += 1
        if parrafo:
            salida.append(f"<p>{_inline(chr(10).join(parrafo))}</p>")

    return "\n\n".join(salida)


def markdown_a_html(texto: str) -> str:
    return _bloques(texto.replace("\r\n", "\n").replace("\t", "    ").split("\n"))


# --------------------------------------------------------------------------
# Plantilla del sitio
# --------------------------------------------------------------------------

ESTILOS = """
:root {
  color-scheme: light dark;
  --fondo: #fbfaf8; --texto: #1f2328; --tenue: #5b6570;
  --borde: #e2e0dc; --acento: #9a3f2b; --codigo: #f2efeb;
}
@media (prefers-color-scheme: dark) {
  :root {
    --fondo: #16181c; --texto: #e6e3de; --tenue: #9aa2ad;
    --borde: #30343b; --acento: #e08a6f; --codigo: #21242a;
  }
}
* { box-sizing: border-box; }
body {
  margin: 0; background: var(--fondo); color: var(--texto);
  font: 16px/1.65 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}
main { max-width: 46rem; margin: 0 auto; padding: 2.5rem 1.25rem 5rem; }
h1, h2, h3, h4 { line-height: 1.25; margin: 2.2rem 0 .8rem; }
h1 { font-size: 1.9rem; margin-top: 0; }
h2 { font-size: 1.35rem; padding-bottom: .3rem; border-bottom: 1px solid var(--borde); }
h3 { font-size: 1.1rem; }
a { color: var(--acento); }
p, ul, ol { margin: 0 0 1rem; }
li { margin: .25rem 0; }
code {
  background: var(--codigo); border-radius: 4px; padding: .12em .35em;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: .88em;
}
pre {
  background: var(--codigo); border: 1px solid var(--borde); border-radius: 8px;
  padding: .9rem 1rem; overflow-x: auto;
}
pre code { background: none; padding: 0; font-size: .85em; }
blockquote {
  margin: 1.2rem 0; padding: .1rem 1rem; color: var(--tenue);
  border-left: 3px solid var(--borde);
}
.tabla { overflow-x: auto; margin: 0 0 1.2rem; }
table { border-collapse: collapse; width: 100%; font-size: .94em; }
th, td { border: 1px solid var(--borde); padding: .45rem .7rem; text-align: left; }
th { background: var(--codigo); }
hr { border: 0; border-top: 1px solid var(--borde); margin: 2rem 0; }
img { max-width: 100%; }
.navegacion { font-size: .9rem; margin-bottom: 2rem; }
.navegacion a { text-decoration: none; }
.pie { margin-top: 3rem; padding-top: 1rem; border-top: 1px solid var(--borde);
       color: var(--tenue); font-size: .85rem; }
.indice { list-style: none; padding: 0; }
.indice li { border: 1px solid var(--borde); border-radius: 8px; padding: .8rem 1rem; margin: .6rem 0; }
.indice a { font-weight: 600; text-decoration: none; font-size: 1.05rem; }
.indice .resumen { color: var(--tenue); font-size: .9rem; margin: .25rem 0 0; }
"""

PLANTILLA = """<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{titulo}</title>
<style>{estilos}</style>
</head>
<body>
<main>
{navegacion}{contenido}
<p class="pie">Generado con <code>docs/generar_html.py</code> a partir de <code>{origen}</code>.</p>
</main>
</body>
</html>
"""


def _titulo(texto: str, respaldo: str) -> str:
    for linea in texto.split("\n"):
        encabezado = ENCABEZADO_RE.match(linea)
        if encabezado and len(encabezado.group(1)) == 1:
            return encabezado.group(2).strip()
    return respaldo


def _texto_plano(marcado: str) -> str:
    """Version sin marcado de un titulo, para <title> y atributos."""
    limpio = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", marcado)
    return re.sub(r"[`*_~]", "", limpio).strip()


def _resumen(texto: str) -> str:
    """Primera linea con contenido util despues del titulo, para el indice."""
    visto_titulo = False
    for linea in texto.split("\n"):
        limpia = linea.strip()
        if not limpia:
            continue
        if ENCABEZADO_RE.match(limpia):
            visto_titulo = True
            continue
        if not visto_titulo or VALLA_RE.match(limpia):
            continue
        limpia = re.sub(r"^[-*+]\s+", "", limpia)
        limpia = re.sub(r"[*`_]", "", limpia)
        limpia = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", limpia)
        return limpia[:160]
    return ""


def _ruta_legible(ruta: Path) -> str:
    """Ruta relativa al directorio actual si es posible; absoluta si no."""
    try:
        return str(ruta.relative_to(Path.cwd()))
    except ValueError:
        return str(ruta)


def _orden_natural(ruta: Path):
    """boletin2 antes que boletin10; README siempre el primero."""
    partes = [int(t) if t.isdigit() else t.lower()
              for t in re.split(r"(\d+)", ruta.stem)]
    return (0 if ruta.stem.lower() == "readme" else 1, partes)


def generar(entrada: Path, salida: Path, titulo_indice: str) -> int:
    fuentes = sorted(entrada.glob("*.md"), key=_orden_natural)
    if not fuentes:
        print(f"No se encontro ningun .md en {entrada}", file=sys.stderr)
        return 1

    salida.mkdir(parents=True, exist_ok=True)
    paginas = []

    for fuente in fuentes:
        texto = fuente.read_text(encoding="utf-8")
        titulo = _titulo(texto, fuente.stem)
        destino = salida / (fuente.stem + ".html")
        destino.write_text(
            PLANTILLA.format(
                titulo=html.escape(_texto_plano(titulo), quote=True),
                estilos=ESTILOS,
                navegacion='<p class="navegacion"><a href="index.html">&larr; Indice</a></p>\n',
                contenido=markdown_a_html(texto),
                origen=html.escape(fuente.name, quote=True),
            ),
            encoding="utf-8",
        )
        paginas.append((destino.name, titulo, _resumen(texto)))
        print(f"  {fuente.name} -> {_ruta_legible(destino)}")

    filas = []
    for nombre, titulo, resumen in paginas:
        resumen_html = f'\n    <p class="resumen">{_inline(resumen)}</p>' if resumen else ""
        filas.append(
            f'  <li>\n    <a href="{html.escape(nombre, quote=True)}">'
            f"{_inline(titulo)}</a>{resumen_html}\n  </li>"
        )

    indice = (
        f"<h1>{html.escape(titulo_indice, quote=False)}</h1>\n"
        f"<p>{len(paginas)} documento(s) generados desde "
        f"<code>{html.escape(entrada.name)}/*.md</code>.</p>\n"
        '<ul class="indice">\n' + "\n".join(filas) + "\n</ul>"
    )
    destino_indice = salida / "index.html"
    destino_indice.write_text(
        PLANTILLA.format(
            titulo=html.escape(_texto_plano(titulo_indice), quote=True),
            estilos=ESTILOS,
            navegacion="",
            contenido=indice,
            origen=html.escape(f"{entrada.name}/*.md", quote=True),
        ),
        encoding="utf-8",
    )
    print(f"  index.html -> {_ruta_legible(destino_indice)} ({len(paginas)} enlaces)")
    return 0


def main() -> int:
    aqui = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(description="Convierte los .md de docs/ en un sitio HTML.")
    parser.add_argument("-i", "--entrada", type=Path, default=aqui,
                        help="carpeta con los .md (por defecto, la del script)")
    parser.add_argument("-o", "--salida", type=Path, default=aqui / "site",
                        help="carpeta de salida (por defecto, docs/site)")
    parser.add_argument("-t", "--titulo", default="Bitacora de sesiones",
                        help="titulo del index.html")
    args = parser.parse_args()

    entrada = args.entrada.resolve()
    if not entrada.is_dir():
        print(f"La carpeta de entrada no existe: {entrada}", file=sys.stderr)
        return 1
    return generar(entrada, args.salida.resolve(), args.titulo)


if __name__ == "__main__":
    raise SystemExit(main())
