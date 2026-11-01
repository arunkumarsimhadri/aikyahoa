package com.aikya.common;

public enum Role {
    SUPER_ADMIN,
    PRESIDENT,
    SECRETARY,
    TREASURER,
    COMMITTEE_MEMBER,
    OWNER,
    TENANT,
    SECURITY_STAFF,
    TECHNICIAN;

    public boolean isManagingCommittee() {
        return this == SUPER_ADMIN || this == PRESIDENT || this == SECRETARY
                || this == TREASURER || this == COMMITTEE_MEMBER;
    }

    public boolean canReconcilePayments() {
        return this == SUPER_ADMIN || this == TREASURER || this == PRESIDENT;
    }

    public boolean canVoteInOwnerPoll() {
        return this == OWNER || this == PRESIDENT || this == SECRETARY
                || this == TREASURER || this == COMMITTEE_MEMBER;
    }
}

