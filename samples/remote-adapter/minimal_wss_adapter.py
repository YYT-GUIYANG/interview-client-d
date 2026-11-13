#!/usr/bin/env python3
"""Minimal WSS adapter sample for D↔B protocol alignment.

Upstream audio: 16 kHz mono float32 little-endian frames (NO WAV header).
Client ends input with text frame: Done

Official sherpa streaming_server replies with {"text","segment"} only.
This adapter wraps that style into D/C contract fields and emits answer_complete.

Pending confirmation with B:
- auth token header / query
- TLS certificate trust on device
- whether answer_id is assigned by client or server
- exact field names after C freezes the schema
"""

from __future__ import annotations

import argparse
import asyncio
import json

try:
    import websockets
except ImportError as exc:  # pragma: no cover
    raise SystemExit("pip install websockets") from exc


async def handle(ws):
    answer_id = None
    segment = 0
    revision = 0
    samples = 0
    async for message in ws:
        if isinstance(message, bytes):
            if answer_id is None:
                await ws.send(json.dumps({"error_code": "MISSING_ANSWER_ID", "answer_complete": False}))
                await ws.close(code=1008, reason="answer_id required")
                return
            # float32 little-endian mono @ 16 kHz
            if len(message) % 4 != 0:
                await ws.send(json.dumps({"error": "BAD_AUDIO_ALIGNMENT"}))
                continue
            n = len(message) // 4
            samples += n
            # Placeholder: echo cumulative sample count as temporary text.
            # Replace with B's real ASR service.
            revision += 1
            await ws.send(
                json.dumps(
                    {
                        "answer_id": answer_id,
                        "segment_id": str(segment),
                        "revision": revision,
                        "text": f"已收音频样本 {samples}",
                        "is_final": False,
                        "sample_start": samples - n,
                        "sample_end": samples,
                    },
                    ensure_ascii=False,
                )
            )
        else:
            text = message.strip()
            if text == "Done":
                if answer_id is None:
                    await ws.send(json.dumps({"error_code": "MISSING_ANSWER_ID", "answer_complete": False}))
                    return
                revision += 1
                await ws.send(
                    json.dumps(
                        {
                            "answer_id": answer_id,
                            "segment_id": str(segment),
                            "revision": revision,
                            "text": "远程路径联调样例最终文本。",
                            "is_final": True,
                            "sample_start": 0,
                            "sample_end": samples,
                        },
                        ensure_ascii=False,
                    )
                )
                await ws.send(json.dumps({"answer_complete": True, "answer_id": answer_id}))
                await ws.close(code=1000, reason="complete")
                return
            elif text.startswith("{"):
                # Required first control frame: {"answer_id":"..."}
                try:
                    data = json.loads(text)
                    value = data["answer_id"]
                    if answer_id is not None or not isinstance(value, str) or not 1 <= len(value) <= 128:
                        raise ValueError("invalid answer_id")
                    answer_id = value
                except (ValueError, KeyError, TypeError):
                    await ws.send(json.dumps({"error_code": "BAD_CONTROL", "answer_complete": False}))
                    await ws.close(code=1008, reason="bad control")
                    return
            else:
                await ws.send(json.dumps({"error": "UNKNOWN_TEXT", "text": text}))


async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=6006)
    args = parser.parse_args()
    # For local lab only. Production must use wss:// with trusted certs.
    async with websockets.serve(handle, args.host, args.port, max_size=8 * 1024 * 1024):
        print(f"ws://{args.host}:{args.port}  (device path still requires wss:// via reverse proxy)")
        await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())
