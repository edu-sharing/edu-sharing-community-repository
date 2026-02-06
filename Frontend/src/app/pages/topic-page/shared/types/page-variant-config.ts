import { PageStructure } from './page-structure';

export interface PageVariantConfig {
    variables?: { [key: string]: string };
    template: {
        id: string;
        lastModified?: string;
        version: string;
    };
    structure: PageStructure;
}
