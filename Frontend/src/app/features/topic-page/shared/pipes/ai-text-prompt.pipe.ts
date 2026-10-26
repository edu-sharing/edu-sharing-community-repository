import { Pipe, PipeTransform } from '@angular/core';
import { PromptToTextMapping } from '../types/prompt-to-text-mapping';

@Pipe({
    name: 'aiTextPrompt',
    standalone: true,
})
export class AiTextPromptPipe implements PipeTransform {
    transform(
        existingText: string = '',
        editMode: boolean,
        mapping: PromptToTextMapping | false,
    ): string {
        // this assumes that once a mapping exists, it is an AI prompt
        if (mapping) {
            return editMode ? mapping.prompt : mapping.text;
        }
        return existingText;
    }
}
