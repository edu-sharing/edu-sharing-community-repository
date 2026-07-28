import { FrontendModuleConfig } from './FrontendModuleConfig';

export type RenderData = {
    items?: Array<AssetStateItem>;
    module: string;
    frontendModuleConfig?: FrontendModuleConfig;
    publicErrorMessage?: string;
    // Set when the module deferred its (expiring) links: no links are fetched yet; the consumer
    // renders its own action UI and calls RenderComponent.fetchLinks() on click to obtain them.
    deferred?: boolean;
};

export type AssetStateItem = {
    link: string;
    progress: number;
    height: number;
    width: number;
    publicErrorMessage?: string;
    status: 'QUEUED' | 'PROCESSING' | 'FINISHED' | 'FAILED' | 'TIMEOUT';
    additionalData?: {
        [key: string]: string;
    };
};
