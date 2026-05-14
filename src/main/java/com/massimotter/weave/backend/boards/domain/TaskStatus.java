package com.massimotter.weave.backend.boards.domain;

public enum TaskStatus {
    OPEN("open"),
    BLOCKED("blocked"),
    COMPLETED("completed"),
    ARCHIVED("archived");

    private final String contractName;

    TaskStatus(String contractName) {
        this.contractName = contractName;
    }

    public String contractName() {
        return contractName;
    }
}
