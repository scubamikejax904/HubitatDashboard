#!/usr/bin/env bash
# Regenerate docs/USER_GUIDE.pdf from docs/USER_GUIDE.md.
#
# Pure-Python pipeline (markdown -> HTML -> pdf via xhtml2pdf): no LaTeX, pandoc,
# or system packages are required. On first run it creates a local virtualenv at
# docs/.venv (gitignored) and installs the three Python deps.
#
# Usage:
#   bash docs/gen-pdf.sh
#
# The PDF is a DERIVED artifact of docs/USER_GUIDE.md — edit the .md, then run
# this to refresh the .pdf.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCS="$ROOT/docs"
MD="$DOCS/USER_GUIDE.md"
OUT="$DOCS/USER_GUIDE.pdf"
VENV="$DOCS/.venv"
PY="$VENV/bin/python"

# --- 1. virtualenv + deps (one-time) ---------------------------------------
if [[ ! -x "$PY" ]]; then
  echo "  [gen-pdf] Creating venv at $VENV ..."
  python3 -m venv "$VENV"
  "$VENV/bin/pip" install --quiet --disable-pip-version-check \
      markdown xhtml2pdf fonttools
fi

# --- 2. Find a Unicode font to embed ----------------------------------------
# xhtml2pdf refuses to read files outside the "document tree", so we COPY the
# font into the venv (inside the repo) and reference it from there. Embedding a
# real Unicode font makes em-dashes, curly quotes, and symbols render instead of
# showing as boxes in the built-in Helvetica.
pick() { # $@ candidate paths -> echoes first that exists
  local c
  for c in "$@"; do [[ -f "$c" ]] && { echo "$c"; return 0; }; done
  return 1
}

REGULAR="$(pick \
  /usr/share/fonts/truetype/noto/NotoSans-Regular.ttf \
  /usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf \
  /usr/share/fonts/truetype/dejavu/DejaVuSans.ttf \
  /usr/share/fonts/dejavu/DejaVuSans.ttf \
  /usr/share/fonts/TTF/DejaVuSans.ttf \
  "/System/Library/Fonts/Supplemental/Arial Unicode.ttf" \
  "/Library/Fonts/Arial Unicode.ttf" \
)" || REGULAR=""

FONTDIR="$VENV/fonts"
mkdir -p "$FONTDIR"
FONT_NAME=""
FONT_BOLD=""

if [[ -n "$REGULAR" ]]; then
  cp -f "$REGULAR" "$FONTDIR/embedded-font.ttf"
  FONT_NAME="embedded-font.ttf"
  base="${REGULAR%.ttf}"
  BOLD="$(pick \
    "${base%-Regular}-Bold.ttf" \
    "${base%-Sans-Regular}-Sans-Bold.ttf" \
    "${base}-Bold.ttf" \
    "${base} Bold.ttf")" || BOLD=""
  if [[ -n "$BOLD" ]]; then
    cp -f "$BOLD" "$FONTDIR/embedded-font-bold.ttf"
    FONT_BOLD="embedded-font-bold.ttf"
    echo "  [gen-pdf] Embedding font: $REGULAR (+ bold)"
  else
    echo "  [gen-pdf] Embedding font: $REGULAR (bold unavailable)"
  fi
else
  echo "  [gen-pdf] WARNING: no Unicode font found; PDF will use built-in"
  echo "             Helvetica, so em-dashes/symbols may display as boxes."
fi

# --- 3. Generate -------------------------------------------------------------
"$PY" - "$MD" "$OUT" "$FONTDIR" "$FONT_NAME" "$FONT_BOLD" <<'PY'
import os, sys, unicodedata
from fontTools.ttLib import TTFont
import markdown
from xhtml2pdf import pisa

md_path, out_path, fontdir, font_name, font_bold = sys.argv[1:6]

with open(md_path, "r", encoding="utf-8") as f:
    text = f.read()

# Glyph coverage: if we embedded a font, drop/replace anything it can't render.
ALT = {0x2192: "->", 0x22EE: "..."}   # right-arrow, vertical-ellipsis
font_regular = os.path.join(fontdir, font_name) if font_name else None
if font_regular and os.path.exists(font_regular):
    cmap = TTFont(font_regular).getBestCmap()
    for ch in set(text):
        if ord(ch) <= 127 or ord(ch) in cmap:
            continue
        text = text.replace(ch, ALT[ord(ch)] if ord(ch) in ALT else "")

body = markdown.markdown(text, extensions=["tables", "fenced_code", "sane_lists"])

def face(weight, style, path):
    return ('@font-face { font-family: "Fancy"; src: url("file://' + path +
            '"); font-weight: ' + weight + '; font-style: ' + style + '; }')

css_parts = []
if font_name:
    reg = os.path.join(fontdir, font_name)
    bol = os.path.join(fontdir, font_bold) if font_bold else reg
    css_parts.append(face("normal", "normal", reg))
    css_parts.append(face("bold", "normal", bol))
    css_parts.append(face("normal", "italic", reg))
    css_parts.append(face("bold", "italic", bol))

css = "\n".join(css_parts)
FAM = "Fancy" if font_name else "helvetica"
MONO = "Fancy" if font_name else "courier"
css += """
@page { size: letter; margin: 1.7cm 1.7cm 1.9cm 1.7cm; }
body { font-family: __FAM__; font-size: 9.5pt; line-height: 1.35; }
h1 { font-size: 16pt; color: #1a1a1a; -pdf-keep-with-next: true; }
h2 { font-size: 13pt; color: #222222; -pdf-keep-with-next: true; border-bottom: 0.6pt solid #bbbbbb; padding-bottom: 1.5pt; }
h3 { font-size: 11.5pt; color: #222222; -pdf-keep-with-next: true; }
h4 { font-size: 10.5pt; color: #222222; -pdf-keep-with-next: true; }
p, li { font-size: 9.5pt; }
table { width: 100%; font-size: 8pt; -pdf-keep-with-next: true; }
th, td { border: 1px solid #9a9a9a; padding: 2.5pt 3.5pt; font-size: 8pt; }
th { background: #e8e8e8; font-weight: bold; }
pre { background: #f3f3f3; border: 1px solid #cccccc; padding: 4pt 5pt; font-size: 7.2pt; }
code { font-family: __MONO__; font-size: 8pt; }
blockquote { margin: 4pt 9pt; color: #333333; border-left: 2pt solid #bbbbbb; padding: 2pt 6pt; }
"""
css = css.replace("__FAM__", FAM).replace("__MONO__", MONO)

html = ("<!DOCTYPE html><html><head><meta charset='utf-8'><style>"
        + css + "</style></head><body>\n" + body + "\n</body></html>")

with open(out_path, "wb") as out:
    status = pisa.CreatePDF(html, dest=out)
sys.exit(1 if status.err else 0)
PY

echo "  [gen-pdf] Wrote $OUT ($(du -h "$OUT" | cut -f1))"
echo "  [gen-pdf] Done."