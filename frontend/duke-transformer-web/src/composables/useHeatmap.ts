interface ColorResult {
  bgColor: string;
  textColor: string;
}

function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result
    ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
      }
    : { r: 255, g: 255, b: 255 };
}

function interpolateColor(color1: string, color2: string, factor: number): string {
  const rgb1 = hexToRgb(color1);
  const rgb2 = hexToRgb(color2);

  const r = Math.round(rgb1.r + (rgb2.r - rgb1.r) * factor);
  const g = Math.round(rgb1.g + (rgb2.g - rgb1.g) * factor);
  const b = Math.round(rgb1.b + (rgb2.b - rgb1.b) * factor);

  return `rgb(${r}, ${g}, ${b})`;
}

function getContrastColor(bgColorStr: string): string {
  const match = bgColorStr.match(/\d+/g);
  if (!match || match.length < 3) {
    return '#000000';
  }

  const r = parseInt(match[0]);
  const g = parseInt(match[1]);
  const b = parseInt(match[2]);

  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance < 0.5 ? '#ffffff' : '#000000';
}

export function useHeatmap() {
  function getColor(value: number, minVal: number, maxVal: number, scheme: string): ColorResult {
    let normalized: number;

    if (minVal === maxVal) {
      normalized = 0.5;
    } else {
      normalized = (value - minVal) / (maxVal - minVal);
      normalized = Math.max(0, Math.min(1, normalized));
    }

    let bgColor: string;

    if (scheme === 'red') {
      bgColor = interpolateColor('#ffffff', '#dc2626', normalized);
    } else if (scheme === 'diverging') {
      if (normalized < 0.5) {
        bgColor = interpolateColor('#2563eb', '#ffffff', normalized * 2);
      } else {
        bgColor = interpolateColor('#ffffff', '#dc2626', (normalized - 0.5) * 2);
      }
    } else {
      bgColor = interpolateColor('#ffffff', '#1e40af', normalized);
    }

    const textColor = getContrastColor(bgColor);
    return { bgColor, textColor };
  }

  return {
    getColor
  };
}
