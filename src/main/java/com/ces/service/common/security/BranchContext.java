package com.ces.service.common.security;

import java.util.UUID;

/**
 * ThreadLocal holder for the active branch of the current request.
 *
 * <p>Per SRS §5.4, the {@code X-Branch-Id} header value is validated against the
 * branches embedded in the JWT and then written here by {@code BranchContextFilter}.
 * Service-layer queries read it to enforce row-level multi-branch tenancy.</p>
 */
public final class BranchContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private BranchContext() {
    }

    public static void set(UUID branchId) {
        CURRENT.set(branchId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    /**
     * @return the active branch id, never null.
     * @throws IllegalStateException when no branch is bound to the current thread.
     */
    public static UUID require() {
        UUID id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("No active branch bound to the current request");
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
