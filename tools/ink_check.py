#!/usr/bin/env python3
"""Ink coverage check for a screencap region.

Dependency-free PNG decoder (zlib + unfilter, 8-bit truecolour) used to confirm
that something is actually *drawn* where a glyph should be. This is a
verification aid for a device whose screen cannot be inspected visually here.

usage: ink_check.py shot.png x0 y0 x1 y1   -> prints ink ratio + bounding box
"""
import struct, sys, zlib


def decode(path):
    d = open(path, "rb").read()
    assert d[:8] == b"\x89PNG\r\n\x1a\n", "not a png"
    pos = 8
    w = h = bitdepth = colortype = None
    idat = bytearray()
    while pos < len(d):
        ln = struct.unpack(">I", d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        body = d[pos + 8:pos + 8 + ln]
        if typ == b"IHDR":
            w, h, bitdepth, colortype = struct.unpack(">IIBB", body[:10])
        elif typ == b"IDAT":
            idat += body
        elif typ == b"IEND":
            break
        pos += 12 + ln
    assert bitdepth == 8, f"bitdepth {bitdepth} unsupported"
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[colortype]
    raw = zlib.decompress(bytes(idat))
    stride = w * nch
    out = bytearray(h * stride)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        ft = raw[p]; p += 1
        line = bytearray(raw[p:p + stride]); p += stride
        if ft == 1:
            for i in range(nch, stride):
                line[i] = (line[i] + line[i - nch]) & 0xFF
        elif ft == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif ft == 3:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif ft == 4:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                b = prev[i]
                c = prev[i - nch] if i >= nch else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        out[y * stride:(y + 1) * stride] = line
        prev = line
    return w, h, nch, out


def main():
    path = sys.argv[1]
    x0, y0, x1, y1 = (int(a) for a in sys.argv[2:6])
    w, h, nch, px = decode(path)
    stride = w * nch

    def rgb(x, y):
        o = y * stride + x * nch
        return px[o], px[o + 1], px[o + 2]

    # background = most common colour in the region
    from collections import Counter
    c = Counter()
    for y in range(y0, min(y1, h)):
        for x in range(x0, min(x1, w)):
            c[rgb(x, y)] += 1
    bg, bgn = c.most_common(1)[0]
    total = sum(c.values())
    # "ink" = pixels clearly different from the background colour
    ink = 0
    minx, miny, maxx, maxy = 10 ** 9, 10 ** 9, -1, -1
    for y in range(y0, min(y1, h)):
        for x in range(x0, min(x1, w)):
            r, g, b = rgb(x, y)
            if abs(r - bg[0]) + abs(g - bg[1]) + abs(b - bg[2]) > 90:
                ink += 1
                minx, miny = min(minx, x), min(miny, y)
                maxx, maxy = max(maxx, x), max(maxy, y)
    print(f"image {w}x{h} ch={nch}")
    print(f"region ({x0},{y0})-({x1},{y1})  bg={bg} ({100*bgn//total}% of region)")
    print(f"ink pixels: {ink} / {total} = {100*ink/max(total,1):.1f}%")
    if ink:
        print(f"ink bbox: ({minx},{miny})-({maxx},{maxy})  -> {maxx-minx+1}x{maxy-miny+1}px")
    # crude cover: fraction of the ink bbox that is background (low = solid stroke)
    if ink and maxx > minx:
        box_total = (maxx - minx + 1) * (maxy - miny + 1)
        print(f"ink density inside bbox: {100*ink/box_total:.1f}%")


if __name__ == "__main__":
    main()
