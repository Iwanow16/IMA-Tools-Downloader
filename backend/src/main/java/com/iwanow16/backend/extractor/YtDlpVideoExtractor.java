package com.iwanow16.backend.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwanow16.backend.model.dto.VideoInfoDto;
import com.iwanow16.backend.model.dto.FormatDto;
import com.iwanow16.backend.util.ProcessExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class YtDlpVideoExtractor implements VideoExtractor {
    private static final Logger log = LoggerFactory.getLogger(YtDlpVideoExtractor.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public VideoInfoDto extractInfo(String url) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("yt-dlp");
        cmd.add("--dump-json");
        cmd.add(url);

        StringBuilder out = new StringBuilder();
        int rc = ProcessExecutor.run(cmd, 30, out);
        if (rc != 0) {
            throw new RuntimeException("yt-dlp failed with code " + rc + ": " + out.toString());
        }

        String json = out.toString().trim();
        // yt-dlp may emit warnings/info lines before the JSON payload.
        // Find the first line that looks like JSON (starts with '{' or '[').
        String[] lines = json.split("\n");
        String jsonLine = null;
        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith("{") || t.startsWith("[")) {
                jsonLine = t;
                break;
            }
        }
        if (jsonLine == null) {
            throw new RuntimeException("Could not find JSON output from yt-dlp. Output: " + json);
        }
        JsonNode node = mapper.readTree(jsonLine);

        VideoInfoDto info = new VideoInfoDto();
        info.setId(node.path("id").asText());
        info.setUrl(url);
        info.setTitle(node.path("title").asText());
        info.setAuthor(node.path("uploader").asText(null));
        info.setDurationSeconds(node.path("duration").asLong(0));
        info.setFilesize(node.path("filesize").asLong(0));
        info.setThumbnail(node.path("thumbnail").asText(null));

        // Извлечение форматов
        List<FormatDto> formats = new ArrayList<>();
        JsonNode formatsNode = node.path("formats");
        if (formatsNode.isArray()) {
            for (JsonNode f : formatsNode) {
                FormatDto format = new FormatDto();
                format.setFormatId(f.path("format_id").asText());
                format.setExt(f.path("ext").asText());
                String formatNote = f.path("format_note").asText("");
                format.setNote(formatNote);
                format.setResolution(f.path("resolution").asText(""));
                format.setAcodec(f.path("acodec").asText(""));
                format.setVcodec(f.path("vcodec").asText(""));
                long size = f.path("filesize").asLong(0);
                if (size == 0) {
                    size = f.path("filesize_approx").asLong(0);
                }
                format.setFilesize(size);

                // prefer quality from format_note, fallback to resolution
                String quality = formatNote;
                if (quality == null || quality.isBlank()) {
                    quality = f.path("resolution").asText("");
                }
                format.setQuality(quality);
                formats.add(format);
            }
        }
        info.setFormats(formats);
        return info;
    }

    @Override
    public String getServiceName() {
        return "youtube";
    }
}
