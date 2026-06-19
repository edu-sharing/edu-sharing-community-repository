export type FrontendModuleConfig = {
    module: string;
    urlModuleConfig: UrlModuleConfig | null;
};

export type UrlModuleConfig = {
    embedding: UrlEmbeddings;
    externalId: string;
};

export enum UrlEmbeddings {
    YOUTUBE,
    VIMEO,
    VIDEO,
    AUDIO,
    IMAGE,
    LTI13TOOL,
    H5P,
    PREZI,
    LEARNINGAPPS,
    LINK,
    SODIX,
    PIXABAY,
    OERSI,
    BROCKHAUS,
}
