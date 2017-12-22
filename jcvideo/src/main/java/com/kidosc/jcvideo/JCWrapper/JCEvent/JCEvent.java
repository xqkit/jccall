package com.kidosc.jcvideo.JCWrapper.JCEvent;

public class JCEvent {

    public enum EventType {
        Exit,
        LOGIN,
        LOGOUT,
        CLIENT_STATE_CHANGE,
        CALL_ADD,
        CALL_UPDATE,
        CALL_REMOVE,
        CALL_UI,
        CONFERENCE_JOIN,
        CONFERENCE_LEAVE,
        CONFERENCE_QUERY,
        CONFERENCE_PARTP_JOIN,
        CONFERENCE_PARTP_LEAVE,
        CONFERENCE_PARTP_UPDATE,
        CONFERENCE_PROP_CHANGE,
        CONFERENCE_MESSAGE_RECEIVED,
        MESSAGE,
    }

    private EventType mEventType;

    public JCEvent(EventType eventType) {
        this.mEventType = eventType;
    }

    public EventType getEventType() {
        return mEventType;
    }

}

