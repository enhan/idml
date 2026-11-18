#!/usr/bin/env python3
"""Prints the functions index (src/main/tut/functions/index.md): a link to every "## " section of every functions page.

Usage: generate-index.py <functions dir>
"""
import re
import sys
from pathlib import Path


def slug(header):
    # the header id Laika generates
    return re.sub(r"[^a-z0-9]+", "-", header.lower()).strip("-")


def headers(page, level):
    prefix = "#" * level + " "
    return [line[len(prefix):].strip() for line in page.read_text().splitlines() if line.startswith(prefix)]


def link(page, section):
    # a section named like its page shares the page title's id, which Laika rejects as ambiguous: link the page
    fragment = slug(section)
    same_as_title = fragment in (slug(title) for title in headers(page, 1))
    return page.name if same_as_title else "{}#{}".format(page.name, fragment)


def generate_index(directory):
    yield "# Functions"
    for page in sorted(p for p in directory.glob("*.md") if p.name != "index.md"):
        yield "## {}".format(page.stem)
        for section in headers(page, 2):
            yield "[{}]({})".format(section, link(page, section))


if __name__ == "__main__":
    assert slug("apply and applyArray") == "apply-and-applyarray"
    assert slug("Split, Join") == "split-join"
    print("\n\n".join(generate_index(Path(sys.argv[1]))))
