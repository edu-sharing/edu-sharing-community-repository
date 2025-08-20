package org.edu_sharing.service.share;

public class ShareInfoContextHolder {
    private static final ThreadLocal<ShareInfoContext> context = new ThreadLocal<>();

    public static ShareInfoContext getContext() {
        if (context.get() == null) {
            context.set(new ShareInfoContext());
        }
        return context.get();
    }
}

