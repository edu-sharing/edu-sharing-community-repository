import { Node, RestConstants } from 'ngx-edu-sharing-api';
/**
 * Different helper functions, may be used globally
 */
export class RestHelper {
    protected static SPACES_STORE_REF = 'workspace://SpacesStore/';
    public static getName(node: Node): string {
        if (node.name) return node.name;
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
        return properties[RestConstants.LOM_PROP_TITLE]
            ? properties[RestConstants.LOM_PROP_TITLE]
            : properties[RestConstants.CM_NAME];
    }
}
