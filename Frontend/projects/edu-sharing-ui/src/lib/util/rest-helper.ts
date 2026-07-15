import { Node, RestConstants } from 'ngx-edu-sharing-api';

/**
 * Different helper functions, may be used globally
 */
export class RestHelper {
    protected static SPACES_STORE_REF = 'workspace://SpacesStore/';
    public static getName(node: Node): string {
        if (node.name) return node.name;
        if (node.properties?.[RestConstants.CM_NAME])
            return node.properties[RestConstants.CM_NAME].join();
        if (node.title) return node.title;
        if (node.ref) return node.ref.id;
        return null;
    }
    public static getTitle(node: Node): string {
        if (node?.title) return node.title;
        if (node?.name) {
            return node?.name;
        }
        if (node?.properties) {
            return RestHelper.getTitleFromProperties(node?.properties);
        }
        return '';
    }
    public static getTitleFromProperties(properties: any): string {
        const value = properties[RestConstants.LOM_PROP_TITLE]
            ? properties[RestConstants.LOM_PROP_TITLE]
            : properties[RestConstants.CM_NAME];
        if (Array.isArray(value)) {
            return value.join(', ');
        }
        return value;
    }

    public static hasAccessPermission(node: Node, permission: string): boolean {
        return node.access && node.access.indexOf(permission) != -1;
    }

    public static guessMimeType(file: File): string {
        let type = file.type;
        if (type == 'application/x-zip-compressed') type = 'application/zip';
        return type;
    }
}
