#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const { PNG } = require("pngjs");
const ImageTracer = require("imagetracerjs");

const PALETTE_VALUES = [0x01, 0xb1, 0xd8, 0xe8, 0xfb];
const PALETTE_RGBA = [
  { r: 0x01, g: 0x01, b: 0x01, a: 255 },
  { r: 0xb1, g: 0xb1, b: 0xb1, a: 255 },
  { r: 0xd8, g: 0xd8, b: 0xd8, a: 255 },
  { r: 0xe8, g: 0xe8, b: 0xe8, a: 255 },
  { r: 0xfb, g: 0xfb, b: 0xfb, a: 255 },
];

const REFERENCE_IMAGES = [
  { name: "cover-peek", filename: "iCopy_2026_04_10_06_53_01.png", extraCleanup: false },
  { name: "success-lift", filename: "iCopy_2026_04_10_06_53_07.png", extraCleanup: false },
  { name: "error-sad", filename: "iCopy_2026_04_10_06_53_12.png", extraCleanup: true },
  { name: "hello-wave", filename: "iCopy_2026_04_10_06_53_16.png", extraCleanup: false },
];

function resolveArg(flag, fallback) {
  const index = process.argv.indexOf(flag);
  if (index === -1 || index === process.argv.length - 1) {
    return fallback;
  }
  return process.argv[index + 1];
}

function expandHome(inputPath) {
  if (!inputPath.startsWith("~/")) {
    return inputPath;
  }
  return path.join(process.env.HOME || "", inputPath.slice(2));
}

function readPng(filePath) {
  return PNG.sync.read(fs.readFileSync(filePath));
}

function getPixelIndex(width, x, y) {
  return (y * width + x) * 4;
}

function computeVisibleBounds(png) {
  let minX = png.width - 1;
  let minY = png.height - 1;
  let maxX = 0;
  let maxY = 0;

  for (let y = 0; y < png.height; y += 1) {
    for (let x = 0; x < png.width; x += 1) {
      const index = getPixelIndex(png.width, x, y);
      const alpha = png.data[index + 3];
      const brightness = Math.max(png.data[index], png.data[index + 1], png.data[index + 2]);
      if (alpha > 8 && brightness > 12) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
      }
    }
  }

  const width = maxX - minX + 1;
  const height = maxY - minY + 1;
  const radius = Math.max(18, Math.min(32, Math.round(Math.min(width, height) * 0.06)));
  return { x: minX, y: minY, width, height, radius };
}

function buildCropGrayscale(png, bounds) {
  const gray = new Uint8Array(bounds.width * bounds.height);
  for (let y = 0; y < bounds.height; y += 1) {
    for (let x = 0; x < bounds.width; x += 1) {
      const sourceIndex = getPixelIndex(png.width, bounds.x + x, bounds.y + y);
      const r = png.data[sourceIndex];
      const g = png.data[sourceIndex + 1];
      const b = png.data[sourceIndex + 2];
      gray[y * bounds.width + x] = Math.round(0.299 * r + 0.587 * g + 0.114 * b);
    }
  }
  return gray;
}

function patchWatermark(gray, width, height) {
  const patchHeight = Math.max(26, Math.min(70, Math.floor(height / 8)));
  const patchWidth = Math.max(72, Math.min(138, Math.floor(width / 4)));
  for (let y = 10; y < Math.min(height, 10 + patchHeight); y += 1) {
    for (let x = 12; x < Math.min(width, 12 + patchWidth); x += 1) {
      gray[y * width + x] = 0xfb;
    }
  }
}

function neighborhoodValues(gray, width, height, x, y) {
  const values = [];
  for (let offsetY = -1; offsetY <= 1; offsetY += 1) {
    for (let offsetX = -1; offsetX <= 1; offsetX += 1) {
      const sampleX = Math.max(0, Math.min(width - 1, x + offsetX));
      const sampleY = Math.max(0, Math.min(height - 1, y + offsetY));
      values.push(gray[sampleY * width + sampleX]);
    }
  }
  return values;
}

function medianBlur(gray, width, height) {
  const output = new Uint8Array(gray.length);
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const values = neighborhoodValues(gray, width, height, x, y).sort((a, b) => a - b);
      output[y * width + x] = values[4];
    }
  }
  return output;
}

function erode(gray, width, height) {
  const output = new Uint8Array(gray.length);
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const values = neighborhoodValues(gray, width, height, x, y);
      output[y * width + x] = Math.min(...values);
    }
  }
  return output;
}

function dilate(gray, width, height) {
  const output = new Uint8Array(gray.length);
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const values = neighborhoodValues(gray, width, height, x, y);
      output[y * width + x] = Math.max(...values);
    }
  }
  return output;
}

function morphologyOpen(gray, width, height) {
  return dilate(erode(gray, width, height), width, height);
}

function kmeans1d(gray, k, maxIterations = 14) {
  const centers = [];
  for (let index = 0; index < k; index += 1) {
    const ratio = index / Math.max(1, k - 1);
    centers.push(Math.round(20 + ratio * 220));
  }

  const assignments = new Uint8Array(gray.length);
  for (let iteration = 0; iteration < maxIterations; iteration += 1) {
    let changed = false;
    const sums = new Array(k).fill(0);
    const counts = new Array(k).fill(0);

    for (let index = 0; index < gray.length; index += 1) {
      const value = gray[index];
      let bestCluster = 0;
      let bestDistance = Number.POSITIVE_INFINITY;
      for (let cluster = 0; cluster < k; cluster += 1) {
        const distance = Math.abs(value - centers[cluster]);
        if (distance < bestDistance) {
          bestDistance = distance;
          bestCluster = cluster;
        }
      }
      if (assignments[index] !== bestCluster) {
        assignments[index] = bestCluster;
        changed = true;
      }
      sums[bestCluster] += value;
      counts[bestCluster] += 1;
    }

    for (let cluster = 0; cluster < k; cluster += 1) {
      if (counts[cluster] > 0) {
        centers[cluster] = sums[cluster] / counts[cluster];
      }
    }

    if (!changed) {
      break;
    }
  }

  return { centers, assignments };
}

function nearestPaletteValue(value) {
  let best = PALETTE_VALUES[0];
  let bestDistance = Number.POSITIVE_INFINITY;
  for (const paletteValue of PALETTE_VALUES) {
    const distance = Math.abs(value - paletteValue);
    if (distance < bestDistance) {
      bestDistance = distance;
      best = paletteValue;
    }
  }
  return best;
}

function quantizeGray(gray) {
  const { centers, assignments } = kmeans1d(gray, 6);
  const quantized = new Uint8Array(gray.length);
  for (let index = 0; index < gray.length; index += 1) {
    const clusterCenter = centers[assignments[index]];
    quantized[index] = nearestPaletteValue(clusterCenter);
  }
  return quantized;
}

function findBorderConnectedWhite(quantized, width, height) {
  const visited = new Uint8Array(width * height);
  const stack = [];
  const push = (x, y) => {
    if (x < 0 || y < 0 || x >= width || y >= height) {
      return;
    }
    const index = y * width + x;
    if (visited[index] || quantized[index] !== 0xfb) {
      return;
    }
    visited[index] = 1;
    stack.push(index);
  };

  for (let x = 0; x < width; x += 1) {
    push(x, 0);
    push(x, height - 1);
  }
  for (let y = 0; y < height; y += 1) {
    push(0, y);
    push(width - 1, y);
  }

  while (stack.length > 0) {
    const index = stack.pop();
    const x = index % width;
    const y = Math.floor(index / width);
    push(x + 1, y);
    push(x - 1, y);
    push(x, y + 1);
    push(x, y - 1);
  }

  return visited;
}

function buildRgbaCanvas(pngWidth, pngHeight, bounds, quantized, transparent) {
  const canvas = new Uint8ClampedArray(pngWidth * pngHeight * 4);
  const backgroundWhite = transparent
    ? findBorderConnectedWhite(quantized, bounds.width, bounds.height)
    : new Uint8Array(quantized.length);

  for (let y = 0; y < bounds.height; y += 1) {
    for (let x = 0; x < bounds.width; x += 1) {
      const cropIndex = y * bounds.width + x;
      const outputIndex = getPixelIndex(pngWidth, bounds.x + x, bounds.y + y);
      const value = quantized[cropIndex];
      const isTransparentWhite = transparent && backgroundWhite[cropIndex] === 1;
      canvas[outputIndex] = value;
      canvas[outputIndex + 1] = value;
      canvas[outputIndex + 2] = value;
      canvas[outputIndex + 3] = isTransparentWhite ? 0 : 255;
    }
  }

  return canvas;
}

function traceToSvg(width, height, rgbaData) {
  return ImageTracer.imagedataToSVG(
    {
      width,
      height,
      data: rgbaData,
    },
    {
      pal: PALETTE_RGBA,
      colorsampling: 0,
      numberofcolors: 5,
      pathomit: 8,
      ltres: 1,
      qtres: 1,
      rightangleenhance: false,
      layering: 0,
      blurradius: 0,
      linefilter: true,
      roundcoords: 1,
      viewbox: true,
      strokewidth: 0,
      desc: false,
    }
  );
}

function wrapSvg(svg, bounds, transparent) {
  const clipMarkup =
    `  <defs>\n` +
    `    <clipPath id="cardClip">\n` +
    `      <rect x="${bounds.x}" y="${bounds.y}" width="${bounds.width}" height="${bounds.height}" rx="${bounds.radius}" ry="${bounds.radius}"/>\n` +
    `    </clipPath>\n` +
    `  </defs>\n`;
  const cardMarkup = transparent
    ? ""
    : `    <rect x="${bounds.x}" y="${bounds.y}" width="${bounds.width}" height="${bounds.height}" rx="${bounds.radius}" ry="${bounds.radius}" fill="#FBFBFB"/>\n`;

  const cleaned = svg
    .replace("<svg ", '<svg shape-rendering="geometricPrecision" ')
    .replace(">\n", `>\n${clipMarkup}  <g clip-path="url(#cardClip)">\n${cardMarkup}`, 1)
    .replace("</svg>", "  </g>\n</svg>\n");
  return cleaned;
}

function ensureDir(directoryPath) {
  fs.mkdirSync(directoryPath, { recursive: true });
}

function generateReference(reference, inputDir, outputDir) {
  const png = readPng(path.join(inputDir, reference.filename));
  const bounds = computeVisibleBounds(png);
  let gray = buildCropGrayscale(png, bounds);
  patchWatermark(gray, bounds.width, bounds.height);
  gray = medianBlur(gray, bounds.width, bounds.height);
  if (reference.extraCleanup) {
    gray = morphologyOpen(gray, bounds.width, bounds.height);
  }
  const quantized = quantizeGray(gray);

  const researchSvg = wrapSvg(
    traceToSvg(
      png.width,
      png.height,
      buildRgbaCanvas(png.width, png.height, bounds, quantized, false)
    ),
    bounds,
    false
  );
  const transparentSvg = wrapSvg(
    traceToSvg(
      png.width,
      png.height,
      buildRgbaCanvas(png.width, png.height, bounds, quantized, true)
    ),
    bounds,
    true
  );

  const researchDir = path.join(outputDir, "research");
  const transparentDir = path.join(outputDir, "transparent");
  ensureDir(researchDir);
  ensureDir(transparentDir);
  fs.writeFileSync(path.join(researchDir, `${reference.name}.svg`), researchSvg);
  fs.writeFileSync(path.join(transparentDir, `${reference.name}.svg`), transparentSvg);
}

function main() {
  const inputDir = path.resolve(expandHome(resolveArg("--input-dir", "~/Desktop/polar bears")));
  const outputDir = path.resolve(resolveArg("--output-dir", "src/assets/bear"));

  for (const reference of REFERENCE_IMAGES) {
    generateReference(reference, inputDir, outputDir);
  }
}

main();
