package io.skodjob.testframe.helper;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.util.LinkedList;
import java.util.List;

public class TestLoggerAppender extends AbstractAppender {
    private final List<LogEvent> logEvents = new LinkedList<>();

    public TestLoggerAppender(String name) {
        super(name, null, null, true, null);
    }

    @Override
    public void append(LogEvent event) {
        logEvents.add(event.toImmutable());
    }

    public List<LogEvent> getLogEvents() {
        return new LinkedList<>(logEvents);
    }

    public void clean() {
        logEvents.clear();
    }
}
