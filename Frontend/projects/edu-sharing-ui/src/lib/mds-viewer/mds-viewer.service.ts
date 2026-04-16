import { ElementRef, Injectable, QueryList, ViewChildren } from '@angular/core';
import { MdsDefinition, MdsWidget } from 'ngx-edu-sharing-api';
import { BehaviorSubject } from 'rxjs';
import { Values } from '../services/search-helper.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { FormatSizePipe } from '../pipes/file-size.pipe';
import { VCardNamePipe } from '../pipes/vcard-name.pipe';
import { DateHelper } from '../util/DateHelper';

@Injectable()
export class MdsViewerService {
    @ViewChildren('container') container: QueryList<ElementRef>;
    values$ = new BehaviorSubject<Values>(undefined);
    mds$ = new BehaviorSubject<MdsDefinition>(undefined);
    constructor(private translate: TranslateService) {}
    getFormattedValue(value: string[], definition: MdsWidget, basicType: string): string[] {
        switch (basicType) {
            case 'date':
                return this.formatDate(value, definition);
            case 'text':
                return this.formatText(value, definition);
            case 'number':
                return this.formatNumber(value, definition);
            case 'vcard':
                return this.formatVCard(value);
        }
        return value;
    }

    private formatVCard(value: string[]): string[] {
        return value.map((v) => {
            return new VCardNamePipe(this.translate).transform(v);
        });
    }

    private formatDate(value: string[], definition: MdsWidget): string[] {
        return value.map((v) => {
            if (definition.format) {
                try {
                    return new DatePipe('en-US').transform(v, definition.format);
                } catch (e) {
                    console.warn('Could not format date', e, definition);
                }
            }
            return DateHelper.formatDate(this.translate, v, { showAlwaysTime: true });
        });
    }

    private formatText(value: string[], definition: MdsWidget): string[] {
        return value.map((v) => {
            if (definition.format) {
                return definition.format.replace('${value}', v);
            }
            return v;
        });
    }

    private formatNumber(value: string[], definition: MdsWidget): string[] {
        return value.map((value) => {
            if (definition.format === 'bytes') {
                return new FormatSizePipe(this.translate).transform(value);
            }
            return value;
        });
    }

    /**
     * returns the basic type for a widget definition
     * @param flat (flatten trees?)
     */
    static getBasicType(definition: MdsWidget, flat: boolean = true): string {
        switch (definition?.id) {
            case 'license':
                return 'license';
        }
        switch (definition?.type) {
            case 'text':
            case 'email':
            case 'month':
            case 'color':
            case 'textarea':
            case 'singleoption':
            case 'radioVertical':
            case 'radioHorizontal':
                return 'text';
            case 'number':
                return 'number';
            case 'date':
                return 'date';
            case 'vcard':
                return 'vcard';
            case 'multivalueFixedBadges':
            case 'multivalueSuggestBadges':
            case 'singlevalueSuggestBadges':
            case 'multivalueBadges':
            case 'multivalueButtons':
            case 'singlevalueTree':
            case 'multivalueTree':
                return flat ? 'array' : 'tree';
            case 'slider':
                return 'slider';
            case 'duration':
                return 'duration';
            case 'range':
                return 'range';
        }
        return 'unknown';
    }
}
