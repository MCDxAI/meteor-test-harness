package io.mcdxai.harness.universal.mcp;

public final class SessionGate {
    private String ownerSessionId;

    public synchronized boolean claimOrValidate(String sessionId, boolean singleSessionMode) {
        if (!singleSessionMode) {
            return true;
        }

        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }

        if (ownerSessionId == null) {
            ownerSessionId = sessionId;
            return true;
        }

        return ownerSessionId.equals(sessionId);
    }

    public synchronized void release(String sessionId) {
        if (ownerSessionId != null && ownerSessionId.equals(sessionId)) {
            ownerSessionId = null;
        }
    }

    public synchronized void clear() {
        ownerSessionId = null;
    }

    public synchronized String ownerSessionId() {
        return ownerSessionId;
    }
}
