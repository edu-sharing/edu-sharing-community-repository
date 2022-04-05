const NEWLINE = '\n';

// edit this array if you want to ignore or unignore something
const FILTER_REGEXP: RegExp[] = [
    /Error: STACKTRACE TRACKING/,
    /checkAndUpdateView/,
    /callViewAction/,
    /execEmbeddedViewsAction/,
    /execComponentViewsAction/,
    /callWithDebugContext/,
    /debugCheckDirectivesFn/,
    /Zone/,
    /checkAndUpdateNode/,
    /debugCheckAndUpdateNode/,
    /onScheduleTask/,
    /onInvoke/,
    /updateDirectives/,
    /@angular/,
    /Observable\._trySubscribe/,
    /Observable.subscribe/,
    /SafeSubscriber/,
    /Subscriber.js.Subscriber/,
    /checkAndUpdateDirectiveInline/,
    /drainMicroTaskQueue/,
    /getStacktraceWithUncaughtError/,
    /LongStackTrace/,
    /Observable._zoneSubscribe/,
    /polyfills.js:/,
    /(<anonymous>)/,
];

interface Frame {
    error: {
        stack: string;
    };
}

function concatStacks(frames: Frame[] = []): string[] {
    const stacks = frames
        .filter((frame) => frame.error.stack)
        .map((frame) => frame.error.stack)
        .map((stack) => stack.split(NEWLINE));
    return [].concat(...stacks);
}

function filterFrames(stack: string[]): string[] {
    return stack.filter((frame) => !FILTER_REGEXP.some((reg) => reg.test(frame)));
}

function isVendor(frame: string): boolean {
    return /vendor.js:/.test(frame);
}

// Adapted from https://stackoverflow.com/a/54943260
function getLongStackTrace(frames: Frame[]): string[] {
    const stack = concatStacks(frames);
    return filterFrames(stack);
}

function getStackTraceLog(stack: string[], maxVendorLines: number, maxLines: number): string[] {
    let vendorLines = 0;
    let lines = 0;
    let message = '';
    let format: string[] = [];
    for (const frame of stack) {
        if (isVendor(frame)) {
            if (vendorLines < maxVendorLines) {
                message += `\n%c${frame}`;
                format.push('');
                vendorLines++;
            } else {
                continue;
            }
        } else {
            message += `\n%c${frame}`;
            format.push('font-weight: bold');
        }
        lines++;
        if (lines >= maxLines) {
            break;
        }
    }
    return [message, ...format];
}

export function printCurrentTaskInfo(prefix: string): void {
    const frames: Frame[] = (Zone.currentTask?.data as any)?.__creationTrace__;
    const stack = getLongStackTrace(frames);
    const [message, ...format] = getStackTraceLog(stack, 2, 5);
    console.log(prefix + ' ' + Zone.currentTask?.source + message, ...format);
}
