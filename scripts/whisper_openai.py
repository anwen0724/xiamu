#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Whisper ASR 脚本（openai-whisper 官方版本）
用于将音频文件转写为文本
"""

import sys
import os
import whisper
import json

def transcribe_audio(audio_path):
    """
    转写音频文件
    :param audio_path: 音频文件路径
    :return: JSON 格式的转写结果
    """
    try:
        # 规范化路径（处理 Windows 路径问题）
        audio_path = os.path.normpath(audio_path)

        # 检查文件是否存在
        if not os.path.exists(audio_path):
            raise FileNotFoundError(f"音频文件不存在: {audio_path}")

        # 加载模型（small 模型）
        model = whisper.load_model("small")

        # 执行转写
        result = model.transcribe(
            audio_path,
            language=None,
            task="transcribe",
            verbose=False,
            fp16=False
        )

        # 构建返回结果
        output = {
            "text": result.get("text", ""),
            "segments": [],
            "language": result.get("language", ""),
            "language_probability": result.get("language_probability", 0.0)
        }

        for i, segment in enumerate(result.get("segments", [])):
            segment_data = {
                "id": i,
                "start": segment.get("start", 0),
                "end": segment.get("end", 0),
                "text": segment.get("text", "")
            }
            output["segments"].append(segment_data)

        # 输出 JSON（添加 indent=2，去掉 ensure_ascii=False）
        print(json.dumps(output, indent=2))

        return output

    except FileNotFoundError as e:
        error = {"error": f"音频文件不存在: {str(e)}"}
        print(json.dumps(error, indent=2))
        sys.exit(1)

    except Exception as e:
        error = {"error": f"转写失败: {str(e)}"}
        print(json.dumps(error, indent=2))
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"error": "请提供音频文件路径"}, indent=2))
        sys.exit(1)

    audio_path = sys.argv[1]
    transcribe_audio(audio_path)
