export class ColorHelper {
    public static cssColorToRgb(color: string): number[] {
        color = color.trim();
        if (color.startsWith('rgb')) {
            let result = /rgb.?\(\s*([\d]*)\s*,\s*([\d]*)\s*,\s*([\d*]*)\s*/i.exec(color);
            return result ? [parseInt(result[1]), parseInt(result[2]), parseInt(result[3])] : null;
        }
        let result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(color);
        return result
            ? [parseInt(result[1], 16), parseInt(result[2], 16), parseInt(result[3], 16)]
            : null;
    }

    /**
     * returns the estimated color brightness for a css color based on a pereceived brightness factor
     * Value between [0,1] and -1 if the css string was not readable
     * @param {string} color
     * @returns {number}
     */
    public static getColorBrightness(color: string) {
        const rgb = ColorHelper.cssColorToRgb(color);
        if (rgb) {
            // return (rgb[0]*0.2126 + rgb[1]*0.7152 + rgb[2]*0.0722) / 255;
            const a = rgb.map((v) => {
                v /= 255;
                return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
            });
            return a[0] * 0.2126 + a[1] * 0.7152 + a[2] * 0.0722;
        }
        return -1;
    }

    private static getRatio(l1: number, l2: number) {
        // calculate the color contrast ratio
        return l1 > l2 ? (l2 + 0.05) / (l1 + 0.05) : (l1 + 0.05) / (l2 + 0.05);
    }
    /**
     * returns 0 (black) or 1 (white)
     * @param color
     */
    public static getPreferredColor(color: string): PreferredColor {
        if (!color) {
            return PreferredColor.Black;
        }
        const brightness = ColorHelper.getColorBrightness(color);
        const ratioBlack = ColorHelper.getRatio(0, brightness);
        const ratioWhite = ColorHelper.getRatio(1, brightness);
        return ratioBlack > ratioWhite ? PreferredColor.Black : PreferredColor.White;
    }

    public static rgbToHex(rgb: number[]): string {
        return (
            '#' +
            ColorHelper.componentToHex(rgb[0]) +
            ColorHelper.componentToHex(rgb[1]) +
            ColorHelper.componentToHex(rgb[2])
        );
    }
    public static componentToHex(c: number) {
        var hex = Math.max(0, Math.min(Math.round(c), 255)).toString(16);
        return hex.length == 1 ? '0' + hex : hex;
    }
    public static rgbToHsl(rgb: number[]) {
        const r = rgb[0] / 255;
        const g = rgb[1] / 255;
        const b = rgb[2] / 255;
        const max = Math.max(r, g, b);
        const min = Math.min(r, g, b);
        // tslint:disable-next-line:one-variable-per-declaration prefer-const
        let h,
            s,
            l = (max + min) / 2;
        if (max === min) {
            h = s = 0;
        } else {
            const d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

            switch (max) {
                case r:
                    h = (g - b) / d + (g < b ? 6 : 0);
                    break;
                case g:
                    h = (b - r) / d + 2;
                    break;
                case b:
                    h = (r - g) / d + 4;
                    break;
            }
            h /= 6;
        }
        return [h, s, l];
    }
    private static hue2rgb(p: number, q: number, t: number) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1 / 6) return p + (q - p) * 6 * t;
        if (t < 1 / 2) return q;
        if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
        return p;
    }
    public static hslToRgb(hsl: number[]) {
        const h = hsl[0];
        const s = hsl[1];
        const l = hsl[2];
        // tslint:disable-next-line:one-variable-per-declaration
        let r, g, b;
        if (s === 0) {
            r = g = b = l;
        } else {
            const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            const p = 2 * l - q;

            r = ColorHelper.hue2rgb(p, q, h + 1 / 3);
            g = ColorHelper.hue2rgb(p, q, h);
            b = ColorHelper.hue2rgb(p, q, h - 1 / 3);
        }

        return [r * 255, g * 255, b * 255];
    }
}
export enum PreferredColor {
    Black,
    White,
}
