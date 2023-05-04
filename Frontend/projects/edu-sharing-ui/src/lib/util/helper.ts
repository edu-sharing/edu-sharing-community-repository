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
}
