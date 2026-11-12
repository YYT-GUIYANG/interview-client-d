package edu.demo.interview.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.os.SystemClock;
import android.content.Context;
import java.util.Arrays;

/** 16 kHz mono PCM16 capture. Android permission must be checked by the caller. */
public final class PcmRecorder {
    public interface Sink {
        void onAudio(short[] samples, int count, long sampleStart, long capturedAtElapsedNanos);
        void onError(String detail);
    }
    private volatile boolean running;
    private Thread worker;
    private AudioRecord recorder;

    @SuppressLint("MissingPermission")
    public synchronized void start(Context context, Sink sink) {
        if (running) throw new IllegalStateException("already recording");
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("录音权限未授予");
        int min = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) throw new IllegalStateException("设备不支持 16 kHz PCM16 录音");
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 6400));
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            recorder.release();
            throw new IllegalStateException("录音设备初始化失败");
        }
        running = true;
        recorder.startRecording();
        worker = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            short[] buffer = new short[1600];
            long sample = 0;
            try {
                while (running) {
                    int n = recorder.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
                    if (n > 0) {
                        long at = SystemClock.elapsedRealtimeNanos();
                        sink.onAudio(Arrays.copyOf(buffer, n), n, sample, at);
                        sample += n;
                    } else if (n < 0) {
                        sink.onError("AudioRecord.read=" + n);
                        break;
                    }
                }
            } finally {
                running = false;
            }
        }, "audio-capture");
        worker.start();
    }

    public synchronized void stop() {
        running = false;
        if (recorder != null) {
            try { recorder.stop(); } catch (IllegalStateException ignored) { }
        }
        if (worker != null && worker != Thread.currentThread()) {
            try { worker.join(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        if (recorder != null) { recorder.release(); recorder = null; }
        worker = null;
    }
}
