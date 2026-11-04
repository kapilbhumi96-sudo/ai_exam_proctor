package com.proctor.rules;

/** The kinds of suspicious situations the rule engine can report. */
public enum EventType {
    NO_FACE("NO FACE"),
    MULTIPLE_FACES("MULTIPLE FACES"),
    LOOKING_AWAY("LOOKING AWAY");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    /** Short text for on-screen banners. */
    public String label() {
        return label;
    }
}
