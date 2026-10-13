#!/usr/bin/env python3
"""Kontrastprüfung nach WCAG 2.x für die Luma-Care-Tokens.

Aufruf:
  contrast.py                      prüft alle Token-Paare aus PAIRS
  contrast.py "#FG" "#BG" [min]    prüft ein Paar (Standard-Mindestwert 4.5)

Exit-Code 1, wenn ein Paar den Mindestwert unterschreitet.
"""
import sys

TEXT = 4.5  # normaler Text
UI = 3.0    # Rahmen, Icons, große Schrift

# (Name, Vordergrund, Hintergrund, Mindestwert) – synchron halten mit references/tokens.md
PAIRS = [
    ("light primary auf surface", "#2E7F85", "#FFFFFF", TEXT),
    ("light onPrimary auf primary", "#FFFFFF", "#2E7F85", TEXT),
    ("light onSurface auf surface", "#211E32", "#FFFFFF", TEXT),
    ("light onSurface auf background", "#211E32", "#F7F7F9", TEXT),
    ("light muted auf surface", "#5C5A66", "#FFFFFF", TEXT),
    ("light outline auf surface", "#8E8B99", "#FFFFFF", UI),
    ("light outline auf background", "#8E8B99", "#F7F7F9", UI),
    ("light accent mit Text", "#211E32", "#FACF39", TEXT),
    ("light Kopfbereich Text auf secondary", "#FFFFFF", "#211E32", TEXT),
    ("dark primary auf surface", "#4AAFB6", "#211E32", TEXT),
    ("dark onPrimary auf primary", "#211E32", "#4AAFB6", TEXT),
    ("dark onSurface auf surface", "#ECEAF4", "#211E32", TEXT),
    ("dark onSurface auf background", "#ECEAF4", "#17151F", TEXT),
    ("dark muted auf surface", "#A9A6B6", "#211E32", TEXT),
    ("dark outline auf surface", "#807C92", "#211E32", UI),
    ("light muted auf surfaceContainerHighest", "#5C5A66", "#F0EFF4", TEXT),
    ("light onSurface auf surfaceContainerHighest", "#211E32", "#F0EFF4", TEXT),
    ("light onPrimaryContainer auf primaryContainer", "#17474B", "#D5EEF0", TEXT),
    ("light inverseOnSurface auf inverseSurface", "#ECEAF4", "#211E32", TEXT),
    ("light inversePrimary auf inverseSurface", "#4AAFB6", "#211E32", TEXT),
    ("light onError auf error", "#FFFFFF", "#B42318", TEXT),
    ("light onErrorContainer auf errorContainer", "#B42318", "#FEE4E2", TEXT),
    ("dark muted auf surfaceContainerHighest", "#A9A6B6", "#322F48", TEXT),
    ("dark onSurface auf surfaceContainerHighest", "#ECEAF4", "#322F48", TEXT),
    ("dark onPrimaryContainer auf primaryContainer", "#CDEEF0", "#1F4A4E", TEXT),
    ("dark inverseOnSurface auf inverseSurface", "#211E32", "#ECEAF4", TEXT),
    ("dark inversePrimary auf inverseSurface", "#256A70", "#ECEAF4", TEXT),
    ("dark onError auf error", "#3A1210", "#FDA29B", TEXT),
    ("dark onErrorContainer auf errorContainer", "#FDA29B", "#3A1210", TEXT),
    ("light urgent", "#B42318", "#FEE4E2", TEXT),
    ("light attention", "#93370D", "#FEF0C7", TEXT),
    ("light info", "#256A70", "#E3F4F5", TEXT),
    ("light done", "#067647", "#DCFAE6", TEXT),
    ("dark urgent", "#FDA29B", "#3A1210", TEXT),
    ("dark attention", "#FEC84B", "#33230A", TEXT),
    ("dark info", "#7FD3D9", "#10282A", TEXT),
    ("dark done", "#75E0A7", "#0B2A1B", TEXT),
]


def luminance(hex_color: str) -> float:
    h = hex_color.lstrip("#")
    if len(h) != 6:
        raise ValueError(f"Erwartet #RRGGBB, bekommen: {hex_color}")
    channels = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    lin = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
    return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]


def ratio(fg: str, bg: str) -> float:
    a, b = sorted((luminance(fg), luminance(bg)), reverse=True)
    return (a + 0.05) / (b + 0.05)


def check(name: str, fg: str, bg: str, minimum: float) -> bool:
    r = ratio(fg, bg)
    ok = r >= minimum
    print(f"{'OK ' if ok else 'FAIL'}  {r:5.2f} : 1  (min {minimum})  {fg} auf {bg}  {name}")
    return ok


def main(argv: list[str]) -> int:
    if len(argv) >= 2:
        minimum = float(argv[2]) if len(argv) > 2 else TEXT
        return 0 if check("Paar", argv[0], argv[1], minimum) else 1
    results = [check(*pair) for pair in PAIRS]
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
