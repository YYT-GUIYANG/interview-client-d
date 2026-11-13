#!/usr/bin/env python3
"""Exercise the sample control/audio/Done protocol on loopback (no ASR)."""

import asyncio
import json
import struct

import websockets

from minimal_wss_adapter import handle


async def check():
    async with websockets.serve(handle, "127.0.0.1", 0) as server:
        port = server.sockets[0].getsockname()[1]
        async with websockets.connect(f"ws://127.0.0.1:{port}") as ws:
            await ws.send(json.dumps({"answer_id": "check-1"}))
            await ws.send(struct.pack("<2f", 0.5, -0.5))
            interim = json.loads(await ws.recv())
            assert interim["answer_id"] == "check-1"
            assert interim["sample_end"] == 2 and not interim["is_final"]
            await ws.send("Done")
            final = json.loads(await ws.recv())
            complete = json.loads(await ws.recv())
            assert final["answer_id"] == "check-1" and final["is_final"]
            assert complete == {"answer_complete": True, "answer_id": "check-1"}

        async with websockets.connect(f"ws://127.0.0.1:{port}") as ws:
            await ws.send(struct.pack("<f", 0.1))
            error = json.loads(await ws.recv())
            assert error["error_code"] == "MISSING_ANSWER_ID"
            assert error["answer_complete"] is False


if __name__ == "__main__":
    asyncio.run(check())
    print("Protocol sample check: passed (loopback fixture; no ASR)")
