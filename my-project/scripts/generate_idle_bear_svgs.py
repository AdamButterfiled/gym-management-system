#!/usr/bin/env python3

from __future__ import annotations

import argparse
from pathlib import Path

import cv2
import numpy as np

from generate_bear_research_svgs import (
    border_connected,
    build_svg,
    layer_paths,
    quantize_grayscale,
    render_svg_preview,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--open", required=True, type=Path)
    parser.add_argument("--closed", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--preview-dir", type=Path)
    return parser.parse_args()


def load_grayscale(path: Path) -> np.ndarray:
    image = cv2.imread(str(path), cv2.IMREAD_UNCHANGED)
    if image is None:
        raise FileNotFoundError(f"Missing input image: {path}")

    if image.ndim == 2:
        return image

    if image.shape[2] == 4:
        bgr = image[:, :, :3].astype(np.float32)
        alpha = image[:, :, 3:4].astype(np.float32) / 255.0
        composite = bgr * alpha + 255.0 * (1.0 - alpha)
        return cv2.cvtColor(composite.astype(np.uint8), cv2.COLOR_BGR2GRAY)

    return cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)


def align_closed_to_open(open_image: np.ndarray, closed_image: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    canvas_height = max(open_image.shape[0], closed_image.shape[0])
    canvas_width = max(open_image.shape[1], closed_image.shape[1])

    open_canvas = np.full((canvas_height, canvas_width), 255, dtype=np.uint8)
    closed_canvas = np.full((canvas_height, canvas_width), 255, dtype=np.uint8)
    open_canvas[: open_image.shape[0], : open_image.shape[1]] = open_image
    closed_canvas[: closed_image.shape[0], : closed_image.shape[1]] = closed_image

    warp = np.eye(2, 3, dtype=np.float32)
    criteria = (cv2.TERM_CRITERIA_EPS | cv2.TERM_CRITERIA_COUNT, 200, 1e-6)

    try:
        _, warp = cv2.findTransformECC(open_canvas, closed_canvas, warp, cv2.MOTION_AFFINE, criteria)
        aligned_closed = cv2.warpAffine(
            closed_canvas,
            warp,
            (canvas_width, canvas_height),
            flags=cv2.INTER_LINEAR | cv2.WARP_INVERSE_MAP,
            borderMode=cv2.BORDER_CONSTANT,
            borderValue=255,
        )
    except cv2.error:
        aligned_closed = cv2.resize(
            closed_canvas,
            (canvas_width, canvas_height),
            interpolation=cv2.INTER_LINEAR,
        )

    return open_canvas, aligned_closed


def to_svg(image: np.ndarray, title: str) -> str:
    blurred = cv2.medianBlur(image, 3)
    quantized = quantize_grayscale(blurred)
    background_mask = (quantized >= 0xD8).astype(np.uint8)
    quantized[border_connected(background_mask)] = 0xFB
    radius = max(18, min(32, round(min(image.shape[:2]) * 0.06)))
    return build_svg(quantized, radius=radius, transparent=True, title=title)


def extract_eye_overlay_masks(open_image: np.ndarray, closed_image: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    height, width = open_image.shape

    light_diff = ((closed_image.astype(np.int16) - open_image.astype(np.int16)) > 18).astype(np.uint8)
    light_diff[:100, :] = 0
    light_diff[190:, :] = 0
    light_diff[:, :190] = 0
    light_diff[:, 360:] = 0

    white_mask = np.zeros((height, width), dtype=np.uint8)
    component_count, labels, stats, _ = cv2.connectedComponentsWithStats(light_diff, 8)
    light_components: list[tuple[int, int]] = []

    for component_index in range(1, component_count):
        area = int(stats[component_index, cv2.CC_STAT_AREA])
        if 40 <= area <= 400:
            light_components.append((area, component_index))

    light_components.sort(reverse=True)

    light_kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (7, 7))
    light_close_kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))

    for _, component_index in light_components[:2]:
        component_mask = (labels == component_index).astype(np.uint8) * 255
        component_mask = cv2.dilate(component_mask, light_kernel, iterations=1)
        component_mask = cv2.morphologyEx(component_mask, cv2.MORPH_CLOSE, light_close_kernel)
        white_mask = np.maximum(white_mask, component_mask)

    dark_regions = (closed_image < 90).astype(np.uint8)
    dark_regions[:110, :] = 0
    dark_regions[190:, :] = 0
    dark_regions[:, :210] = 0
    dark_regions[:, 350:] = 0

    black_mask = np.zeros((height, width), dtype=np.uint8)
    component_count, labels, stats, _ = cv2.connectedComponentsWithStats(dark_regions, 8)
    dark_components: list[tuple[int, int]] = []

    for component_index in range(1, component_count):
        area = int(stats[component_index, cv2.CC_STAT_AREA])
        component_width = int(stats[component_index, cv2.CC_STAT_WIDTH])
        component_height = int(stats[component_index, cv2.CC_STAT_HEIGHT])
        if 60 <= area <= 160 and component_width <= 30 and component_height <= 18:
            dark_components.append((area, component_index))

    dark_components.sort(reverse=True)

    for _, component_index in dark_components[:2]:
        component_mask = (labels == component_index).astype(np.uint8) * 255
        component_mask = cv2.dilate(component_mask, np.ones((2, 2), np.uint8), iterations=1)
        black_mask = np.maximum(black_mask, component_mask)

    return white_mask, black_mask


def build_eye_overlay_svg(open_image: np.ndarray, closed_image: np.ndarray) -> str:
    height, width = open_image.shape
    white_mask, black_mask = extract_eye_overlay_masks(open_image, closed_image)

    white_paths = layer_paths(white_mask, epsilon=0.8, min_area=20)
    black_paths = layer_paths(black_mask, epsilon=0.4, min_area=8)

    svg_parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" shape-rendering="geometricPrecision">',
        "<title>idle eye blink overlay</title>",
    ]

    if white_paths:
        svg_parts.append(f'<path d="{" ".join(white_paths)}" fill="#FBFBFB" fill-rule="evenodd"/>')

    if black_paths:
        svg_parts.append(f'<path d="{" ".join(black_paths)}" fill="#010101" fill-rule="evenodd"/>')

    svg_parts.append("</svg>")
    return "".join(svg_parts)


def write_svg(output_dir: Path, preview_dir: Path | None, name: str, svg_text: str) -> None:
    svg_path = output_dir / f"{name}.svg"
    svg_path.write_text(svg_text, encoding="utf-8")

    if preview_dir is not None:
        preview_dir.mkdir(parents=True, exist_ok=True)
        render_svg_preview(svg_path, preview_dir / f"{name}.png", background="#f1ecdf")


def main() -> None:
    args = parse_args()
    output_dir = args.output_dir.expanduser().resolve()
    preview_dir = args.preview_dir.expanduser().resolve() if args.preview_dir else None
    output_dir.mkdir(parents=True, exist_ok=True)

    open_image = load_grayscale(args.open.expanduser().resolve())
    closed_image = load_grayscale(args.closed.expanduser().resolve())
    aligned_open, aligned_closed = align_closed_to_open(open_image, closed_image)

    write_svg(output_dir, preview_dir, "idle-open", to_svg(aligned_open, "idle-open transparent"))
    write_svg(output_dir, preview_dir, "idle-closed", to_svg(aligned_closed, "idle-closed transparent"))
    write_svg(
        output_dir,
        preview_dir,
        "idle-eye-blink-overlay",
        build_eye_overlay_svg(aligned_open, aligned_closed),
    )

    print(f"Generated idle assets in: {output_dir}")


if __name__ == "__main__":
    main()
