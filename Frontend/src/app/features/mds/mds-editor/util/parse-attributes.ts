import { MdsWidget } from '../../types/types';

export function parseAttributes(html: string, widgetDefinition: MdsWidget): MdsWidget {
    const attributes = getAttributes(html, widgetDefinition.id);
    if (attributes.length === 0) {
        return widgetDefinition;
    }
    const definitionCopy = { ...widgetDefinition };
    for (const attribute of attributes) {
        overrideAttribute(attribute, definitionCopy);
    }
    return definitionCopy;
}

type Type = 'boolean' | 'string' | 'object' | 'number';

type PartialBy<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;

interface PropertyInfo {
    type: Type;
    property: string;
}

const knownPropertyInfo: { [property: string]: PartialBy<PropertyInfo, 'property'> } = {
    allowempty: { type: 'boolean' },
    bottomCaption: { type: 'string' },
    caption: { type: 'string' },
    condition: { type: 'object' },
    defaultMax: { type: 'number' },
    defaultMin: { type: 'number' },
    defaultvalue: { type: 'string' },
    hasValues: { type: 'boolean' },
    hideIfEmpty: { type: 'boolean' },
    icon: { type: 'string' },
    id: { type: 'string' },
    isExtended: { type: 'boolean' },
    extended: { type: 'boolean', property: 'isExtended' },
    isRequired: { type: 'string' },
    required: { type: 'string', property: 'isRequired' },
    isSearchable: { type: 'boolean' },
    link: { type: 'string' },
    max: { type: 'number' },
    maxlength: { type: 'number' },
    min: { type: 'number' },
    placeholder: { type: 'string' },
    step: { type: 'number' },
    subwidgets: { type: 'object' },
    template: { type: 'string' },
    type: { type: 'string' },
    unit: { type: 'string' },
    values: { type: 'object' },
};

export type Attributes = { [key: string]: string };
export function getAttributesArray(html: string, widgetId: string): Attributes {
    const attr = getAttributes(html, widgetId);
    const result: Attributes = {};
    attr.forEach((attr) => (result[attr.name] = attr.value));
    return result;
}

function getAttributes(html: string, widgetId: string): Attr[] {
    const div = document.createElement('div');
    div.innerHTML = html;
    const elements = div.getElementsByTagName(widgetId);
    if (elements.length === 0) {
        console.warn(html);
        throw new Error('Failed to parse attributes: widget not found in template: ' + widgetId);
    } else if (elements.length > 1) {
        throw new Error(
            'Failed to parse attributes: widget more than once in template: ' + widgetId,
        );
    }
    return Array.from(elements[0].attributes);
}

function overrideAttribute(attribute: Attr, widgetDefinition: MdsWidget): void {
    const { property, type } =
        getKnownPropertyInfo(attribute) ?? guessPropertyInfo(attribute, widgetDefinition);
    (widgetDefinition as any)[property] = parseValue(attribute.value, type);
}

function getKnownPropertyInfo(attribute: Attr): PropertyInfo | null {
    const property = Object.keys(knownPropertyInfo).find(
        (property) => property.toLowerCase() === attribute.name,
    );
    if (property) {
        return {
            property: knownPropertyInfo[property].property ?? property,
            type: knownPropertyInfo[property].type,
        };
    } else {
        return null;
    }
}

function guessPropertyInfo(attribute: Attr, widgetDefinition: MdsWidget): PropertyInfo {
    const property = Object.keys(widgetDefinition).find(
        (property) => property.toLowerCase() === attribute.name,
    );
    if (['defaulttab'].includes(attribute.name)) {
        return { property, type: 'string' };
    }
    if (property) {
        const type = typeof (widgetDefinition as any)[property];
        if (
            type === 'number' ||
            type === 'string' ||
            type === 'boolean' ||
            (type === 'object' && (widgetDefinition as any)[property] !== null)
        ) {
            return { property, type };
        }
    } else if (!['style', 'class'].includes(attribute.name)) {
        console.warn(
            `Encountered unknown attribute in widget definition for ${widgetDefinition.id}:`,
            attribute,
        );
    }
    return {
        property: property ?? attribute.name,
        type: 'string',
    };
}

function parseValue(value: string, type: 'boolean'): boolean;
function parseValue(value: string, type: 'string'): string;
function parseValue(value: string, type: 'object'): any;
function parseValue(value: string, type: 'number'): number;
function parseValue(value: string, type: Type): boolean | string | number | any;
function parseValue(value: string, type: Type): boolean | string | number | any {
    switch (type) {
        case 'boolean':
            return value?.toLowerCase() === 'true';
        case 'number':
            return parseInt(value, 10);
        case 'string':
            return value;
        case 'object':
            return JSON.parse(value);
        default:
            throw new Error('Failed to parse attributes: type not supported: ' + type);
    }
}
