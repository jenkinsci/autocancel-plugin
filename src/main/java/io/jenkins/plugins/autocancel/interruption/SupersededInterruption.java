package io.jenkins.plugins.autocancel.interruption;

import jenkins.model.CauseOfInterruption;

public class SupersededInterruption extends CauseOfInterruption {
    private final String cause;

    public SupersededInterruption(String cause) {
        this.cause = cause;
    }

    @Override
    public String getShortDescription() {
        return this.cause;
    }
}
