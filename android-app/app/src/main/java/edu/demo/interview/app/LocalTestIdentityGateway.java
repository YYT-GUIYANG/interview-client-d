package edu.demo.interview.app;

import java.util.Optional;

/** Explicit local stub. Do not claim S-01 multi-user auth is complete. */
public final class LocalTestIdentityGateway implements IdentityGateway {
    private Identity identity;

    @Override public synchronized Optional<Identity> current() {
        return Optional.ofNullable(identity);
    }

    @Override public synchronized void setLocalTestIdentity(String accountId, String displayName) {
        if (accountId == null || accountId.trim().isEmpty())
            throw new IllegalArgumentException("accountId required");
        identity = new Identity(accountId.trim(), displayName == null ? "" : displayName.trim(), false);
    }

    @Override public synchronized void clear() { identity = null; }

    @Override public synchronized boolean canAccess(String ownerAccountId) {
        if (identity == null || !identity.verified) {
            // Unverified local test identity may only see records tagged with the same accountId.
            return identity != null && identity.accountId.equals(ownerAccountId);
        }
        return identity.accountId.equals(ownerAccountId);
    }
}
