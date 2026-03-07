package com.anwen.xiamubackend.strategy.impl;

import com.anwen.xiamubackend.strategy.TranscriptionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * 本地模型转录策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalModelStrategy implements TranscriptionStrategy {

    @Value("${xiamu.whisper.python-path:python}")
    private String pythonPath;

    @Value("${xiamu.whisper.script-path:scripts/whisper_openai.py}")
    private String scriptPath;

    @Override
    public String transcribe(String audioPath) throws Exception {
        log.info("使用本地模型转写音频: {}", audioPath);

        // 检查脚本是否存在
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            throw new Exception("Whisper 脚本不存在: " + scriptPath);
        }

        // 检查音频文件是否存在
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            throw new Exception("音频文件不存在: " + audioPath);
        }

        // 构建命令
        ProcessBuilder processBuilder = new ProcessBuilder(
            pythonPath,
            scriptPath,
            audioPath
        );

        processBuilder.redirectErrorStream(false);

        log.info("执行 Whisper 脚本: {} {} {}", pythonPath, scriptPath, audioPath);

        Process process = processBuilder.start();

        // 读取标准输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // 等待进程结束
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            // 读取错误信息
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }
            throw new Exception("Python 脚本执行失败，退出码: " + exitCode + ", 错误信息: " + errorOutput);
        }

        // 从输出中提取 JSON（找到以 { 开头且以 } 结尾的完整 JSON）
        String fullOutput = output.toString();
        String jsonOutput = extractJson(fullOutput);

        log.info("本地模型转写成功，JSON 长度: {}, 文本长度: {}", jsonOutput.length(), getTextLength(jsonOutput));
        return jsonOutput;
    }

    /**
     * 从输出中提取 JSON 字符串
     * @param output 完整输出
     * @return JSON 字符串
     */
    private String extractJson(String output) {
        // 找到第一个 { 的位置
        int start = output.indexOf("{");
        if (start == -1) {
            return output;
        }

        // 找到最后一个完整的 JSON 对象（以 } 结束且前面有 "text" 字段）
        int end = output.lastIndexOf("}");

        // 从最后往前找对应的 {，确保是完整的 JSON
        while (end > start) {
            // 查找 text 字段的结束引号
            int textEnd = output.lastIndexOf("\"text\": \"", end);
            if (textEnd > start && textEnd < end) {
                // 找到 segments 字段的开始
                int segmentsStart = output.indexOf("\"segments\": [", end);
                if (segmentsStart > start && segmentsStart < end) {
                    // 找到 language 字段的开始
                    int languageStart = output.indexOf("\"language\": \"", end);
                    if (languageStart > start && languageStart < end) {
                        // 找到 language_probability 字段的开始
                        int probStart = output.indexOf("\"language_probability\": ", end);
                        if (probStart > start && probStart < end) {
                            // 找到完整的 JSON 对象
                            end = output.indexOf("}", probStart);
                            if (end > start) {
                                return output.substring(start, end + 1);
                            }
                        }
                    }
                }
            }
            end--; // 往环向前找
        }

        return output.substring(start);
    }

    /**
     * 从 JSON 中提取文本长度
     * @param jsonOutput JSON 字符串
     * @return 文本长度
     */
    private int getTextLength(String jsonOutput) {
        try {
            // 提取 text 字段的值
            int textStart = jsonOutput.indexOf("\"text\": \"");
            if (textStart == -1) {
                return 0;
            }
            int textEnd = jsonOutput.indexOf("\"", textStart + 8); // 跳过 "text": "
            if (textEnd == -1) {
                return 0;
            }
            String textValue = jsonOutput.substring(textStart + 8, textEnd);
            // 移除转义字符
            textValue = textValue.replace("\\n", "").replace("\\\"", "\"").replace("\\\\", "\\");
            return textValue.length();
        } catch (Exception e) {
            log.error("解析文本长度失败", e);
            return 0;
        }
    }
}
