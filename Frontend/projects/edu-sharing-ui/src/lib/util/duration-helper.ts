export enum DurationFormat {
    /**
     * format with colon, i.e. H:MM:SS
     */
    Colon = 'colon',
    /**
     * format with Hh Mm Ss
     */
    Hms = 'hms',
}
export class DurationHelper {
    public static getDurationInSeconds(value: string): number {
        // PT1H5M23S
        // or 00:00:00
        // or 00:00
        if (!value) return 0;
        try {
            let result = value.split(':');
            if (result.length == 3) {
                let h = result[0] ? parseInt(result[0]) : 0;
                let m = result[1] ? parseInt(result[1]) : 0;
                let s = result[2] ? parseInt(result[2]) : 0;
                let time = h * 60 * 60 + m * 60 + s;
                return time;
            }
            if (result.length == 2) {
                let m = result[0] ? parseInt(result[0]) : 0;
                let s = result[1] ? parseInt(result[1]) : 0;
                let time = m * 60 + s;
                return time;
            }
        } catch (e) {
            return value as unknown as number;
        }
        try {
            let regexp = new RegExp('PT(\\d+H)?(\\d+M)?(\\d+S)?');
            let result = regexp.exec(value);
            let h = result[1] ? parseInt(result[1]) : 0;
            let m = result[2] ? parseInt(result[2]) : 0;
            let s = result[3] ? parseInt(result[3]) : 0;
            let time = h * 60 * 60 + m * 60 + s;
            return time;
        } catch (e) {
            return value as unknown as number;
        }
    }
    public static getDurationFormatted(
        duration: string,
        format: DurationFormat = DurationFormat.Colon,
    ): string {
        let time = DurationHelper.getDurationInSeconds(duration);
        if (!time) return '';
        let h = Math.floor(time / 60 / 60);
        let m = Math.floor(Math.floor(time / 60) % 60);
        let s = Math.floor(time % 60);
        let options: Intl.NumberFormatOptions = {
            minimumIntegerDigits: 2,
            maximumFractionDigits: 0,
        };
        let numberFormat = new Intl.NumberFormat([], options);
        let str = '';
        /*
      if(h>0) {
        str = format.format(h) + "h";
      }
      if(m>0) {
        if (str)
          str += " ";
        str += format.format(m) + "m";
      }
      if(s>0) {
        if (str)
          str += " ";
        str += format.format(s) + "s";
      }
      */
        if (format === DurationFormat.Colon) {
            str =
                numberFormat.format(h) +
                ':' +
                numberFormat.format(m) +
                ':' +
                numberFormat.format(s);
        } else {
            if (h > 0) {
                str += h + 'h';
            }
            if (m > 0) {
                str += ' ' + m + 'm';
            }
            if (s > 0) {
                str += ' ' + s + 's';
            }
            str = str.trim();
        }
        return str;
    }
}
