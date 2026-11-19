import { InjectionToken } from '@angular/core';
import { CombinedRenderData } from 'ngx-edu-sharing-ui';

/**
 * Loads the render data of a node, see `RenderWrapperComponent.dataLoader`.
 */
export type RenderDataLoader = (nodeId: string) => Promise<CombinedRenderData>;

/**
 * Lets a host provide how the render data is loaded for everything below it, also for render wrappers
 * it does not create itself (e.g. the one inside the preview of the editorial sidebar). Without it,
 * the logged in node api is used.
 */
export const RENDER_DATA_LOADER = new InjectionToken<RenderDataLoader>('RENDER_DATA_LOADER');
