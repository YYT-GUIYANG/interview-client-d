#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DL="$ROOT/downloads"
JNI="$ROOT/android-app/app/src/main/jniLibs"
ASSETS="$ROOT/android-app/app/src/main/assets"
mkdir -p "$DL" "$JNI" "$ASSETS"

ANDROID_TAR="$DL/sherpa-onnx-v2.0.1-android.tar.bz2"
MODEL_TAR="$DL/sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23.tar.bz2"

if [[ ! -f "$ANDROID_TAR" ]]; then
  curl -L --retry 5 -o "$ANDROID_TAR" \
    https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.1/sherpa-onnx-v2.0.1-android.tar.bz2
fi
if [[ ! -f "$MODEL_TAR" ]]; then
  curl -L --retry 5 -o "$MODEL_TAR" \
    https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23.tar.bz2
fi

sha256sum "$ANDROID_TAR" "$MODEL_TAR" | tee "$DL/checksums.sha256"
rm -rf "$DL/android-extract"
mkdir -p "$DL/android-extract"
tar -xjf "$ANDROID_TAR" -C "$DL/android-extract"
rm -rf "$JNI"/*
found=0
while IFS= read -r so; do
  abi="$(basename "$(dirname "$so")")"
  case "$abi" in
    arm64-v8a|armeabi-v7a|x86_64|x86)
      mkdir -p "$JNI/$abi"
      cp -a "$so" "$JNI/$abi/"
      found=1
      ;;
  esac
done < <(find "$DL/android-extract" -type f -name 'lib*.so')
test "$found" -eq 1

tar -xjf "$MODEL_TAR" -C "$ASSETS"
MODEL="$ASSETS/sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23"
rm -rf "$MODEL/test_wavs" || true
test -f "$MODEL/encoder-epoch-99-avg-1.int8.onnx"
test -f "$MODEL/decoder-epoch-99-avg-1.onnx"
test -f "$MODEL/joiner-epoch-99-avg-1.int8.onnx"
test -f "$MODEL/tokens.txt"
sha256sum \
  "$MODEL/encoder-epoch-99-avg-1.int8.onnx" \
  "$MODEL/decoder-epoch-99-avg-1.onnx" \
  "$MODEL/joiner-epoch-99-avg-1.int8.onnx" \
  "$MODEL/tokens.txt" | tee "$DL/model-required.sha256"
echo "Native assets ready under jniLibs/ and assets/"
