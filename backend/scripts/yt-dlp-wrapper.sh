#!/usr/bin/env bash
set -euo pipefail

URL="$1"
OUTDIR="$2"
FORMAT="$3"
QUALITY="$4"

mkdir -p "$OUTDIR"

# basic safe filename template
OUT="${OUTDIR}/%(title)s [%(id)s].%(ext)s"

CMD=(yt-dlp --no-playlist -o "$OUT")

if [ -n "$FORMAT" ]; then
  CMD+=( -f "$FORMAT" )
fi

if [ "$QUALITY" = "audio" ]; then
  CMD+=( --extract-audio --audio-format mp3 )
fi

CMD+=("$URL")

echo "Running: ${CMD[*]}"
"${CMD[@]}"
