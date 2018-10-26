package com.sequenceiq.cloudbreak.api.model;

public enum EndpointRuleAction {
    PERMIT("permit"),
    DENY("deny");

    private final String text;

    EndpointRuleAction(String value) {
        text = value;
    }

    public String getText() {
        return text;
    }
}
