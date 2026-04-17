#!/usr/bin/env python3

from __future__ import annotations

import argparse
import io
import json
import os
from concurrent.futures import ProcessPoolExecutor
from dataclasses import dataclass
from pathlib import Path

import cairosvg
import cv2
import numpy as np
from PIL import Image

from generate_bear_research_svgs import PALETTE, border_connected, layer_paths, quantize_grayscale
from generate_intro_video_svgs import (
    compute_quantized_bounds,
    compute_union_bounds,
    patch_video_watermark,
    process_frame as process_intro_frame,
    strip_border_artifacts,
)


@dataclass(frozen=True)
class VideoSpec:
    key: str
    filename: str
    fallback_fps: float = 24.0


VIDEO_SPECS = [
    VideoSpec("intro", "打招呼_3950_0.mp4", 24.0),
    VideoSpec("idle", "稳定版idle.mp4", 24.0),
    VideoSpec("idleInteract", "处于idle状态下的点击反馈.mp4", 24.0),
    VideoSpec("password", "password.mp4", 24.0),
    VideoSpec("error", "登录失败.mp4", 23.608220757077397),
    VideoSpec("success", "登录成功.mp4", 23.608220757077397),
]

CANVAS_MARGIN = 8
PROXY_TRIM_TOP_RATIO = 0.24
FRAME_WORKERS = max(4, os.cpu_count() or 4)

cv2.setUseOptimized(True)
# Let the outer frame-level executor saturate cores instead of each OpenCV call
# recursively consuming the whole CPU.
cv2.setNumThreads(1)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-dir", required=True, type=Path)
    parser.add_argument("--project-output-root", required=True, type=Path)
    parser.add_argument("--desktop-output-root", type=Path)
    parser.add_argument("--reference-svg", required=True, type=Path)
    parser.add_argument("--only", nargs="*", default=[])
    return parser.parse_args()


def ensure_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def clear_old_frames(output_dir: Path) -> None:
    if not output_dir.exists():
        return

    for path in output_dir.glob("frame-*.svg"):
        path.unlink()


def render_svg_mask(svg_path: Path) -> tuple[np.ndarray, int, int]:
    png_bytes = cairosvg.svg2png(url=str(svg_path))
    image = Image.open(io.BytesIO(png_bytes)).convert("RGBA")
    rgba = np.array(image)
    alpha_mask = (rgba[:, :, 3] > 0).astype(np.uint8)
    return alpha_mask, image.width, image.height


def compute_mask_bounds(mask: np.ndarray) -> tuple[int, int, int, int]:
    coords = np.column_stack(np.where(mask > 0))
    if coords.size == 0:
        raise RuntimeError("No visible mask bounds found")

    min_y, min_x = coords.min(axis=0)
    max_y, max_x = coords.max(axis=0)
    return int(min_x), int(min_y), int(max_x) + 1, int(max_y) + 1


def compute_proxy_bounds(mask: np.ndarray, trim_top_ratio: float = PROXY_TRIM_TOP_RATIO) -> tuple[int, int, int, int]:
    min_x, min_y, max_x, max_y = compute_mask_bounds(mask)
    height = max_y - min_y
    trim_top = min_y + int(height * trim_top_ratio)
    trimmed = mask.copy()
    trimmed[:trim_top, :] = 0

    try:
        return compute_mask_bounds(trimmed)
    except RuntimeError:
        return min_x, min_y, max_x, max_y


def read_gray_frames(video_path: Path) -> tuple[list[np.ndarray], float]:
    capture = cv2.VideoCapture(str(video_path))
    if not capture.isOpened():
        raise RuntimeError(f"Could not open video: {video_path}")

    fps = float(capture.get(cv2.CAP_PROP_FPS) or 0.0)
    frames: list[np.ndarray] = []

    while True:
        ok, frame = capture.read()
        if not ok:
            break
        frames.append(cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY))

    capture.release()
    return frames, fps


def process_frame_for_mode(
    gray: np.ndarray,
    bounds: tuple[int, int, int, int],
    mode: str,
) -> np.ndarray:
    if mode == "intro":
        return process_intro_frame(gray, bounds)

    min_x, min_y, max_x, max_y = bounds
    crop = gray[min_y:max_y, min_x:max_x].copy()
    patch_video_watermark(crop)
    crop = cv2.medianBlur(crop, 3)
    quantized = quantize_grayscale(crop)

    # Non-intro clips are centered card captures. Reusing intro's edge-band
    # cleanup deletes valid bear outlines whenever arms/ears drift near the top
    # band, so keep only the border-connected cleanup here.
    quantized = strip_border_artifacts(quantized)
    quantized = keep_primary_subject(quantized)
    if mode == "password":
        outline_quantized = quantized.copy()
        quantized = fill_subject_interior(quantized)
        quantized = trim_password_light_fringe(outline_quantized, quantized)
        quantized = reinforce_password_outer_contour(quantized)
    return quantized


def keep_primary_subject(quantized: np.ndarray) -> np.ndarray:
    foreground = (quantized > 0).astype(np.uint8)
    if not foreground.any():
        return quantized

    component_count, labels, stats, _ = cv2.connectedComponentsWithStats(foreground, 8)
    if component_count <= 1:
        return quantized

    best_component = max(
        range(1, component_count),
        key=lambda component_index: int(stats[component_index, cv2.CC_STAT_AREA]),
    )

    cleaned = np.zeros_like(quantized)
    cleaned[labels == best_component] = quantized[labels == best_component]
    return cleaned


def fill_subject_interior(quantized: np.ndarray) -> np.ndarray:
    """Recover white fur areas that match the card background too closely.

    Some password clips have a very light background and a very light torso.
    Border cleanup correctly removes the card, but it can also punch holes in
    the bear's body. Use the remaining outline/fur strokes to infer the closed
    subject silhouette, then fill only interior holes with the lightest palette
    value. Border-connected gaps, such as the space around the feet, stay
    transparent.
    """
    subject = (quantized > 0).astype(np.uint8)
    if not subject.any():
        return quantized

    component_count, labels, stats, _ = cv2.connectedComponentsWithStats(subject, 8)
    if component_count <= 1:
        return quantized

    largest_component = max(
        range(1, component_count),
        key=lambda component_index: int(stats[component_index, cv2.CC_STAT_AREA]),
    )
    subject = (labels == largest_component).astype(np.uint8) * 255

    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (71, 71))
    closed = cv2.morphologyEx(subject, cv2.MORPH_CLOSE, kernel, iterations=1)

    flood = closed.copy()
    height, width = flood.shape[:2]
    mask = np.zeros((height + 2, width + 2), np.uint8)
    cv2.floodFill(flood, mask, (0, 0), 255)
    holes = (flood == 0).astype(np.uint8) * 255

    filled = np.maximum(closed, holes)
    recovered = quantized.copy()
    recovered[(filled > 0) & (recovered == 0)] = 0xFB
    return recovered


def trim_password_light_fringe(outline_quantized: np.ndarray, filled_quantized: np.ndarray) -> np.ndarray:
    """Remove light card/background remnants outside the inferred bear silhouette.

    The password clip has white fur on a near-white card. `fill_subject_interior`
    deliberately restores missing torso/arm whites, but that can also leave a
    thin light halo outside the black outline. Build the silhouette from stable
    dark/gray strokes before the fill, then trim only light pixels outside it.
    """
    anchor = ((outline_quantized > 0) & (outline_quantized <= 0xD8)).astype(np.uint8) * 255
    if not anchor.any():
        return filled_quantized

    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (71, 71))
    closed = cv2.morphologyEx(anchor, cv2.MORPH_CLOSE, kernel, iterations=1)

    component_count, labels, stats, _ = cv2.connectedComponentsWithStats((closed > 0).astype(np.uint8), 8)
    if component_count > 1:
        largest_component = max(
            range(1, component_count),
            key=lambda component_index: int(stats[component_index, cv2.CC_STAT_AREA]),
        )
        closed = (labels == largest_component).astype(np.uint8) * 255

    flood = closed.copy()
    height, width = flood.shape[:2]
    mask = np.zeros((height + 2, width + 2), np.uint8)
    cv2.floodFill(flood, mask, (0, 0), 255)
    holes = (flood == 0).astype(np.uint8) * 255
    allowed = np.maximum(closed, holes)
    # Keep this conservative. Larger erosion values hide light halos, but they
    # also eat into feet/fur contours and make the rebuilt black outline look
    # broken around the paws.
    erosion_kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (13, 13))
    allowed = cv2.erode(allowed, erosion_kernel, iterations=1)

    trimmed = filled_quantized.copy()
    trimmed[(trimmed >= 0xE8) & (allowed == 0)] = 0
    return trimmed


def reinforce_password_outer_contour(quantized: np.ndarray) -> np.ndarray:
    """Repair thin broken exterior paw lines without globally thickening fur.

    The password source has a near-white body on a near-white card. A global
    morphology gradient fixes gaps but makes the whole bear look over-inked.
    Draw only the external subject contour back into the black layer, using the
    already-clean transparent subject mask as the source of truth.
    """
    subject = (quantized > 0).astype(np.uint8)
    if not subject.any():
        return quantized

    component_count, labels, stats, _ = cv2.connectedComponentsWithStats(subject, 8)
    if component_count <= 1:
        return quantized

    largest_component = max(
        range(1, component_count),
        key=lambda component_index: int(stats[component_index, cv2.CC_STAT_AREA]),
    )
    subject = (labels == largest_component).astype(np.uint8) * 255
    contours, _ = cv2.findContours(subject, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
    if not contours:
        return quantized

    outline = np.zeros_like(subject)
    cv2.drawContours(outline, contours, -1, 255, thickness=1, lineType=cv2.LINE_8)

    repaired = quantized.copy()
    repaired[outline > 0] = 0x01
    repaired = bridge_password_sole_gaps(repaired)
    return repaired


def bridge_password_sole_gaps(quantized: np.ndarray) -> np.ndarray:
    """Connect tiny broken sole-outline gaps without thickening the bear.

    Vectorizing the password clip can leave 1-2 short breaks on the foot sole.
    Limit the repair to the bottom band of the subject and bridge only short
    horizontal gaps that sit inside the foot mask, so arm/body fur is untouched.
    """
    subject = quantized > 0
    if not subject.any():
        return quantized

    ys, xs = np.where(subject)
    min_x, max_x = int(xs.min()), int(xs.max())
    min_y, max_y = int(ys.min()), int(ys.max()) + 1
    width = max_x - min_x + 1
    height = max_y - min_y
    start_y = min_y + int(height * 0.90)
    max_gap = max(4, min(18, int(width * 0.018)))

    repaired = quantized.copy()
    black = repaired == 0x01

    for y in range(start_y, max_y):
        row_xs = np.where(black[y, min_x : max_x + 1])[0] + min_x
        if row_xs.size < 2:
            continue

        segments: list[tuple[int, int]] = []
        start = prev = int(row_xs[0])
        for x in row_xs[1:]:
            current = int(x)
            if current == prev + 1:
                prev = current
            else:
                segments.append((start, prev))
                start = prev = current
        segments.append((start, prev))

        for (_, left_end), (right_start, _) in zip(segments, segments[1:]):
            gap = right_start - left_end - 1
            if gap <= 0 or gap > max_gap:
                continue

            gap_slice = slice(left_end + 1, right_start)
            nearby_subject = subject[max(min_y, y - 2) : min(max_y, y + 3), gap_slice]
            if nearby_subject.size and nearby_subject.mean() >= 0.25:
                repaired[y, gap_slice] = 0x01

    return repaired


def process_frame_task(args: tuple[np.ndarray, tuple[int, int, int, int], str]) -> np.ndarray:
    gray, bounds, mode = args
    return process_frame_for_mode(gray, bounds, mode)


def extract_sequence(
    video_path: Path,
    bounds: tuple[int, int, int, int],
    mode: str,
) -> tuple[list[np.ndarray], tuple[int, int, int, int], tuple[int, int, int, int], float]:
    gray_frames, fps = read_gray_frames(video_path)
    if not gray_frames:
        raise RuntimeError(f"No frames found in {video_path}")

    with ProcessPoolExecutor(max_workers=FRAME_WORKERS) as executor:
        processed_frames = list(
            executor.map(
                process_frame_task,
                ((gray, bounds, mode) for gray in gray_frames),
                chunksize=2,
            )
        )

    union_mask = None
    for quantized in processed_frames:
        frame_mask = (quantized > 0).astype(np.uint8)
        union_mask = frame_mask if union_mask is None else np.maximum(union_mask, frame_mask)

    if union_mask is None or not union_mask.any():
        raise RuntimeError(f"No foreground bear pixels found in {video_path}")

    full_bounds = compute_mask_bounds(union_mask)
    proxy_bounds = compute_proxy_bounds(union_mask)
    return processed_frames, full_bounds, proxy_bounds, fps


def fit_scale_for_canvas(
    scale: float,
    crop_width: int,
    crop_height: int,
    canvas_width: int,
    canvas_height: int,
    target_center_x: float,
    target_bottom: float,
) -> float:
    width_half = crop_width / 2.0
    max_scale_left = (target_center_x - CANVAS_MARGIN) / max(width_half, 1.0)
    max_scale_right = (canvas_width - CANVAS_MARGIN - target_center_x) / max(width_half, 1.0)
    max_scale_top = (target_bottom - CANVAS_MARGIN) / max(crop_height, 1.0)
    max_scale_bottom = (canvas_height - CANVAS_MARGIN) / max(crop_height, 1.0)
    return min(scale, max_scale_left, max_scale_right, max_scale_top, max_scale_bottom)


def build_normalized_svg(
    quantized: np.ndarray,
    title: str,
    canvas_width: int,
    canvas_height: int,
    scale: float,
    translate_x: float,
    translate_y: float,
    strip_border_black: bool = True,
    reinforce_outline: bool = False,
    outline_kernel_size: int = 3,
) -> str:
    svg_parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {canvas_width} {canvas_height}" shape-rendering="geometricPrecision">',
        f"<title>{title}</title>",
        f'<g transform="translate({translate_x:.3f} {translate_y:.3f})">',
        f'<g transform="scale({scale:.6f})">',
    ]

    outline_mask = None
    if reinforce_outline:
        foreground = (quantized > 0).astype(np.uint8)
        component_count, labels, stats, _ = cv2.connectedComponentsWithStats(foreground, 8)
        cleaned_foreground = np.zeros_like(foreground)
        for component_index in range(1, component_count):
            area = int(stats[component_index, cv2.CC_STAT_AREA])
            if area >= 24:
                cleaned_foreground[labels == component_index] = 255

        if cleaned_foreground.any():
            kernel_size = max(3, outline_kernel_size)
            kernel = np.ones((kernel_size, kernel_size), np.uint8)
            closed_foreground = cv2.morphologyEx(cleaned_foreground, cv2.MORPH_CLOSE, kernel)
            outline_mask = cv2.morphologyEx(closed_foreground, cv2.MORPH_GRADIENT, kernel)

    for value, hex_color in reversed(PALETTE):
        mask = (quantized == value).astype(np.uint8)
        if not mask.any() and not (value == 0x01 and outline_mask is not None and outline_mask.any()):
            continue

        if value == 0x01 and strip_border_black:
            border_black = border_connected(mask)
            mask[border_black] = 0

        if value == 0x01 and outline_mask is not None:
            mask = np.maximum(mask, (outline_mask > 0).astype(np.uint8))

        component_count, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
        cleaned = np.zeros_like(mask)
        for component_index in range(1, component_count):
            area = int(stats[component_index, cv2.CC_STAT_AREA])
            threshold = 4 if value == 0x01 else 8
            if area >= threshold:
                cleaned[labels == component_index] = 255

        paths = layer_paths(
            cleaned,
            epsilon=0.42 if value != 0x01 else 0.32,
            min_area=4 if value == 0x01 else 8,
        )
        if not paths:
            continue

        svg_parts.append(f'<path d="{" ".join(paths)}" fill="{hex_color}" fill-rule="evenodd"/>')

    svg_parts.append("</g></g></svg>")
    return "".join(svg_parts)


def export_sequence(
    processed_frames: list[np.ndarray],
    full_bounds: tuple[int, int, int, int],
    proxy_bounds: tuple[int, int, int, int],
    output_dir: Path,
    canvas_width: int,
    canvas_height: int,
    target_center_x: float,
    target_bottom: float,
    target_proxy_height: float,
    title_prefix: str,
    stabilize_per_frame: bool = False,
    strip_border_black: bool = True,
    reinforce_outline: bool = False,
    outline_kernel_size: int = 3,
) -> dict[str, float | int]:
    ensure_dir(output_dir)
    clear_old_frames(output_dir)

    full_min_x, full_min_y, full_max_x, full_max_y = full_bounds
    proxy_min_x, proxy_min_y, proxy_max_x, proxy_max_y = proxy_bounds
    crop_width = full_max_x - full_min_x
    crop_height = full_max_y - full_min_y
    proxy_height = max(proxy_max_y - proxy_min_y, 1)

    scale = target_proxy_height / proxy_height
    scale = fit_scale_for_canvas(
        scale,
        crop_width,
        crop_height,
        canvas_width,
        canvas_height,
        target_center_x,
        target_bottom,
    )

    translate_x = target_center_x - scale * (crop_width / 2.0)
    translate_y = target_bottom - scale * crop_height

    for frame_index, quantized in enumerate(processed_frames):
        current_full_bounds = full_bounds
        current_proxy_bounds = proxy_bounds

        if stabilize_per_frame:
            frame_mask = (quantized > 0).astype(np.uint8)
            if frame_mask.any():
                current_full_bounds = compute_mask_bounds(frame_mask)
                current_proxy_bounds = compute_proxy_bounds(frame_mask)

        current_full_min_x, current_full_min_y, current_full_max_x, current_full_max_y = current_full_bounds
        current_proxy_min_x, current_proxy_min_y, current_proxy_max_x, current_proxy_max_y = current_proxy_bounds
        current_crop_width = current_full_max_x - current_full_min_x
        current_crop_height = current_full_max_y - current_full_min_y
        current_proxy_height = max(current_proxy_max_y - current_proxy_min_y, 1)

        current_scale = scale
        current_translate_x = translate_x
        current_translate_y = translate_y

        if stabilize_per_frame:
            current_scale = target_proxy_height / current_proxy_height
            current_scale = fit_scale_for_canvas(
                current_scale,
                current_crop_width,
                current_crop_height,
                canvas_width,
                canvas_height,
                target_center_x,
                target_bottom,
            )
            current_translate_x = target_center_x - current_scale * (current_crop_width / 2.0)
            current_translate_y = target_bottom - current_scale * current_crop_height

        cropped = quantized[current_full_min_y:current_full_max_y, current_full_min_x:current_full_max_x]
        svg = build_normalized_svg(
            cropped,
            title=f"{title_prefix} frame {frame_index:03d}",
            canvas_width=canvas_width,
            canvas_height=canvas_height,
            scale=current_scale,
            translate_x=current_translate_x,
            translate_y=current_translate_y,
            strip_border_black=strip_border_black,
            reinforce_outline=reinforce_outline,
            outline_kernel_size=outline_kernel_size,
        )
        (output_dir / f"frame-{frame_index:03d}.svg").write_text(svg, encoding="utf-8")

    return {
        "frameCount": len(processed_frames),
        "scale": round(scale, 6),
        "offsetX": round(translate_x, 3),
        "offsetY": round(translate_y, 3),
    }


def write_manifest(output_path: Path, data: dict) -> None:
    ensure_dir(output_path.parent)
    output_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


def load_manifest(manifest_path: Path, canvas_width: int, canvas_height: int, reference_svg: Path, target_center_x: float, target_bottom: float, target_proxy_height: float) -> dict:
    if manifest_path.exists():
        return json.loads(manifest_path.read_text(encoding="utf-8"))

    return {
        "canvas": {
            "width": canvas_width,
            "height": canvas_height,
        },
        "reference": {
            "svg": str(reference_svg),
            "centerX": round(target_center_x, 3),
            "bottom": round(target_bottom, 3),
            "proxyHeight": round(target_proxy_height, 3),
        },
        "palette": [value for value, _ in PALETTE],
        "sequences": {},
    }


def parse_selected_specs(only_args: list[str]) -> list[VideoSpec]:
    if not only_args:
        return VIDEO_SPECS

    requested = {
        item.strip()
        for chunk in only_args
        for item in chunk.split(",")
        if item.strip()
    }
    selected = [spec for spec in VIDEO_SPECS if spec.key in requested]
    if not selected:
        raise RuntimeError(f"No matching videos for --only {sorted(requested)}")
    return selected


def main() -> None:
    args = parse_args()
    input_dir = args.input_dir.expanduser().resolve()
    project_output_root = args.project_output_root.expanduser().resolve()
    desktop_output_root = args.desktop_output_root.expanduser().resolve() if args.desktop_output_root else None
    reference_svg = args.reference_svg.expanduser().resolve()

    selected_specs = parse_selected_specs(args.only)
    print(f"Processing videos: {[spec.key for spec in selected_specs]}")
    print(f"Frame workers: {FRAME_WORKERS}")

    reference_mask, canvas_width, canvas_height = render_svg_mask(reference_svg)
    target_full_bounds = compute_mask_bounds(reference_mask)
    target_proxy_bounds = compute_proxy_bounds(reference_mask)
    target_center_x = (target_full_bounds[0] + target_full_bounds[2]) / 2.0
    target_bottom = float(target_full_bounds[3])
    target_proxy_height = float(target_proxy_bounds[3] - target_proxy_bounds[1])

    project_manifest_path = project_output_root / "manifest.json"
    project_manifest = load_manifest(
        project_manifest_path,
        canvas_width,
        canvas_height,
        reference_svg,
        target_center_x,
        target_bottom,
        target_proxy_height,
    )
    desktop_manifest = project_manifest.copy()

    for spec in selected_specs:
        video_path = input_dir / spec.filename
        if not video_path.exists():
            raise FileNotFoundError(f"Missing video: {video_path}")

        bounds = compute_union_bounds(video_path)
        print(f"{spec.key} raw bounds: {bounds}")

        processed_frames, full_bounds, proxy_bounds, fps = extract_sequence(video_path, bounds, spec.key)

        export_meta = export_sequence(
            processed_frames=processed_frames,
            full_bounds=full_bounds,
            proxy_bounds=proxy_bounds,
            output_dir=project_output_root / spec.key,
            canvas_width=canvas_width,
            canvas_height=canvas_height,
            target_center_x=target_center_x,
            target_bottom=target_bottom,
            target_proxy_height=target_proxy_height,
            title_prefix=spec.key,
            stabilize_per_frame=spec.key == "idle",
            strip_border_black=spec.key == "intro",
            reinforce_outline=spec.key in {"idleInteract", "success"},
            outline_kernel_size=3,
        )

        if desktop_output_root:
            export_sequence(
                processed_frames=processed_frames,
                full_bounds=full_bounds,
                proxy_bounds=proxy_bounds,
                output_dir=desktop_output_root / spec.key,
                canvas_width=canvas_width,
                canvas_height=canvas_height,
                target_center_x=target_center_x,
                target_bottom=target_bottom,
                target_proxy_height=target_proxy_height,
                title_prefix=spec.key,
                stabilize_per_frame=spec.key == "idle",
                strip_border_black=spec.key == "intro",
                reinforce_outline=spec.key in {"idleInteract", "success"},
                outline_kernel_size=3,
            )

        sequence_meta = {
            "video": spec.filename,
            "fps": round(fps if fps > 0 else spec.fallback_fps, 6),
            **export_meta,
        }
        project_manifest["sequences"][spec.key] = sequence_meta
        if desktop_output_root:
            desktop_manifest["sequences"][spec.key] = sequence_meta

        write_manifest(project_manifest_path, project_manifest)
        if desktop_output_root:
            write_manifest(desktop_output_root / "manifest.json", desktop_manifest)

        print(f"{spec.key}: generated {export_meta['frameCount']} frames")

    print(f"Project output: {project_output_root}")
    if desktop_output_root:
        print(f"Desktop output: {desktop_output_root}")


if __name__ == "__main__":
    main()
