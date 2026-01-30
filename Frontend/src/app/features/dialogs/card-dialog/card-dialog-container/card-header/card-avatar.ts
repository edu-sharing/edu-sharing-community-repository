import { Node } from 'ngx-edu-sharing-api';

export type CardAvatar =
    | { kind: 'image'; url: string }
    | { kind: 'icon'; icon: string }
    | { kind: 'node'; node: Node };
