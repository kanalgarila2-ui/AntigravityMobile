#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PIL (Pillow) Launcher Icon Generator for Antigravity 2.0 Android (ru.nanodev.antigravity)
Generates adaptive icon layers (432x432), Play Store 512x512 icon, and all mipmap-* PNGs.
Can be run locally or inside GitHub Actions CI before `./gradlew assembleRelease`.
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter

def render_antigravity_icon(size: int = 512, round_mask: bool = False, transparent_bg: bool = False) -> Image.Image:
    # Render at 2x supersampling for crisp anti-aliased geometry
    ss = 2
    w = size * ss
    img = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    if not transparent_bg:
        # Deep space obsidian background with subtle radial indigo/cyan glow
        bg = Image.new("RGBA", (w, w), (11, 14, 20, 255))
        bg_draw = ImageDraw.Draw(bg)
        cx, cy = w // 2, w // 2
        max_r = int(w * 0.55)
        for r in range(max_r, 0, -4):
            t = 1.0 - (r / max_r)
            col = (
                int(11 + 28 * t),
                int(14 + 55 * t),
                int(20 + 95 * t),
                255
            )
            bg_draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col)

        # Mask into squircle or circle
        mask = Image.new("L", (w, w), 0)
        mdraw = ImageDraw.Draw(mask)
        pad = int(w * 0.04)
        if round_mask:
            mdraw.ellipse([pad, pad, w - pad, w - pad], fill=255)
        else:
            radius = int(w * 0.22)
            mdraw.rounded_rectangle([pad, pad, w - pad, w - pad], radius=radius, fill=255)

        img.paste(bg, (0, 0), mask)
        draw = ImageDraw.Draw(img)

        # Subtle glowing border on squircle/circle
        border_pad = int(w * 0.045)
        if round_mask:
            draw.ellipse([border_pad, border_pad, w - border_pad, w - border_pad], outline=(0, 229, 255, 95), width=max(2, w // 90))
        else:
            draw.rounded_rectangle(
                [border_pad, border_pad, w - border_pad, w - border_pad],
                radius=int(w * 0.22),
                outline=(79, 140, 255, 110),
                width=max(2, w // 90)
            )

    cx, cy = w // 2, w // 2

    # 1. Outer Anti-Gravity Orbital Ring
    ring_r = int(w * 0.33)
    ring_w = max(4, int(w * 0.028))
    draw.arc(
        [cx - ring_r, cy - ring_r, cx + ring_r, cy + ring_r],
        start=200, end=520,
        fill=(0, 229, 255, 220),
        width=ring_w
    )
    draw.arc(
        [cx - int(ring_r * 1.08), cy - int(ring_r * 0.62), cx + int(ring_r * 1.08), cy + int(ring_r * 0.62)],
        start=20, end=165,
        fill=(79, 140, 255, 180),
        width=max(3, int(w * 0.018))
    )

    # 2. Central Floating "A" / Delta Anti-Gravity Core
    top_y = int(cy - w * 0.23)
    bot_y = int(cy + w * 0.19)
    half_span = int(w * 0.19)

    # Outer triangle points
    outer_pts = [
        (cx, top_y),
        (cx + half_span, bot_y),
        (cx + int(half_span * 0.52), bot_y),
        (cx, int(top_y + (bot_y - top_y) * 0.42)),
        (cx - int(half_span * 0.52), bot_y),
        (cx - half_span, bot_y),
    ]
    draw.polygon(outer_pts, fill=(255, 255, 255, 255))

    # Floating cyan energy crossbar (levitating inside the A)
    bar_y1 = int(cy + w * 0.01)
    bar_y2 = int(cy + w * 0.065)
    bar_x_span = int(w * 0.11)
    draw.rounded_rectangle(
        [cx - bar_x_span, bar_y1, cx + bar_x_span, bar_y2],
        radius=max(2, (bar_y2 - bar_y1) // 2),
        fill=(0, 229, 255, 255)
    )

    # Top levitating spark / satellite dot
    dot_r = max(3, int(w * 0.026))
    dot_y = int(top_y - w * 0.055)
    draw.ellipse([cx - dot_r, dot_y - dot_r, cx + dot_r, dot_y + dot_r], fill=(0, 229, 255, 255))

    return img.resize((size, size), Image.Resampling.LANCZOS)

def generate_all_android_icons(project_root: str = "."):
    res_dir = os.path.join(project_root, "app/src/main/res")
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, px in densities.items():
        target_dir = os.path.join(res_dir, folder)
        os.makedirs(target_dir, exist_ok=True)
        render_antigravity_icon(px, round_mask=False, transparent_bg=False).save(
            os.path.join(target_dir, "ic_launcher.png"), "PNG"
        )
        render_antigravity_icon(px, round_mask=True, transparent_bg=False).save(
            os.path.join(target_dir, "ic_launcher_round.png"), "PNG"
        )

    # Adaptive icon foreground & background (432x432)
    drawable_dir = os.path.join(res_dir, "drawable")
    os.makedirs(drawable_dir, exist_ok=True)
    render_antigravity_icon(432, round_mask=False, transparent_bg=True).save(
        os.path.join(drawable_dir, "ic_launcher_foreground.png"), "PNG"
    )
    bg_img = Image.new("RGBA", (432, 432), (11, 14, 20, 255))
    bg_img.save(os.path.join(drawable_dir, "ic_launcher_background.png"), "PNG")

    # Adaptive icon XMLs for Android 8.0+ / Android 11+
    anydpi_dir = os.path.join(res_dir, "mipmap-anydpi-v26")
    os.makedirs(anydpi_dir, exist_ok=True)
    adaptive_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""
    with open(os.path.join(anydpi_dir, "ic_launcher.xml"), "w", encoding="utf-8") as f:
        f.write(adaptive_xml)
    with open(os.path.join(anydpi_dir, "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
        f.write(adaptive_xml)

    # High-res 512x512 Play Store / Repo preview icon
    playstore_path = os.path.join(project_root, "app/src/main/ic_launcher-playstore.png")
    render_antigravity_icon(512, round_mask=False, transparent_bg=False).save(playstore_path, "PNG")
    print("Generated all Android mipmap & adaptive icons via Pillow (PIL).")

if __name__ == "__main__":
    generate_all_android_icons(".")
