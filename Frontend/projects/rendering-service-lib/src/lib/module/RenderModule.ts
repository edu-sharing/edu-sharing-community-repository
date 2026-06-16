import { RenderDataResponse } from 'ngx-rendering-service-api';
import { Node } from 'ngx-edu-sharing-api';
import { RenderData } from '../dto/RenderData';
export interface RenderModule {
    data: RenderData | undefined;
    node: Node | undefined;
}
