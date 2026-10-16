#!/usr/bin/env python3
"""Check that a font maps the UCSUR sitelen pona codepoints used by the dictionary.

Dependency-free: parses the sfnt cmap subtable directly (format 4 and 12).
"""
import json, struct, sys, pathlib

ROOT = pathlib.Path(__file__).resolve().parent

def tables(d):
    n = struct.unpack(">H", d[4:6])[0]
    out = {}
    for i in range(n):
        off = 12 + 16 * i
        tag = d[off:off + 4].decode("latin1")
        toff, tlen = struct.unpack(">II", d[off + 8:off + 16])
        out[tag] = (toff, tlen)
    return out

def cmap_lookup(d, t):
    toff, _ = t["cmap"]
    n = struct.unpack(">H", d[toff + 2:toff + 4])[0]
    best = None
    for i in range(n):
        rec = toff + 4 + 8 * i
        pid, eid, sub = struct.unpack(">HHI", d[rec:rec + 8])
        fmt = struct.unpack(">H", d[toff + sub:toff + sub + 2])[0]
        if fmt in (4, 12):
            # prefer full-unicode (pid 3 eid 10 / pid 0)
            score = (fmt, pid, eid)
            if best is None or (fmt == 12 and best[0] != 12):
                best = (fmt, pid, eid, toff + sub)
    return best

def has_cp(d, sub_off, fmt, cp):
    if fmt == 12:
        n = struct.unpack(">I", d[sub_off + 12:sub_off + 16])[0]
        lo, hi = 0, n - 1
        while lo <= hi:
            mid = (lo + hi) // 2
            p = sub_off + 16 + 12 * mid
            s, e, g = struct.unpack(">III", d[p:p + 12])
            if cp < s: hi = mid - 1
            elif cp > e: lo = mid + 1
            else: return g + (cp - s)
        return None
    if fmt == 4:
        segx2 = struct.unpack(">H", d[sub_off + 6:sub_off + 8])[0]
        seg = segx2 // 2
        ends = struct.unpack(">" + "H" * seg, d[sub_off + 14:sub_off + 14 + segx2])
        starts_off = sub_off + 16 + segx2
        starts = struct.unpack(">" + "H" * seg, d[starts_off:starts_off + segx2])
        deltas_off = starts_off + segx2
        deltas = struct.unpack(">" + "h" * seg, d[deltas_off:deltas_off + segx2])
        ranges_off = deltas_off + segx2
        ranges = struct.unpack(">" + "H" * seg, d[ranges_off:ranges_off + segx2])
        for i in range(seg):
            if starts[i] <= cp <= ends[i]:
                if ranges[i] == 0:
                    return (cp + deltas[i]) & 0xFFFF
                addr = ranges_off + 2 * i + ranges[i] + 2 * (cp - starts[i])
                if addr + 2 <= len(d):
                    g = struct.unpack(">H", d[addr:addr + 2])[0]
                    return (g + deltas[i]) & 0xFFFF if g else None
                return None
        return None
    return None

def main():
    font = ROOT / "data" / "nasin-nanpa.otf"
    if len(sys.argv) > 1:
        font = pathlib.Path(sys.argv[1])
    d = font.read_bytes()
    t = tables(d)
    if "cmap" not in t:
        print("NO cmap table"); return 1
    best = cmap_lookup(d, t)
    if not best:
        print("no usable cmap subtable"); return 1
    fmt, pid, eid, sub = best
    print(f"font: {font.name}  cmap subtable: format {fmt} (pid {pid}, eid {eid})")

    doc = json.load(open(ROOT.parent / "app" / "src" / "main" / "assets" / "dictionary.json"))
    ok, missing = 0, []
    for w in doc["words"]:
        if not w["glyph"]:
            continue
        cp = int(w["glyph"].replace("U+", ""), 16)
        if has_cp(d, sub, fmt, cp):
            ok += 1
        else:
            missing.append((w["word"], w["glyph"]))
    print(f"dictionary glyphs mapped by this font: {ok}")
    if missing:
        print(f"NOT mapped ({len(missing)}): {missing[:12]}")
    # also the word "pona" and a few particles
    for word in ("pona", "mi", "sina", "li", "e", "toki", "jan", "ala"):
        w = next((x for x in doc["words"] if x["word"] == word), None)
        if w and w["glyph"]:
            cp = int(w["glyph"].replace("U+", ""), 16)
            print(f"  {word:8s} {w['glyph']} -> {'mapped' if has_cp(d, sub, fmt, cp) else 'MISSING'}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
