#!/usr/bin/env python3
"""Generate launcher PNGs for kama sona.

Dependency-free PNG writer (zlib + CRC32) with 4x supersampling. The mark is the
same "ilo" arch+base+stem used in the vector drawable, so the icon set is
self-consistent. Vector drawables as android:icon render blank in some API-24
launchers, so real bitmaps are safer on this device.
"""
import os, struct, zlib, pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"

BG = (10, 14, 18)      # #0A0E12
FG = (34, 211, 238)    # #22D3EE
SS = 4                 # supersample factor


def rounded_rect(x, y, x0, y0, x1, y1, r):
    if x < x0 or x > x1 or y < y0 or y > y1:
        return False
    cx = min(max(x, x0 + r), x1 - r)
    cy = min(max(y, y0 + r), y1 - r)
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def inside(x, y, n):
    """Shape test in an n x n design space. True = foreground."""
    s = n / 48.0

    def R(x0, y0, x1, y1, r):
        return rounded_rect(x, y, x0 * s, y0 * s, x1 * s, y1 * s, r * s)

    # arch: outer frame minus inner, open at the bottom
    outer = R(10, 10, 38, 40, 7)
    inner = R(15, 15, 33, 40, 3)
    arch = outer and not inner and y < 34 * s
    # base bar
    base = R(6, 33, 42, 39, 2) and y >= 33 * s
    # stem
    stem = R(21.5, 17, 26.5, 33, 1.5)
    return arch or base or stem


def render(n):
    px = bytearray()
    for y in range(n):
        row = bytearray()
        for x in range(n):
            hits = 0
            for sy in range(SS):
                for sx in range(SS):
                    fx = x + (sx + 0.5) / SS
                    fy = y + (sy + 0.5) / SS
                    if inside(fx, fy, n):
                        hits += 1
            a = hits / (SS * SS)
            r = int(BG[0] * (1 - a) + FG[0] * a)
            g = int(BG[1] * (1 - a) + FG[1] * a)
            b = int(BG[2] * (1 - a) + FG[2] * a)
            row += bytes((r, g, b, 255))
        px += b"\x00" + row
    return px


def chunk(tag, data):
    return (struct.pack(">I", len(data)) + tag + data +
            struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))


def write_png(path, n):
    raw = render(n)
    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", n, n, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)
    return len(png)


# density -> launcher icon px
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

def main():
    for dens, px in DENSITIES.items():
        p = RES / f"mipmap-{dens}" / "ic_launcher.png"
        size = write_png(p, px)
        print(f"{p.relative_to(ROOT)}  {px}x{px}  {size} bytes")


if __name__ == "__main__":
    main()
