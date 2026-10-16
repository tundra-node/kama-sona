#!/usr/bin/env python3
"""Build kama-sona's bundled dictionary.json + audio manifest from a lipu Linku (sona) checkout.

Source: https://github.com/lipu-linku/sona (data CC BY-SA 4.0)
Glyphs: https://github.com/lipu-linku/sona glyphs/metadata (UCSUR codepoints)
Audio:  https://github.com/lipu-linku/ijo kalama/<author>/<word>.mp3
"""
import json, os, sys, tomllib, datetime, pathlib

ROOT = pathlib.Path(__file__).resolve().parent
SONA = ROOT / "data" / "sona"
OUT = ROOT.parent / "app" / "src" / "main" / "assets"
VOICE = "kalaasi2023"          # primary voice to bundle
ALT_VOICE = "jlakuse"          # secondary (only recorded in the manifest)

def load_dir(d):
    out = {}
    for f in sorted(d.glob("*.toml")):
        try:
            with open(f, "rb") as fh:
                out[f.stem] = tomllib.load(fh)
        except Exception as e:
            print(f"  ! skip {f.name}: {e}", file=sys.stderr)
    return out

def load_toml(p):
    if not p.exists():
        return {}
    with open(p, "rb") as fh:
        return tomllib.load(fh)

def main():
    meta = load_dir(SONA / "words" / "metadata")
    glyph_meta = load_dir(SONA / "glyphs" / "metadata")
    def_en = load_toml(SONA / "words" / "translations" / "en" / "definition.toml")
    comm_en = load_toml(SONA / "words" / "translations" / "en" / "commentary.toml")
    etym_en = load_toml(SONA / "words" / "translations" / "en" / "etymology.toml")
    def_tp = load_toml(SONA / "words" / "source" / "definition.toml")

    # word -> ordered list of glyph records
    glyphs_by_word = {}
    all_glyphs = []
    for gid, g in sorted(glyph_meta.items()):
        rec = {
            "id": g.get("id", gid),
            "word": g.get("word", ""),
            "ucsur": g.get("ucsur", ""),
            "ligature": g.get("ligature", ""),
            "primary": bool(g.get("primary", False)),
            "deprecated": bool(g.get("deprecated", False)),
            "origin": ", ".join(g.get("author", []) or []),
        }
        all_glyphs.append(rec)
        glyphs_by_word.setdefault(rec["word"], []).append(rec)

    words, want_audio = [], []
    for w, m in meta.items():
        if m.get("deprecated"):
            continue
        primary = m.get("primary_glyph_id", "")
        gl = glyphs_by_word.get(w, [])
        pg = next((g for g in gl if g["id"] == primary), gl[0] if gl else None)
        audio = []
        for a in m.get("audio", []) or []:
            link = a.get("link", "")
            if not link:
                continue
            src = "unknown"
            for cand in (VOICE, ALT_VOICE):
                if f"/{cand}/" in link:
                    src = cand
            name = link.rsplit("/", 1)[-1]
            audio.append({"by": a.get("author", ""), "source": src, "file": name})
            if src == VOICE:
                want_audio.append((w, link, name))
        words.append({
            "word": w,
            "book": m.get("book", ""),
            "category": m.get("usage_category", ""),
            "def_en": def_en.get(w, ""),
            "def_tp": def_tp.get(w, ""),
            "commentary": comm_en.get(w, ""),
            "etymology": etym_en.get(w, ""),
            "source_language": m.get("source_language", ""),
            "see_also": m.get("see_also", []) or [],
            "era": m.get("coined_era", ""),
            "glyph": pg["ucsur"] if pg else "",
            "ligature": pg["ligature"] if pg else "",
            "glyph_id": pg["id"] if pg else "",
            "glyphs": [g["id"] for g in gl],
            "audio": audio,
        })

    order = {"core": 0, "common": 1, "uncommon": 2, "obscure": 3, "sandbox": 4}
    words.sort(key=lambda x: (order.get(x["category"], 9), x["word"]))

    doc = {
        "generated": datetime.date.today().isoformat(),
        "attribution": "Dictionary data: lipu Linku (sona, CC BY-SA 4.0). "
                       "Glyphs: nasin-nanpa by jan Itan (MIT). "
                       "Audio: community recordings via lipu-linku/ijo.",
        "words": words,
        "glyphs": all_glyphs,
    }
    OUT.mkdir(parents=True, exist_ok=True)
    with open(OUT / "dictionary.json", "w") as fh:
        json.dump(doc, fh, ensure_ascii=False, separators=(",", ":"))

    # audio fetch list for the shell side
    with open(ROOT / "audio_urls.txt", "w") as fh:
        for w, link, name in want_audio:
            fh.write(f"{w}\t{link}\n")

    cats = {}
    for w in words:
        cats[w["category"]] = cats.get(w["category"], 0) + 1
    print(f"words: {len(words)}  glyphs: {len(all_glyphs)}  audio({VOICE}): {len(want_audio)}")
    print("categories:", cats)
    print("with primary glyph:", sum(1 for w in words if w["glyph"]))
    print("with en definition:", sum(1 for w in words if w["def_en"]))
    print("dictionary.json:", (OUT / "dictionary.json").stat().st_size, "bytes")

if __name__ == "__main__":
    main()
