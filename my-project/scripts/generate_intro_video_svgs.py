#!/usr/bin/env python3

from __future__ import annotations

import argparse
from pathlib import Path

import cv2
import numpy as np

from generate_bear_research_svgs import PALETTE, border_connected, compute_visible_bounds, layer_paths, quantize_grayscale


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--video", required=True, type=Path)
    parser.add_argument("--project-output", required=True, type=Path)
    parser.add_argument("--desktop-output", type=Path)
    return parser.parse_args()


def ensure_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def patch_video_watermark(crop: cv2.typing.MatLike) -> None:
    height, width = crop.shape[:2]
    patch_height = min(max(42, height // 10), 72)
    patch_width = min(max(140, width // 4), 220)
    crop[height - patch_height : height, width - patch_width : width] = 0xFB


def strip_border_artifacts(quantized: cv2.typing.MatLike) -> cv2.typing.MatLike:
    cleaned = quantized.copy()
    for value, _hex_color in PALETTE:
        mask = (cleaned == value).astype("uint8")
        if not mask.any():
            continue
        connected = border_connected(mask)
        cleaned[connected & (cleaned == value)] = 0
    return cleaned


def strip_edge_band_artifacts(quantized: cv2.typing.MatLike, edge_margin: int = 64) -> cv2.typing.MatLike:
    cleaned = quantized.copy()
    height, width = cleaned.shape[:2]

    for value, _hex_color in PALETTE:
        mask = (cleaned == value).astype("uint8")
        if not mask.any():
            continue

        component_count, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
        for component_index in range(1, component_count):
            x, y, component_width, component_height, _area = map(int, stats[component_index, :5])
            touches_edge_band = (
                x <= edge_margin
                or y <= edge_margin
                or x + component_width >= width - edge_margin
                or y + component_height >= height - edge_margin
            )
            if touches_edge_band:
                cleaned[labels == component_index] = 0

    return cleaned


def compute_quantized_bounds(quantized: cv2.typing.MatLike) -> tuple[int, int, int, int]:
    mask = quantized > 0
    coords = np.column_stack(np.where(mask))
    min_y, min_x = coords.min(axis=0)
    max_y, max_x = coords.max(axis=0)
    return int(min_x), int(min_y), int(max_x) + 1, int(max_y) + 1


def build_transparent_svg(quantized: cv2.typing.MatLike, title: str) -> str:
    height, width = quantized.shape
    svg_parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" shape-rendering="geometricPrecision">',
        f"<title>{title}</title>",
    ]

    for value, hex_color in reversed(PALETTE):
        mask = (quantized == value).astype(np.uint8)
        if not mask.any():
            continue

        component_count, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
        cleaned = np.zeros_like(mask)
        for component_index in range(1, component_count):
            area = int(stats[component_index, cv2.CC_STAT_AREA])
            threshold = 6 if value == 0x01 else 10
            if area >= threshold:
                cleaned[labels == component_index] = 255

        paths = layer_paths(cleaned, epsilon=0.65 if value != 0x01 else 0.55, min_area=6 if value == 0x01 else 10)
        if not paths:
            continue

        svg_parts.append(
            f'<path d="{" ".join(paths)}" fill="{hex_color}" fill-rule="evenodd"/>'
        )

    svg_parts.append("</svg>")
    return "".join(svg_parts)


def process_frame(gray: cv2.typing.MatLike, bounds: tuple[int, int, int, int]) -> cv2.typing.MatLike:
    min_x, min_y, max_x, max_y = bounds
    crop = gray[min_y:max_y, min_x:max_x].copy()
    patch_video_watermark(crop)
    crop = cv2.medianBlur(crop, 3)
    quantized = quantize_grayscale(crop)
    quantized = strip_border_artifacts(quantized)
    quantized = strip_edge_band_artifacts(quantized)
    return quantized


def compute_union_bounds(video_path: Path) -> tuple[int, int, int, int]:
    capture = cv2.VideoCapture(str(video_path))
    if not capture.isOpened():
        raise RuntimeError(f"Could not open video: {video_path}")

    min_x = None
    min_y = None
    max_x = None
    max_y = None

    while True:
        ok, frame = capture.read()
        if not ok:
            break

        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        frame_min_x, frame_min_y, frame_max_x, frame_max_y = compute_visible_bounds(gray)

        min_x = frame_min_x if min_x is None else min(min_x, frame_min_x)
        min_y = frame_min_y if min_y is None else min(min_y, frame_min_y)
        max_x = frame_max_x if max_x is None else max(max_x, frame_max_x)
        max_y = frame_max_y if max_y is None else max(max_y, frame_max_y)

    capture.release()

    if None in {min_x, min_y, max_x, max_y}:
        raise RuntimeError(f"No visible frames found in: {video_path}")

    return int(min_x), int(min_y), int(max_x), int(max_y)


def write_svg_frames(
    processed_frames: list[cv2.typing.MatLike],
    object_bounds: tuple[int, int, int, int],
    output_dir: Path,
) -> int:
    ensure_dir(output_dir)
    min_x, min_y, max_x, max_y = object_bounds

    for frame_index, quantized in enumerate(processed_frames):
        cropped = quantized[min_y:max_y, min_x:max_x]
        svg = build_transparent_svg(cropped, title=f"intro frame {frame_index:03d}")
        (output_dir / f"frame-{frame_index:03d}.svg").write_text(svg, encoding="utf-8")

    return len(processed_frames)


def clear_old_frames(output_dir: Path) -> None:
    if not output_dir.exists():
        return
    for path in output_dir.glob("frame-*.svg"):
        path.unlink()


def main() -> None:
    args = parse_args()
    video_path = args.video.expanduser().resolve()
    project_output = args.project_output.expanduser().resolve()
    desktop_output = args.desktop_output.expanduser().resolve() if args.desktop_output else None

    bounds = compute_union_bounds(video_path)
    capture = cv2.VideoCapture(str(video_path))
    if not capture.isOpened():
        raise RuntimeError(f"Could not open video: {video_path}")

    processed_frames: list[cv2.typing.MatLike] = []
    object_min_x = None
    object_min_y = None
    object_max_x = None
    object_max_y = None

    while True:
        ok, frame = capture.read()
        if not ok:
            break

        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        quantized = process_frame(gray, bounds)
        processed_frames.append(quantized)

        frame_min_x, frame_min_y, frame_max_x, frame_max_y = compute_quantized_bounds(quantized)
        object_min_x = frame_min_x if object_min_x is None else min(object_min_x, frame_min_x)
        object_min_y = frame_min_y if object_min_y is None else min(object_min_y, frame_min_y)
        object_max_x = frame_max_x if object_max_x is None else max(object_max_x, frame_max_x)
        object_max_y = frame_max_y if object_max_y is None else max(object_max_y, frame_max_y)

    capture.release()

    if None in {object_min_x, object_min_y, object_max_x, object_max_y}:
        raise RuntimeError("No foreground bear pixels found in video")

    object_bounds = (int(object_min_x), int(object_min_y), int(object_max_x), int(object_max_y))

    clear_old_frames(project_output)
    count = write_svg_frames(processed_frames, object_bounds, project_output)

    if desktop_output:
        clear_old_frames(desktop_output)
        write_svg_frames(processed_frames, object_bounds, desktop_output)

    print(f"Bounds: {bounds}")
    print(f"Object bounds: {object_bounds}")
    print(f"Generated {count} intro SVG frames in: {project_output}")
    if desktop_output:
        print(f"Desktop copy: {desktop_output}")


if __name__ == "__main__":
    main()
