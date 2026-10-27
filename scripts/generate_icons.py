#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор иконок Antigravity 2.0 (ru.nanodev.antigravity).

Метка: «A» из толстых скруглённых штрихов с диагональным градиентом
(синий -> фиолетовый -> коралловый) и мягким свечением.
Рисуется с 4x супер-сэмплингом и уменьшается LANCZOS-фильтром.

Выходы:
  app/src/main/res/mipmap-*/ic_launcher{,_round}.png
  app/src/main/res/drawable/ic_launcher_foreground.png   (adaptive, 432x432, прозрачный фон)
  app/src/main/res/drawable/ic_launcher_background.png   (adaptive, 432x432, фон)
  app/src/main/ic_launcher-playstore.png                 (512x512, непрозрачный, для Play)
"""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

SS = 4  # супер-сэмплинг

# Точки градиента: позиция 0..1 -> RGB
PALETTE_POS = [0.00, 0.42, 0.74, 1.00]
PALETTE_RGB = np.array([
    [79, 195, 255],   # голубой
    [66, 133, 244],   # синий
    [155, 107, 255],  # фиолетовый
    [255, 138, 101],  # коралловый
], dtype=np.float32)


def diag_gradient(w: int) -> Image.Image:
    ys, xs = np.mgrid[0:w, 0:w].astype(np.float32)
    t = (xs + ys) / (2.0 * (w - 1))
    t = np.clip((t - 0.30) / 0.42, 0.0, 1.0)  # метка занимает ~30..72% холста
    out = np.empty((w, w, 3), dtype=np.float32)
    for ch in range(3):
        out[..., ch] = np.interp(t, PALETTE_POS, PALETTE_RGB[:, ch])
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def background(w: int) -> Image.Image:
    ys, xs = np.mgrid[0:w, 0:w].astype(np.float32)
    # Вертикальный градиент тёмного фона + мягкое световое пятно сверху
    top = np.array([16, 22, 42], np.float32)
    bot = np.array([3, 5, 11], np.float32)
    t = (ys / w)[..., None]
    base = top * (1 - t) + bot * t
    d = np.sqrt((xs - w * 0.5) ** 2 + (ys - w * 0.30) ** 2) / (w * 0.75)
    glow = np.clip(1 - d, 0, 1) ** 2
    col = base + glow[..., None] * np.array([34, 64, 128], np.float32)
    return Image.fromarray(np.clip(col, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def _p(w, f, x, y):
    """Нормированные координаты метки -> пиксели холста с масштабом f."""
    return (w / 2 + (x - 0.5) * w * f, w / 2 + (y - 0.5) * w * f)


def mark_mask(w: int, f: float, sw_k: float = 0.125) -> Image.Image:
    """Маска метки 'A' и искорки (L-режим), размер w x w."""
    m = Image.new("L", (w, w), 0)
    d = ImageDraw.Draw(m)
    sw = sw_k * w * f

    apex, lf, rf = (0.50, 0.24), (0.25, 0.78), (0.75, 0.78)
    # Точка левой и правой ноги на высоте перекладины y=0.60
    ty = (0.60 - apex[1]) / (lf[1] - apex[1])
    cl = (apex[0] + (lf[0] - apex[0]) * ty, 0.60)
    cr = (apex[0] + (rf[0] - apex[0]) * ty, 0.60)

    legs = [_p(w, f, *apex), _p(w, f, *lf)]
    d.line(legs, fill=255, width=int(round(sw)), joint="curve")
    d.line([_p(w, f, *apex), _p(w, f, *rf)], fill=255, width=int(round(sw)), joint="curve")
    d.line([_p(w, f, *cl), _p(w, f, *cr)], fill=255, width=int(round(sw * 0.82)))

    # Скруглённые концы
    r = sw / 2
    for x, y in [apex, lf, rf]:
        px, py = _p(w, f, x, y)
        d.ellipse([px - r, py - r, px + r, py + r], fill=255)

    return m


def mark_layer(w: int, f: float) -> Image.Image:
    """RGBA-слой с градиентной меткой и свечением. Прозрачный фон."""
    mask = mark_mask(w, f)
    grad = diag_gradient(w)

    # Свечение: размытая маска градиентом, полупрозрачно
    glow_alpha = mask.filter(ImageFilter.GaussianBlur(radius=w * 0.035))
    glow_alpha = glow_alpha.point(lambda v: int(v * 0.55))
    glow = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    glow.paste(grad, (0, 0), glow_alpha)

    # Сама метка
    solid = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    solid.paste(grad, (0, 0), mask)

    out = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    out.alpha_composite(glow)
    out.alpha_composite(solid)
    return out


def full_icon(size: int, round_mask: bool) -> Image.Image:
    w = size * SS
    bg = background(w)
    layer = mark_layer(w, f=0.66)  # полноразмерная метка на полном холсте

    canvas = bg.copy()
    canvas.alpha_composite(layer)

    if round_mask:
        m = Image.new("L", (w, w), 0)
        ImageDraw.Draw(m).ellipse([0, 0, w - 1, w - 1], fill=255)
        canvas.putalpha(m)
    return canvas.resize((size, size), Image.Resampling.LANCZOS)


def foreground_layer(size: int = 432) -> Image.Image:
    """Adaptive-foreground: метка в безопасной зоне (66% диаметра), прозрачный фон."""
    w = size * SS
    layer = mark_layer(w, f=0.70)
    return layer.resize((size, size), Image.Resampling.LANCZOS)


def generate_all_android_icons(project_root: str = "."):
    res = os.path.join(project_root, "app/src/main/res")
    densities = {
        "mipmap-mdpi": 48, "mipmap-hdpi": 72, "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144, "mipmap-xxxhdpi": 192,
    }
    for folder, px in densities.items():
        d = os.path.join(res, folder)
        os.makedirs(d, exist_ok=True)
        full_icon(px, round_mask=False).save(os.path.join(d, "ic_launcher.png"), "PNG")
        full_icon(px, round_mask=True).save(os.path.join(d, "ic_launcher_round.png"), "PNG")

    dr = os.path.join(res, "drawable")
    os.makedirs(dr, exist_ok=True)
    foreground_layer(432).save(os.path.join(dr, "ic_launcher_foreground.png"), "PNG")
    background(432 * SS).resize((432, 432), Image.Resampling.LANCZOS).save(
        os.path.join(dr, "ic_launcher_background.png"), "PNG")

    # Play Store: непрозрачный квадрат 512x512, без закруглений (их делает Play сам)
    full_icon(512, round_mask=False).convert("RGB").save(
        os.path.join(project_root, "app/src/main/ic_launcher-playstore.png"), "PNG")
    print("Иконки Antigravity сгенерированы.")


if __name__ == "__main__":
    generate_all_android_icons(".")
