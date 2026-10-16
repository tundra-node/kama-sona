#!/usr/bin/env python3
"""Tiny adb driver: dump -> locate -> tap -> verify, for the S6.

Text is matched as a substring unless --exact. Taps the centre of the node.
  device.py dump                 list visible text + bounds
  device.py tap "text"           tap first node whose text contains "text"
  device.py tap-exact "text"     exact text match
  device.py tap-id "res-id"      tap by resource-id (package prefix optional)
  device.py shot out.png         screencap
  device.py type "hello"         input text
  device.py key back|home|enter
  device.py wait "text" [secs]   poll dump until text appears
"""
import re, subprocess, sys, time

PKG = "com.elias.kamasona"
XML = "/sdcard/ui.xml"


def dump():
    subprocess.run(["adb", "shell", "uiautomator", "dump", XML],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    out = subprocess.run(["adb", "exec-out", "cat", XML], capture_output=True)
    return out.stdout.decode("utf-8", "replace")


def nodes(x):
    out = []
    for m in re.finditer(r"<node[^>]*>", x):
        s = m.group(0)

        def g(k):
            r = re.search(k + r'="([^"]*)"', s)
            return r.group(1) if r else ""

        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', s)
        out.append({
            "text": g("text"), "desc": g("content-desc"), "cls": g("class"),
            "res": g("resource-id"), "click": g("clickable") == "true",
            "bounds": [int(v) for v in b.groups()] if b else None,
        })
    return out


def centre(b):
    return (b[0] + b[2]) // 2, (b[1] + b[3]) // 2


def find(ns, needle, exact=False, clickable_only=False):
    for n in ns:
        for field in ("text", "desc"):
            v = n[field]
            if not v:
                continue
            if (v == needle) if exact else (needle in v):
                if clickable_only and not n["click"]:
                    continue
                return n
    return None


def tap_text(needle, exact=False):
    ns = nodes(dump())
    n = find(ns, needle, exact, clickable_only=True) or find(ns, needle, exact)
    if not n or not n["bounds"]:
        print(f"NOT FOUND: {needle!r}")
        return 1
    x, y = centre(n["bounds"])
    subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)])
    print(f"tapped {needle!r} at ({x},{y})")
    return 0


def main():
    if len(sys.argv) < 2:
        print(__doc__); return 1
    cmd = sys.argv[1]
    if cmd == "dump":
        for n in nodes(dump()):
            t = n["text"] or ("[" + n["desc"] + "]")
            if t.strip("[]") and n["bounds"] and n["bounds"] != [0, 0, 0, 0]:
                print(f"{t[:70]!r:74} {n['bounds']} {'C' if n['click'] else ''} {n['res']}")
    elif cmd in ("tap", "tap-exact"):
        return tap_text(sys.argv[2], exact=(cmd == "tap-exact"))
    elif cmd == "tap-id":
        ns = nodes(dump())
        n = next((n for n in ns if n["res"].endswith(sys.argv[2])), None)
        if not n or not n["bounds"]:
            print("NOT FOUND id:", sys.argv[2]); return 1
        x, y = centre(n["bounds"])
        subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)])
        print(f"tapped id {sys.argv[2]} at ({x},{y})")
    elif cmd == "shot":
        subprocess.run(f"adb exec-out screencap -p > {sys.argv[2]}", shell=True)
        print("saved", sys.argv[2])
    elif cmd == "type":
        subprocess.run(["adb", "shell", "input", "text", sys.argv[2].replace(" ", "%s")])
    elif cmd == "key":
        subprocess.run(["adb", "shell", "input", "keyevent", sys.argv[2]])
    elif cmd == "wait":
        needle = sys.argv[2]
        secs = float(sys.argv[3]) if len(sys.argv) > 3 else 10
        t0 = time.time()
        while time.time() - t0 < secs:
            if find(nodes(dump()), needle):
                print("found:", needle); return 0
            time.sleep(1)
        print("TIMEOUT waiting for:", needle); return 1
    else:
        print(__doc__); return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
