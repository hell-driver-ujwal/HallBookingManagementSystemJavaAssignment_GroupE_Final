package com.hallbooking.models;

/**
 * HallEvent — abstract base class shared by Issue and Maintenance.
 *
 * Both are "events that happen to a hall" and share a hallId and a status.
 * The isResolved() method is overridden differently in each subclass,
 * demonstrating polymorphism.
 *
 * OOP Concepts demonstrated:
 *   - Abstraction  : HallEvent cannot be instantiated directly
 *   - Inheritance  : Issue and Maintenance both extend HallEvent
 *   - Polymorphism : isResolved() behaves differently per subclass
 */
public abstract class HallEvent {

    protected String hallId;
    protected String status;

    public HallEvent() {}

    public HallEvent(String hallId, String status) {
        this.hallId  = hallId;
        this.status  = status;
    }

    // ── Shared getters ─────────────────────────────────────────────
    public String getHallId()  { return hallId; }
    public String getStatus()  { return status; }

    // ── Shared setters ─────────────────────────────────────────────
    public void setHallId(String hallId)   { this.hallId  = hallId; }
    public void setStatus(String status)   { this.status  = status; }

    // ── Abstract methods — each subclass defines its own resolution logic ──
    /**
     * Returns true if this event is fully resolved and requires no more action.
     * Implemented differently by Issue and Maintenance.
     */
    public abstract boolean isResolved();

    /** Serialize this object to a pipe-separated string for .txt storage. */
    public abstract String toFileString();

    @Override
    public abstract String toString();
}
