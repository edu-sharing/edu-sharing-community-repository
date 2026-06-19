import { Injectable, inject } from '@angular/core';
import { ConfigService, Node } from 'ngx-edu-sharing-api';
import { GdprConfig } from './lib/dto/GdprConfig';

@Injectable({ providedIn: 'root' })
export class GdprService {
    private configService = inject(ConfigService);

    async getGdprConfig(node: Node): Promise<GdprConfig | null> {
        const result = await this.configService.get('gdpr');
        if (typeof result !== 'object') {
            return null;
        }
        const gdprConfig = result as any;
        if (!gdprConfig.enabled || !Array.isArray(gdprConfig.entry)) {
            return null;
        }
        let url = '';
        if (node.properties) {
            url = node.properties['ccm:wwwurl']?.[0] || '';
        }
        if (!url) {
            return null;
        }
        for (const entry of gdprConfig.entry) {
            if (!(entry.regex && entry.name && entry.ref)) {
                continue;
            }
            try {
                const regex = new RegExp(entry.regex);
                if (regex.test(url)) {
                    return {
                        pattern: entry.regex,
                        name: entry.name,
                        ref: entry.ref,
                    };
                }
            } catch (e) {
                console.warn('Invalid regex pattern:', entry.regex, e);
            }
        }
        return null;
    }
}
