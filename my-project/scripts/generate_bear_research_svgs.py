#!/usr/bin/env python3

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import cairosvg
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageOps


PALETTE = [
    (0x01, "#010101"),
    (0xB1, "#B1B1B1"),
    (0xD8, "#D8D8D8"),
    (0xE8, "#E8E8E8"),
    (0xFB, "#FBFBFB"),
]


@dataclass(frozen=True)
class ReferenceImage:
    key: str
    filename: str
    extra_cleanup: bool = False


REFERENCES = [
    ReferenceImage("cover-peek", "iCopy_2026_04_10_06_53_01.png"),
    ReferenceImage("success-lift", "iCopy_2026_04_10_06_53_07.png"),
    ReferenceImage("error-sad", "iCopy_2026_04_10_06_53_12.png", extra_cleanup=True),
    ReferenceImage("hello-wave", "iCopy_2026_04_10_06_53_16.png"),
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-dir", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    return parser.parse_args()


def rounded_rect_mask(height: int, width: int, radius: int) -> np.ndarray:
    mask = np.zeros((height, width), dtype=np.uint8)
    cv2.rectangle(mask, (radius, 0), (width - radius, height), 255, -1)
    cv2.rectangle(mask, (0, radius), (width, height - radius), 255, -1)
    cv2.circle(mask, (radius, radius), radius, 255, -1)
    cv2.circle(mask, (width - radius, radius), radius, 255, -1)
    cv2.circle(mask, (radius, height - radius), radius, 255, -1)
    cv2.circle(mask, (width - radius, height - radius), radius, 255, -1)
    return mask


def compute_visible_bounds(image: np.ndarray) -> tuple[int, int, int, int]:
    mask = image > 20
    coords = np.column_stack(np.where(mask))
    if coords.size == 0:
        return 0, 0, image.shape[1], image.shape[0]
    min_y, min_x = coords.min(axis=0)
    max_y, max_x = coords.max(axis=0)
    return min_x, min_y, max_x + 1, max_y + 1


def patch_watermark(crop: np.ndarray) -> None:
    patch_h = min(max(38, crop.shape[0] // 10), 58)
    patch_w = min(max(82, crop.shape[1] // 4), 132)
    start_x = min(18, max(0, crop.shape[1] - patch_w))
    end_y = min(crop.shape[0], patch_h)
    end_x = min(crop.shape[1], start_x + patch_w)
    crop[:end_y, start_x:end_x] = 0xFB


def quantize_grayscale(gray: np.ndarray, clusters: int = 6) -> np.ndarray:
    pixels = gray.reshape((-1, 1)).astype(np.float32)
    criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.1)
    _, labels, centers = cv2.kmeans(
        pixels,
        clusters,
        None,
        criteria,
        10,
        cv2.KMEANS_PP_CENTERS,
    )
    centers = centers.flatten()
    quantized = np.zeros(gray.size, dtype=np.uint8)
    palette_values = np.array([value for value, _ in PALETTE], dtype=np.int16)

    for index, label in enumerate(labels.flatten()):
        center = centers[label]
        nearest = int(palette_values[np.argmin(np.abs(palette_values - center))])
        quantized[index] = nearest

    return quantized.reshape(gray.shape)


def border_connected(mask: np.ndarray) -> np.ndarray:
    visited = np.zeros_like(mask, dtype=np.uint8)
    height, width = mask.shape
    stack: list[tuple[int, int]] = []

    def maybe_push(x: int, y: int) -> None:
        if x < 0 or y < 0 or x >= width or y >= height:
            return
        if visited[y, x] or mask[y, x] == 0:
            return
        visited[y, x] = 1
        stack.append((x, y))

    for x in range(width):
        maybe_push(x, 0)
        maybe_push(x, height - 1)
    for y in range(height):
        maybe_push(0, y)
        maybe_push(width - 1, y)

    while stack:
        x, y = stack.pop()
        maybe_push(x + 1, y)
        maybe_push(x - 1, y)
        maybe_push(x, y + 1)
        maybe_push(x, y - 1)

    return visited.astype(bool)


def contour_to_path(contour: np.ndarray) -> str:
    points = contour[:, 0, :]
    if len(points) < 2:
        return ""
    segments = [f"M {int(points[0, 0])} {int(points[0, 1])}"]
    for point in points[1:]:
        segments.append(f"L {int(point[0])} {int(point[1])}")
    segments.append("Z")
    return " ".join(segments)


def layer_paths(mask: np.ndarray, epsilon: float, min_area: int) -> list[str]:
    contours, hierarchy = cv2.findContours(mask, cv2.RETR_CCOMP, cv2.CHAIN_APPROX_NONE)
    if hierarchy is None:
        return []

    paths: list[str] = []
    hierarchy = hierarchy[0]

    for index, contour in enumerate(contours):
        if hierarchy[index][3] != -1:
            continue
        area = abs(cv2.contourArea(contour))
        if area < min_area:
            continue

        approx = cv2.approxPolyDP(contour, epsilon, True)
        data = contour_to_path(approx)
        child = hierarchy[index][2]

        while child != -1:
            child_area = abs(cv2.contourArea(contours[child]))
            if child_area >= min_area:
                child_approx = cv2.approxPolyDP(contours[child], epsilon, True)
                child_path = contour_to_path(child_approx)
                if child_path:
                    data += " " + child_path
            child = hierarchy[child][0]

        if data:
            paths.append(data)

    return paths


def build_svg(
    quantized: np.ndarray,
    radius: int,
    transparent: bool,
    title: str,
) -> str:
    height, width = quantized.shape
    card_mask = rounded_rect_mask(height, width, radius)
    working = quantized.copy()
    working[card_mask == 0] = 0
    transparent_trim_x = 12 if transparent else 0
    transparent_trim_y = 12 if transparent else 0

    white_mask = ((working == 0xFB) & (card_mask > 0)).astype(np.uint8)
    background_white = border_connected(white_mask)

    svg_parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" shape-rendering="geometricPrecision">',
        f"<title>{title}</title>",
        "<defs>",
        f'<clipPath id="clip-card"><rect x="0" y="0" width="{width}" height="{height}" rx="{radius}" ry="{radius}"/></clipPath>',
        (
            f'<clipPath id="clip-transparent"><rect x="{transparent_trim_x}" y="{transparent_trim_y}" '
            f'width="{width - transparent_trim_x}" height="{height - transparent_trim_y}"/></clipPath>'
            if transparent
            else ""
        ),
        "</defs>",
    ]

    if not transparent:
        svg_parts.append(f'<rect width="{width}" height="{height}" rx="{radius}" ry="{radius}" fill="#FBFBFB"/>')

    group_clip = 'url(#clip-card)'
    if transparent:
        group_clip = 'url(#clip-transparent)'

    svg_parts.append(f'<g clip-path="{group_clip}">')

    for value, hex_color in reversed(PALETTE):
        mask = ((working == value) & (card_mask > 0)).astype(np.uint8)

        if transparent and value == 0xFB:
            mask[background_white] = 0

        if transparent and value == 0x01:
            border_black = border_connected(mask)
            mask[border_black] = 0

        component_count, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
        cleaned = np.zeros_like(mask)
        for component_index in range(1, component_count):
            area = stats[component_index, cv2.CC_STAT_AREA]
            threshold = 6 if value == 0x01 else 10
            if area >= threshold:
                cleaned[labels == component_index] = 255

        paths = layer_paths(cleaned, epsilon=0.65, min_area=6 if value == 0x01 else 10)
        if not paths:
            continue

        svg_parts.append(
            f'<path d="{" ".join(paths)}" fill="{hex_color}" fill-rule="evenodd"/>'
        )

    svg_parts.append("</g></svg>")
    return "".join(svg_parts)


def render_svg_preview(svg_path: Path, output_path: Path, background: str | None = None) -> None:
    if background is None:
        cairosvg.svg2png(url=str(svg_path), write_to=str(output_path))
        return

    svg_text = svg_path.read_text()
    insert = f'<rect width="100%" height="100%" fill="{background}"/>'
    svg_text = svg_text.replace("<g clip-path=", insert + "<g clip-path=", 1)
    cairosvg.svg2png(bytestring=svg_text.encode("utf-8"), write_to=str(output_path))


def fit_with_canvas(image_path: Path, size: tuple[int, int], background: str) -> Image.Image:
    image = Image.open(image_path).convert("RGBA")
    fitted = ImageOps.contain(image, size)
    canvas = Image.new("RGBA", size, background)
    offset = ((size[0] - fitted.width) // 2, (size[1] - fitted.height) // 2)
    canvas.paste(fitted, offset, fitted)
    return canvas


def make_comparison_board(
    input_dir: Path,
    output_dir: Path,
    generated_dir: Path,
) -> None:
    tile_size = (280, 280)
    gutter = 24
    margin = 28
    header_height = 48
    columns = ["Original PNG", "Research SVG Preview", "Transparent SVG Preview"]
    rows = REFERENCES

    width = margin * 2 + len(columns) * tile_size[0] + (len(columns) - 1) * gutter
    height = (
        margin * 2
        + header_height
        + len(rows) * tile_size[1]
        + (len(rows) - 1) * gutter
        + 40
    )

    board = Image.new("RGBA", (width, height), "#f4f0e7")
    draw = ImageDraw.Draw(board)

    for column_index, column in enumerate(columns):
        x = margin + column_index * (tile_size[0] + gutter)
        draw.text((x, margin), column, fill="#111111")

    for row_index, reference in enumerate(rows):
        base_y = margin + header_height + row_index * (tile_size[1] + gutter)
        draw.text((margin, base_y - 18), reference.key, fill="#111111")

        original = fit_with_canvas(input_dir / reference.filename, tile_size, "#ffffff")
        research = fit_with_canvas(generated_dir / "preview-research" / f"{reference.key}.png", tile_size, "#ffffff")
        transparent = fit_with_canvas(
            generated_dir / "preview-transparent" / f"{reference.key}.png",
            tile_size,
            "#ece8df",
        )

        for column_index, tile in enumerate((original, research, transparent)):
            x = margin + column_index * (tile_size[0] + gutter)
            board.alpha_composite(tile, (x, base_y))
            draw.rounded_rectangle(
                [x, base_y, x + tile_size[0], base_y + tile_size[1]],
                radius=22,
                outline="#d9d2c6",
                width=2,
            )

    output_path = output_dir / "bear-svg-comparison-board.png"
    board.save(output_path)


def process_reference(reference: ReferenceImage, input_dir: Path, generated_dir: Path) -> None:
    input_path = input_dir / reference.filename
    image = cv2.imread(str(input_path), cv2.IMREAD_GRAYSCALE)
    if image is None:
      raise FileNotFoundError(f"Missing input image: {input_path}")

    min_x, min_y, max_x, max_y = compute_visible_bounds(image)
    crop = image[min_y:max_y, min_x:max_x].copy()

    patch_watermark(crop)
    crop = cv2.medianBlur(crop, 3)
    if reference.extra_cleanup:
        kernel = np.ones((3, 3), np.uint8)
        crop = cv2.morphologyEx(crop, cv2.MORPH_OPEN, kernel)

    quantized = quantize_grayscale(crop)
    radius = max(18, min(32, round(min(crop.shape[:2]) * 0.06)))

    research_svg = build_svg(quantized, radius, transparent=False, title=f"{reference.key} research")
    transparent_svg = build_svg(quantized, radius, transparent=True, title=f"{reference.key} transparent")

    research_svg_path = generated_dir / "research-svg" / f"{reference.key}.svg"
    transparent_svg_path = generated_dir / "transparent-svg" / f"{reference.key}.svg"
    research_svg_path.write_text(research_svg, encoding="utf-8")
    transparent_svg_path.write_text(transparent_svg, encoding="utf-8")

    render_svg_preview(research_svg_path, generated_dir / "preview-research" / f"{reference.key}.png")
    render_svg_preview(
        transparent_svg_path,
        generated_dir / "preview-transparent" / f"{reference.key}.png",
        background="#f1ecdf",
    )


def ensure_dirs(paths: Iterable[Path]) -> None:
    for path in paths:
        path.mkdir(parents=True, exist_ok=True)


def main() -> None:
    args = parse_args()
    input_dir = args.input_dir.expanduser().resolve()
    output_dir = args.output_dir.expanduser().resolve()
    generated_dir = output_dir / "generated-cv"

    ensure_dirs(
        [
            generated_dir / "research-svg",
            generated_dir / "transparent-svg",
            generated_dir / "preview-research",
            generated_dir / "preview-transparent",
        ]
    )

    for reference in REFERENCES:
        process_reference(reference, input_dir, generated_dir)

    make_comparison_board(input_dir, output_dir, generated_dir)

    print(f"Generated assets in: {generated_dir}")
    print(f"Comparison board: {output_dir / 'bear-svg-comparison-board.png'}")


if __name__ == "__main__":
    main()
