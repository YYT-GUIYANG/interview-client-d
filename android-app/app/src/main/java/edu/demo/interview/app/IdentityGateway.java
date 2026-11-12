package edu.demo.interview.app;

import java.util.Optional;

/**
 * Account boundary for C's contract. Until C freezes fields, only an explicit
 * local test identity can be set; this is NOT multi-user authentication.
 *
 * Pending confirmation with C:
 * - account_id / candidate_id field name
 * - token / session validation endpoint
 * - ownership check for list/read/delete
 * - key rotation and retention rules
 */
public interface IdentityGateway {
    final class Identity {
        public final String accountId;
        public final String displayName;
        public final boolean verified;
        public Identity(String accountId, String displayName, boolean verified) {
            this.accountId = accountId;
            this.displayName = displayName;
            this.verified = verified;
        }
    }

    Optional<Identity> current();
    /** Prototype-only local test identity. Production must call C's auth service. */
    void setLocalTestIdentity(String accountId, String displayName);
    void clear();
    boolean canAccess(String ownerAccountId);
}
