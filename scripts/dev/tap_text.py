#!/usr/bin/env python3
"""Нажать на элемент экрана эмулятора по тексту: tap_text.py <adb> <текст> [индекс, -1 = последний]."""
import sys, re, subprocess
adb, needle = sys.argv[1], sys.argv[2]
idx = int(sys.argv[3]) if len(sys.argv) > 3 else 0
subprocess.run([adb, 'shell', 'uiautomator', 'dump', '/sdcard/ui.xml'], capture_output=True)
xml = subprocess.run([adb, 'shell', 'cat', '/sdcard/ui.xml'], capture_output=True, text=True).stdout
hits = []
for m in re.finditer(r'<node[^>]*>', xml):
    n = m.group(0)
    t = re.search(r'text="([^"]*)"', n).group(1)
    d = re.search(r'content-desc="([^"]*)"', n).group(1)
    if needle in t or needle in d:
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        hits.append(((int(b.group(1)) + int(b.group(3))) // 2, (int(b.group(2)) + int(b.group(4))) // 2))
if not hits or idx >= len(hits) or idx < -len(hits):
    print('NOT FOUND', needle); sys.exit(1)
cx, cy = hits[idx]
subprocess.run([adb, 'shell', 'input', 'tap', str(cx), str(cy)])
print('tapped', needle, cx, cy)
