package io.github.ad417;

public enum Validator {
    TRUE,
    UNKNOWN,
    FALSE;

    public Validator and(Validator other) {
        if (this.equals(FALSE) || other.equals(FALSE)) return FALSE;
        if (this.equals(UNKNOWN) || other.equals(UNKNOWN)) return UNKNOWN;
        return TRUE;
    }

    public Validator or(Validator other) {
        if (this.equals(TRUE) || other.equals(TRUE)) return TRUE;
        if (this.equals(UNKNOWN) || other.equals(UNKNOWN)) return UNKNOWN;
        return FALSE;
    }

    public Validator invert() {
        return switch (this) {
            case UNKNOWN -> UNKNOWN;
            case FALSE -> TRUE;
            default -> FALSE;
        };
    }

    // XNOR
    public Validator matches(Validator other) {
        if (this.equals(UNKNOWN) || other.equals(UNKNOWN)) return UNKNOWN;
        if (other.equals(TRUE)) return this;
        return this.equals(TRUE) ? FALSE : TRUE;
    }

    public boolean value() {
        return !this.equals(FALSE);
    }
}
