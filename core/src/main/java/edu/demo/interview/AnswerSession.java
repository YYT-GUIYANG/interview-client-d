package edu.demo.interview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Single-thread-owned state. Call on the coordinator thread; post snapshots to the UI. */
public final class AnswerSession {
    public enum Status { RECORDING, FINISHING, COMPLETE, PARTIAL, CANCELLED }
    private static final class Segment {
        final String id;
        long revision;
        String text;
        boolean isFinal;
        long sampleStart;
        Segment(RecognitionEvent event) {
            id = event.segmentId;
            update(event);
        }
        void update(RecognitionEvent event) {
            revision = event.revision;
            text = event.text;
            isFinal = event.isFinal;
            sampleStart = event.sampleStart;
        }
    }

    private final String answerId;
    private final List<KeywordMatcher.Keyword> keywords;
    private final Map<String, Segment> segments = new HashMap<>();
    private Status status = Status.RECORDING;

    public AnswerSession(String answerId, List<KeywordMatcher.Keyword> keywords) {
        if (answerId == null || answerId.isEmpty() || keywords == null)
            throw new IllegalArgumentException("invalid answer");
        this.answerId = answerId;
        this.keywords = new ArrayList<>(keywords); // selected question snapshot
    }

    /** A higher-revision final may correct a final; a partial may never reopen it. */
    public boolean apply(RecognitionEvent event) {
        if (!answerId.equals(event.answerId) || status == Status.CANCELLED
                || status == Status.COMPLETE || status == Status.PARTIAL) return false;
        Segment old = segments.get(event.segmentId);
        if (old != null && (event.revision <= old.revision || (old.isFinal && !event.isFinal))) return false;
        if (old == null) segments.put(event.segmentId, new Segment(event));
        else old.update(event);
        return true;
    }

    public Status status() { return status; }
    public void finishing() { if (status == Status.RECORDING) status = Status.FINISHING; }
    public void complete() { if (status == Status.FINISHING) status = Status.COMPLETE; }
    public void partial() { if (status == Status.FINISHING || status == Status.RECORDING) status = Status.PARTIAL; }
    public void cancel() { status = Status.CANCELLED; }

    private List<Segment> ordered() {
        List<Segment> result = new ArrayList<>(segments.values());
        result.sort((a, b) -> {
            if (a.sampleStart >= 0 && b.sampleStart >= 0 && a.sampleStart != b.sampleStart)
                return Long.compare(a.sampleStart, b.sampleStart);
            if (a.id.matches("[0-9]+") && b.id.matches("[0-9]+")) {
                try { return Long.compare(Long.parseLong(a.id), Long.parseLong(b.id)); }
                catch (NumberFormatException ignored) { }
            }
            return a.id.compareTo(b.id);
        });
        return result;
    }

    public String finalText() {
        StringBuilder text = new StringBuilder();
        for (Segment s : ordered()) if (s.isFinal) text.append(s.text);
        return text.toString();
    }

    public String displayText() {
        StringBuilder text = new StringBuilder();
        for (Segment s : ordered()) text.append(s.text);
        return text.toString();
    }

    public List<KeywordMatcher.Hit> hits() {
        return KeywordMatcher.match(finalText(), keywords);
    }

    public List<String> segmentIds() {
        List<String> ids = new ArrayList<>();
        for (Segment s : ordered()) ids.add(s.id);
        return Collections.unmodifiableList(ids);
    }
}
