import { Pipe, PipeTransform } from '@angular/core';
import { ColorHelper } from 'ngx-edu-sharing-ui';

type Settings = {
    brightness?: number; // 0...infinite
    saturation?: number; // 0...infinite
};
@Pipe({ name: 'appColorTransform' })
export class ColorTransformPipe implements PipeTransform {
    transform(color: string, args: Settings = {}): string {
        const rgb = ColorHelper.cssColorToRgb(color);
        const hsl = ColorHelper.rgbToHsl(rgb);
        hsl[1] *= args.saturation ?? 1;
        hsl[2] *= args.brightness ?? 1;
        console.log(rgb);
        console.log(hsl);
        return ColorHelper.rgbToHex(ColorHelper.hslToRgb(hsl));
    }
}
