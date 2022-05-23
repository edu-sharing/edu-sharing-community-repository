import { MdsWidgetValue } from '../../types/types';

export interface DisplayValue {
    key: string;
    label: string;
    hint?: string;
}

export class DisplayValues  {
    values: DisplayValue[];

    static fromMdsValues(values: MdsWidgetValue[]): DisplayValues {
        const displayValues = new DisplayValues();
        displayValues.values = values.map((value) => displayValues.toDisplayValue(value));
        return displayValues;
    }

    private constructor() {
        this.values = [];
    }

    get(key: string): DisplayValue {
        return this.values.find((value) => (value.key === key));
    }

    toDisplayValue(value: MdsWidgetValue): DisplayValue {
        return {
            key: value.id,
            label: value.caption,
        };
    }
}
