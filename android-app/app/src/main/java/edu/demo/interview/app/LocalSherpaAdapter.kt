package edu.demo.interview.app

import android.content.Context
import android.content.res.AssetManager
import android.os.SystemClock
import android.util.Log
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.getEndpointConfig
import com.k2fsa.sherpa.onnx.getFeatureConfig
import edu.demo.interview.RecognitionAdapter
import edu.demo.interview.RecognitionEvent
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real local streaming ASR via sherpa-onnx JNI.
 * Uses Chinese 14M zipformer int8 encoder/joiner (type 9 paths).
 * DemoAdapter must not be treated as recognition evidence.
 */
class LocalSherpaAdapter(private val context: Context) : RecognitionAdapter {
    private data class AudioChunk(
        val samples: FloatArray,
        val sampleStart: Long,
        val capturedAt: Long,
        val finish: Boolean = false,
        val cancel: Boolean = false
    )

    private val queue = ArrayBlockingQueue<AudioChunk>(64)
    private val started = AtomicBoolean(false)
    private val terminal = AtomicBoolean(false)
    @Volatile private var listener: RecognitionAdapter.Listener? = null
    @Volatile private var answerId: String = ""
    private var worker: Thread? = null

    override fun start(answerId: String, listener: RecognitionAdapter.Listener) {
        if (!started.compareAndSet(false, true)) {
            listener.onError("ALREADY_STARTED", "本地识别已启动")
            return
        }
        this.answerId = answerId
        this.listener = listener
        terminal.set(false)
        queue.clear()
        worker = Thread({ runEngine() }, "local-sherpa").also { it.start() }
    }

    override fun feedAudio(
        pcm16: ShortArray,
        count: Int,
        sampleStart: Long,
        capturedAtElapsedNanos: Long
    ) {
        if (!started.get() || terminal.get()) return
        if (count <= 0 || count > pcm16.size) throw IllegalArgumentException("sample count")
        val floats = FloatArray(count)
        for (i in 0 until count) floats[i] = pcm16[i] / 32768.0f
        if (!queue.offer(AudioChunk(floats, sampleStart, capturedAtElapsedNanos))) {
            listener?.onError("QUEUE_FULL", "本地识别队列已满")
            finishInternal(complete = false, detail = "本地队列满，回答未完成")
        }
    }

    override fun finish() {
        queue.offer(AudioChunk(FloatArray(0), -1, SystemClock.elapsedRealtimeNanos(), finish = true))
    }

    override fun cancel() {
        queue.offer(AudioChunk(FloatArray(0), -1, SystemClock.elapsedRealtimeNanos(), cancel = true))
    }

    private fun runEngine() {
        var recognizer: OnlineRecognizer? = null
        var stream: OnlineStream? = null
        try {
            if (!modelAssetsPresent(context.assets)) {
                listener?.onError(
                    "MODEL_MISSING",
                    "缺少模型 assets：请放入 sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23/"
                )
                finishInternal(false, "模型文件缺失")
                return
            }
            val config = OnlineRecognizerConfig(
                featConfig = getFeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = zh14MConfig(),
                endpointConfig = getEndpointConfig(),
                enableEndpoint = true,
            )
            recognizer = OnlineRecognizer(assetManager = context.assets, config = config)
            stream = recognizer.createStream()
            listener?.onReady()

            var segmentIndex = 0
            var revision = 0L
            var segmentSampleStart = 0L
            var lastText = ""

            while (!terminal.get()) {
                val chunk = queue.poll(200, TimeUnit.MILLISECONDS) ?: continue
                if (chunk.cancel) {
                    finishInternal(false, "已取消")
                    break
                }
                if (chunk.finish) {
                    stream.inputFinished()
                    while (recognizer.isReady(stream)) {
                        recognizer.decode(stream)
                    }
                    val result = recognizer.getResult(stream)
                    val text = result.text.trim()
                    if (text.isNotEmpty()) {
                        revision += 1
                        listener?.onEvent(
                            RecognitionEvent(
                                answerId,
                                segmentIndex.toString(),
                                revision,
                                text,
                                true,
                                segmentSampleStart,
                                -1,
                                SystemClock.elapsedRealtimeNanos()
                            )
                        )
                    }
                    finishInternal(text.isNotEmpty() || lastText.isNotEmpty(), "本地识别收尾完成")
                    break
                }

                if (segmentSampleStart < 0) segmentSampleStart = chunk.sampleStart
                if (chunk.sampleStart >= 0 && lastText.isEmpty()) segmentSampleStart = chunk.sampleStart
                stream.acceptWaveform(chunk.samples, 16000)
                while (recognizer.isReady(stream)) {
                    recognizer.decode(stream)
                }
                val result = recognizer.getResult(stream)
                val text = result.text.trim()
                if (text.isNotEmpty() && text != lastText) {
                    revision += 1
                    lastText = text
                    listener?.onEvent(
                        RecognitionEvent(
                            answerId,
                            segmentIndex.toString(),
                            revision,
                            text,
                            false,
                            segmentSampleStart,
                            chunk.sampleStart + chunk.samples.size,
                            chunk.capturedAt
                        )
                    )
                }
                if (recognizer.isEndpoint(stream)) {
                    if (lastText.isNotEmpty()) {
                        revision += 1
                        listener?.onEvent(
                            RecognitionEvent(
                                answerId,
                                segmentIndex.toString(),
                                revision,
                                lastText,
                                true,
                                segmentSampleStart,
                                chunk.sampleStart + chunk.samples.size,
                                SystemClock.elapsedRealtimeNanos()
                            )
                        )
                    }
                    recognizer.reset(stream)
                    segmentIndex += 1
                    revision = 0
                    lastText = ""
                    segmentSampleStart = -1
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "local sherpa failed", e)
            listener?.onError("LOCAL_ENGINE", e.message ?: e.javaClass.simpleName)
            finishInternal(false, "本地引擎异常：" + (e.message ?: e.javaClass.simpleName))
        } finally {
            try { stream?.release() } catch (_: Throwable) {}
            try { recognizer?.release() } catch (_: Throwable) {}
            started.set(false)
        }
    }

    private fun finishInternal(complete: Boolean, detail: String) {
        if (!terminal.compareAndSet(false, true)) return
        listener?.onFinished(complete, detail)
    }

    companion object {
        private const val TAG = "LocalSherpaAdapter"
        private const val MODEL_DIR = "sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23"

        fun modelAssetsPresent(assets: AssetManager): Boolean {
            val needed = listOf(
                "$MODEL_DIR/encoder-epoch-99-avg-1.int8.onnx",
                "$MODEL_DIR/decoder-epoch-99-avg-1.onnx",
                "$MODEL_DIR/joiner-epoch-99-avg-1.int8.onnx",
                "$MODEL_DIR/tokens.txt"
            )
            return needed.all { path ->
                try {
                    assets.open(path).close()
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }

        fun zh14MConfig(): OnlineModelConfig {
            return OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = "$MODEL_DIR/encoder-epoch-99-avg-1.int8.onnx",
                    decoder = "$MODEL_DIR/decoder-epoch-99-avg-1.onnx",
                    joiner = "$MODEL_DIR/joiner-epoch-99-avg-1.int8.onnx",
                ),
                tokens = "$MODEL_DIR/tokens.txt",
                modelType = "zipformer",
                numThreads = 2,
                provider = "cpu",
            )
        }
    }
}
