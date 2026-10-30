import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'highlight',
    standalone: false,
})
export class HighlightPipe implements PipeTransform {
    /** @param byWord whether to mark each word of the highlight string instead of it as a whole */
    transform(value: string, highlightString: string, byWord = false): string {
        if (highlightString && highlightString.trim()) {
            const highlight = highlightString.trim().toLowerCase();
            const highlightWords = byWord
                ? highlight.split(/\s+/)
                : [highlight.replace(/\s+/g, ' ')];
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
                    const addStr = '<mark>' + matchingString + '</mark>';
                    value = value.substr(0, index) + addStr + value.substr(index + length);
                    index += addStr.length;
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
