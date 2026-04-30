package com.duke.auth.event;

import org.springframework.context.ApplicationEvent;

public class ApiSyncCompletedEvent extends ApplicationEvent {
    public ApiSyncCompletedEvent(Object source) {
        super(source);
    }
}
