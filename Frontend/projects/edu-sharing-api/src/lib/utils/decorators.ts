/**
 * Caches and shares return values based on arguments.
 *
 * Arguments need to be serializable.
 */
export function shareReturnValue() {
    return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
        const returnValuesMap: { [jsonArgs: string]: any } = {};
        const originalFunction = descriptor.value;
        descriptor.value = function (this: any, ...args: any[]) {
            const jsonArgs = JSON.stringify(args);
            if (!returnValuesMap[jsonArgs]) {
                returnValuesMap[jsonArgs] = originalFunction.apply(this, args);
            }
            return returnValuesMap[jsonArgs];
        };
    };
}
