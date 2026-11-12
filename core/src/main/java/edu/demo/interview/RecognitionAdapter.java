package edu.demo.interview;

/** Implementations own their worker thread. Callbacks may arrive off the Android UI thread. */
public interface RecognitionAdapter {
    interface Listener {
        void onReady();
        void onEvent(RecognitionEvent event);
        void onFinished(boolean complete, String detail);
        void onError(String code, String detail);
    }
    void start(String answerId, Listener listener);
    void feedAudio(short[] pcm16, int count, long sampleStart, long capturedAtElapsedNanos);
    void finish();
    void cancel();
}
