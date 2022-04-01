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
    /vendor.js:/,
];

interface Frame {
    error: {
        stack: string;
    };
}

function filterFrames(stack: string) {
    return stack
        .split(NEWLINE)
        .filter((frame) => !FILTER_REGEXP.some((reg) => reg.test(frame)))
        .join(NEWLINE);
}

// Adapted from https://stackoverflow.com/a/54943260
function renderLongStackTrace(frames: Frame[]): string {
    if (!frames) {
        return 'no frames';
    }
    return frames
        .filter((frame) => frame.error.stack)
        .map((frame: any) => filterFrames(frame.error.stack))
        .join(NEWLINE);
}

function getFirstRelevantFrame(frames: Frame[]): string {
    return frames?.[0].error.stack
        .split(NEWLINE)
        .find((frame) => !FILTER_REGEXP.some((reg) => reg.test(frame)));
}

export function printCurrentTaskInfo(prefix: string): void {
    const frames: Frame[] = (Zone.currentTask?.data as any)?.__creationTrace__;
    console.log(prefix, Zone.currentTask?.source, getFirstRelevantFrame(frames));
}
