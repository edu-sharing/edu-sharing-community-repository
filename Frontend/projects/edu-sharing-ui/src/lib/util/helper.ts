import { isNumeric } from './isNumeric';
import { isArray } from 'lodash';

export class Helper {
    public static deepCopy(data: any) {
        if (data == null) return null;
        return JSON.parse(JSON.stringify(data));
    }

    public static deepCopyArray(data: any[]) {
        if (!Array.isArray(data)) return data;
        return data.slice();
    }

    /**
     * Returns true if both objects have the same values stored
     * will not work for classes including methods or similar dynamic data
     * @param object1
     * @param object2
     * @returns {boolean}
     */
    public static objectEquals(object1: any, object2: any) {
        if (object1 == null) return object2 == null;
        if (object2 == null) return object1 == null;
        return JSON.stringify(object1) == JSON.stringify(object2);
    }

    /**
     * get data from an object based on a dotted path
     * also supports arrays like "array.0.key"
     * @param nested the object to return the data from
     * @param path the path of the field location in dotted notation
     */
    public static getDotPathFromNestedObject(nested: any, path: string) {
        const split = path.split('.');
        let obj = nested;
        for (let key of split) {
            if (isNumeric(key)) {
                obj = obj?.[parseInt(key)];
            } else {
                if (isArray(obj)) {
                    return obj;
                }
                obj = obj?.[key];
            }
        }
        return obj;
    }
    public static filterObjectPropertyNested(obj: any, property: string[]) {
        for (let i in obj) {
            if (!obj.hasOwnProperty(i)) continue;
            if (property.includes(i)) {
                delete obj[i];
            } else if (typeof obj[i] === 'object') {
                Helper.filterObjectPropertyNested(obj[i], property);
            }
        }
        return obj;
    }
}
