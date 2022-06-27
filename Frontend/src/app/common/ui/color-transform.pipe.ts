import { PipeTransform, Pipe } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { RestHelper } from '../../core-module/core.module';
import { Node } from '../../core-module/core.module';
import { ColorHelper } from '../../core-module/ui/color-helper';

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
