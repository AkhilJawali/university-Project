package com.utms.masterdata.timeslot;

public enum SlotType {
    LECTURE, TUTORIAL, PRACTICAL, BREAK, LUNCH;

    public boolean isSchedulable() {
        return this == LECTURE || this == TUTORIAL || this == PRACTICAL;
    }
}
