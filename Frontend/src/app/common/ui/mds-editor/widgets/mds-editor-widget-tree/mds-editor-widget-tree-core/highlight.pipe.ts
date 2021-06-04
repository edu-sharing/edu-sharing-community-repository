import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'highlight',
})
export class HighlightPipe implements PipeTransform {
    transform(value: string, highlightString: string): string {
        if (highlightString && highlightString.trim()) {
            const highlightWords = highlightString.trim().toLowerCase().split(/\s+/);
            let index = -1;
            do {
                let length: number;
                ({ index, length } = this.findNearestHit(
                    value.toLowerCase(),
                    highlightWords,
                    index + 1,
                ));
                // (index === 0 || (index > 0 && /\s/.test(value[index - 1]))) {
                if (index !== -1) {
                    const matchingString = value.substr(index, length);
                    value =
                        value.substr(0, index) +
                        '<mark>' +
                        matchingString +
                        '</mark>' +
                        value.substr(index + length);
                    index += '<mark></mark>'.length;
                }
            } while (index >= 0);
        }
        return value;
    }

    private findNearestHit(hayStack: string, searchWords: string[], startPosition: number) {
        let index = -1;
        let length: number;
        for (const searchWord of searchWords) {
            const wordIndex = hayStack.indexOf(searchWord, startPosition);
            if (wordIndex >= 0 && (wordIndex < index || index < 0)) {
                index = wordIndex;
                length = searchWord.length;
            }
        }
        return { index, length };
    }
}
