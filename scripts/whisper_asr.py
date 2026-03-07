#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Whisper ASR 脚本（faster-whisper 版本，调试用 CPU 模式）
用于将音频文件转写为文本
"""

import sys
import os
import json
from faster_whisper import WhisperModel

def transcribe_audio(audio_path):
    """
    转写音频文件
    :param audio_path: 音频文件路径
    :return: JSON 格式的转写结果
    """
    try:
        # 规范化路径（处理 Windows 路径问题）
        audio_path = os.path.normpath(audio_path)

        print(f"[DEBUG] 处理后的路径: {audio_path}", file=sys.stderr)

        # 检查文件是否存在
        if not os.path.exists(audio_path):
            raise FileNotFoundError(f"音频文件不存在: {audio_path}")

        print(f"[DEBUG] 文件存在，开始加载模型 (CPU 模式)...", file=sys.stderr)

        # 加载模型（small 模型，CPU 模式，用于调试）
        model = WhisperModel(
            'small',
            device='cpu',  # 改为 CPU 模式
            compute_type='int8'  # 改为 int8，减少内存占用
        )

        print(f"[DEBUG] 模型加载成功，开始转写...", file=sys.stderr)

        # 执行转写，添加详细参数
        segments, info = model.transcribe(
            audio_path,
            language=None,
            beam_size=5,
            word_timestamps=False  # 添加这个参数
        )

        print(f"[DEBUG] 转写完成，语言: {info.language}", file=sys.stderr)

        # 构建返回结果
        result = {
            "text": "",
            "segments": [],
            "language": info.language,
            "language_probability": info.language_probability
        }

        for segment in segments:
            segment_data = {
                "id": segment.id,
                "start": segment.start,
                "end": segment.end,
                "text": segment.text
            }
            result["segments"].append(segment_data)
            result["text"] += segment.text

        print(f"[DEBUG] 构建 JSON 完成，文本长度: {len(result['text'])}", file=sys.stderr)

        # 输出 JSON
        print(json.dumps(result, ensure_ascii=False, indent=2))

        return result

    except FileNotFoundError as e:
        error = {"error": f"音频文件不存在: {str(e)}"}
        print(json.dumps(error, ensure_ascii=False))
        sys.exit(1)

    except Exception as e:
        error = {"error": f"转写失败: {str(e)}"}
        print(json.dumps(error, ensure_ascii=False))
        print(f"[ERROR] 异常详情: {type(e).__name__}: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"error": "请提供音频文件路径"}, ensure_ascii=False))
        sys.exit(1)

    audio_path = sys.argv[1]
    print(f"[DEBUG] 开始处理，参数: {audio_path}", file=sys.stderr)
    transcribe_audio(audio_path)
    print(f"[DEBUG] 处理完成", file=sys.stderr)
