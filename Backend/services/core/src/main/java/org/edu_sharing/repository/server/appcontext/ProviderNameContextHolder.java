package org.edu_sharing.repository.server.appcontext;

class ProviderNameContextHolder {

    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    public static void setProviderName(String providerName) {
        contextHolder.set(providerName);
    }

    public static String getProviderName() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }
}
