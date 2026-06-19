import { FrontendModuleConfig } from './FrontendModuleConfig';

export type RenderData = {
    items?: Array<AssetStateItem>;
    module: string;
    frontendModuleConfig?: FrontendModuleConfig;
    publicErrorMessage?: string;
};

export type AssetStateItem = {
    link: string;
    progress: number;
    height: number;
    width: number;
    publicErrorMessage?: string;
    status: 'QUEUED' | 'PROCESSING' | 'FINISHED' | 'FAILED';
    additionalData?: {
        [key: string]: string;
    };
};
