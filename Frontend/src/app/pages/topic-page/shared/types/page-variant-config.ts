import { PageStructure } from './page-structure';

export interface PageVariantConfig {
    template: {
        id: string;
        lastModified?: string;
        version: string;
    };
    structure: PageStructure;
}
