"""Logo de Magu's Elites: una M de bloques con cara de monstruo y corona, en pixel art 32x32."""
import random, sys
from PIL import Image

N = 32
random.seed(7)
out = sys.argv[1]

def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))

# ---- forma de la M (22 x 16) ----
W, H = 22, 16
m = set()
for k in range(H):
    for x in range(5):                      # patas
        m.add((x, k)); m.add((W - 1 - x, k))
    if k <= 9:                              # diagonales que bajan al centro
        for x in range(k + 1, k + 5):
            if x < W // 2:
                m.add((x, k)); m.add((W - 1 - x, k))
OX, OY = 5, 13                              # posición en el lienzo
M = {(x + OX, y + OY) for x, y in m}

# ---- corona (dorada, sobre la M) ----
crown_rows = [
    "#....#....#....#",
    "##..###..###..##",
    "################",
    "################",
    "################",
]
CX, CY = 8, 8
crown = {(x + CX, y + CY) for y, r in enumerate(crown_rows) for x, c in enumerate(r) if c == "#"}
gems = {(CX + 7, CY + 3): "aqua", (CX + 8, CY + 3): "aqua",
        (CX + 3, CY + 3): "yellow", (CX + 12, CY + 3): "yellow"}

img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
px = img.load()

# ---- fondo: losa de pizarra oscura con resplandor morado ----
for y in range(N):
    for x in range(N):
        d = ((x - 15.5) ** 2 + (y - 17) ** 2) ** 0.5
        t = max(0.0, 1 - d / 17)
        base = mix((22, 20, 30), (70, 34, 104), t ** 1.6)
        n = random.randint(-5, 5)
        px[x, y] = tuple(max(0, c + n) for c in base) + (255,)
# borde biselado tipo bloque
for i in range(N):
    for x, y in [(i, 0), (0, i), (i, N - 1), (N - 1, i)]:
        px[x, y] = (10, 9, 14, 255)
for i in range(1, N - 1):
    px[i, 1] = mix(px[i, 1][:3], (255, 255, 255), .12) + (255,)
    px[1, i] = mix(px[1, i][:3], (255, 255, 255), .12) + (255,)
    px[i, N - 2] = mix(px[i, N - 2][:3], (0, 0, 0), .35) + (255,)
    px[N - 2, i] = mix(px[N - 2, i][:3], (0, 0, 0), .35) + (255,)

def outline(shape, col):
    for (x, y) in shape:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                p = (x + dx, y + dy)
                if p not in shape and 1 < p[0] < N - 2 and 1 < p[1] < N - 2:
                    px[p] = col

# sombra/extrusión 3D de la M (abajo a la derecha)
shadow = {(x + 1, y + 1) for x, y in M} - M
solid = M | shadow | crown
outline(solid, (8, 6, 12, 255))
for p in shadow:
    px[p] = (20, 52, 22, 255)

# ---- M con piel de zombi (verde con ruido) ----
for (x, y) in M:
    g = random.choice([(78, 140, 62), (68, 124, 54), (90, 156, 70), (60, 110, 48), (84, 148, 66)])
    if (x - 1, y) not in M or (x, y - 1) not in M:      # luz arriba-izquierda
        g = mix(g, (190, 230, 150), .35)
    if (x + 1, y) not in M or (x, y + 1) not in M:      # sombra abajo-derecha
        g = mix(g, (20, 50, 20), .35)
    px[x, y] = g + (255,)

# ojos rojos brillantes en las diagonales
for ex, ey in [(OX + 7, OY + 5), (OX + 8, OY + 5), (OX + W - 9, OY + 5), (OX + W - 8, OY + 5)]:
    px[ex, ey] = (255, 60, 40, 255)
    px[ex, ey + 1] = (150, 20, 20, 255)

# ---- corona dorada ----
for (x, y) in crown:
    c = random.choice([(250, 200, 50), (240, 185, 40), (255, 215, 70)])
    if (x, y - 1) not in crown: c = mix(c, (255, 250, 200), .5)
    if (x, y + 1) not in crown: c = mix(c, (150, 95, 10), .5)
    px[x, y] = c + (255,)
for p, g in gems.items():
    px[p] = {"aqua": (80, 240, 255, 255), "yellow": (200, 70, 255, 255)}[g]
    if g == "aqua": px[p[0], p[1] + 1] = (30, 150, 190, 255)
    else: px[p[0], p[1] + 1] = (110, 30, 160, 255)

# ---- chispas de los rangos (amarillo, aqua, dorado) ----
def spark(x, y, col, big=False):
    px[x, y] = col + (255,)
    if big:
        for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
            if (x + dx, y + dy) not in solid:
                px[x + dx, y + dy] = mix(col, (60, 30, 90), .45) + (255,)
spark(4, 5, (255, 250, 110), True)
spark(27, 4, (80, 240, 255), True)
spark(28, 12, (255, 200, 50))
spark(3, 11, (80, 240, 255))
spark(25, 9, (255, 250, 110))
spark(6, 2, (255, 200, 50))

img.save(out.replace(".png", "_32.png"))
img.resize((512, 512), Image.NEAREST).save(out)
