#!/usr/bin/env python3
"""Simple server that shows a random poem from the Poemas/ folder on each page load."""

import argparse
import glob
import os
import random
import re
from http.server import HTTPServer, SimpleHTTPRequestHandler


# Resolve paths relative to this script's directory
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
POEM_DIR = os.path.join(BASE_DIR, "Poemas")


def find_poems():
    """Find all .md files in Poemas/ and extract author/title from the path."""
    poems = []
    for path in sorted(glob.glob(os.path.join(POEM_DIR, "**", "*.md"), recursive=True)):
        # e.g. Poemas/Poemas/Antonio Machado/Anoche cuando dormía.md
        rel = os.path.relpath(path, POEM_DIR)  # Poemas/Antonio Machado/Anoche cuando dormía.md
        parts = rel.replace("\\", "/").split("/")
        # last part is the filename without extension
        title = os.path.splitext(parts[-1])[0]
        # everything before the filename is the author path
        author = "/".join(parts[:-1])
        poems.append({"path": path, "author": author, "title": title})
    return poems


def read_poem(path):
    """Read a poem file and return its text."""
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


# Preload poems list so we can pick randomly
POEMS = find_poems()


class Handler(SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/":
            # Pick a random poem
            poem = random.choice(POEMS)
            text = read_poem(poem["path"])
            html = render_poem(poem["author"], poem["title"], text)
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(html.encode("utf-8"))
        else:
            super().do_GET()

    def log_message(self, format, *args):
        # Quiet logs
        pass


def render_poem(author, title, text):
    return f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title>
<style>
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  body {{
    font-family: Georgia, 'Times New Roman', serif;
    background: #fafafa;
    color: #222;
    display: flex;
    justify-content: center;
    padding: 3rem 1.5rem;
  }}
  .poem {{
    max-width: 48ch;
  }}
  .author {{
    font-size: 0.95rem;
    color: #666;
    margin-bottom: 0.5rem;
    font-style: italic;
  }}
  .title {{
    font-size: 1.5rem;
    margin-bottom: 2rem;
    font-weight: normal;
  }}
  .text {{
    white-space: pre-line;
    line-height: 1.8;
    font-size: 1.05rem;
    text-align: justify;
  }}
</style>
</head>
<body>
<div class="poem">
  <div class="author">{author}</div>
  <h1 class="title">{title}</h1>
  <div class="text">{text}</div>
</div>
</body>
</html>"""


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()

    os.chdir(BASE_DIR)
    print(f"Serving {len(POEMS)} poems from {POEM_DIR}")
    print(f"Open http://localhost:{args.port}")
    HTTPServer(("0.0.0.0", args.port), Handler).serve_forever()
