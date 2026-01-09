package com.iwanow16.backend.util;

import com.iwanow16.backend.model.dto.FormatDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Утилита для обогащения списка форматов синтетическими вариантами video+audio.
 * Если платформа предоставляет только видео или только аудио форматы,
 * добавляет комбинированные варианты для удобства пользователя.
 */
public class FormatEnhancer {
    private static final Logger log = LoggerFactory.getLogger(FormatEnhancer.class);

    /**
     * Обогатить список форматов синтетическими video+audio вариантами.
     * 
     * Примеры:
     * - YouTube: часто есть видео без звука + отдельное аудио → создаём комбо
     * - Bilibili: видео и аудио приходят отдельно → создаём комбо форматы
     *
     * @param formats исходный список форматов от yt-dlp
     * @param serviceName название платформы (youtube, bilibili, etc)
     * @return обогащённый список с дополнительными синтетическими форматами
     */
    public static List<FormatDto> enhanceFormats(List<FormatDto> formats, String serviceName) {
        return enhanceFormats(formats, serviceName, true);
    }

    /**
     * Обогатить список форматов синтетическими video+audio вариантами.
     * 
     * @param formats исходный список форматов от yt-dlp
     * @param serviceName название платформы (youtube, bilibili, etc)
     * @param mergeAudio нужно ли создавать синтетические video+audio форматы
     * @return обогащённый список с дополнительными синтетическими форматами
     */
    public static List<FormatDto> enhanceFormats(List<FormatDto> formats, String serviceName, boolean mergeAudio) {
        if (formats == null || formats.isEmpty()) {
            log.debug("🔚 enhanceFormats called with null/empty formats");
            return formats;
        }

        log.debug("🎬 enhanceFormats START | Service: {} | Input formats: {}", serviceName, formats.size());
        formats.forEach(f -> log.debug("  - Input format: {} | vcodec: {} | acodec: {} | quality: {}", 
            f.getFormatId(), f.getVcodec(), f.getAcodec(), f.getQuality()));

        List<FormatDto> enhanced = new ArrayList<>(formats);
        
        // Разделяем форматы по типам
        List<FormatDto> videoFormats = formats.stream()
                .filter(f -> f.getVcodec() != null && !f.getVcodec().isEmpty() && 
                           (f.getAcodec() == null || f.getAcodec().isEmpty() || "none".equals(f.getAcodec())))
                .collect(Collectors.toList());

        List<FormatDto> audioFormats = formats.stream()
                .filter(f -> f.getAcodec() != null && !f.getAcodec().isEmpty() && 
                           (f.getVcodec() == null || f.getVcodec().isEmpty() || "none".equals(f.getVcodec())))
                .collect(Collectors.toList());

        List<FormatDto> combinedFormats = formats.stream()
                .filter(f -> f.getVcodec() != null && !f.getVcodec().isEmpty() && 
                           f.getAcodec() != null && !f.getAcodec().isEmpty() && 
                           !"none".equals(f.getVcodec()) && !"none".equals(f.getAcodec()))
                .collect(Collectors.toList());

        log.debug("📊 Format analysis | Service: {} | Video-only: {} | Audio-only: {} | Combined: {} | Merge: {}", 
                serviceName, videoFormats.size(), audioFormats.size(), combinedFormats.size(), mergeAudio);

        // Если есть разделённые форматы (видео и аудио отдельно) и нужно их объединять, создаём комбинированные
        if (mergeAudio && !videoFormats.isEmpty() && !audioFormats.isEmpty()) {
            log.info("🔀 Creating synthetic video+audio format combinations | Service: {}", serviceName);
            createCombinedFormats(enhanced, videoFormats, audioFormats);
        }

        log.debug("🔚 FormatEnhancer returning {} formats", enhanced.size());
        return enhanced;
    }

    /**
     * Создать синтетические форматы video+audio из отдельных видео и аудио.
     */
    private static void createCombinedFormats(List<FormatDto> allFormats, 
                                             List<FormatDto> videoFormats, 
                                             List<FormatDto> audioFormats) {
        // Выбираем лучшее видео для каждого качества
        Map<String, FormatDto> bestVideoByQuality = new LinkedHashMap<>();
        
        for (FormatDto video : videoFormats) {
            String quality = video.getQuality();
            if (quality == null) continue;
            
            // Используем всё лучшее видео для каждого качества
            if (!bestVideoByQuality.containsKey(quality)) {
                bestVideoByQuality.put(quality, video);
            }
        }

        // Выбираем лучший аудио (самый высокий битрейт)
        FormatDto bestAudio = audioFormats.stream()
                .max(Comparator.comparing(f -> f.getFilesize() > 0 ? f.getFilesize() : 0))
                .orElse(null);

        if (bestAudio == null && !audioFormats.isEmpty()) {
            bestAudio = audioFormats.get(0);
        }

        // Создаём комбинированные форматы
        int syntheticCount = 0;
        for (Map.Entry<String, FormatDto> entry : bestVideoByQuality.entrySet()) {
            String quality = entry.getKey();
            FormatDto video = entry.getValue();

            if (bestAudio == null) continue;

            // Создаём синтетический формат: video_id+audio_id
            FormatDto combined = new FormatDto();
            combined.setFormatId(video.getFormatId() + "+" + bestAudio.getFormatId());
            combined.setQuality(quality + " + Audio");
            combined.setExt("mp4");
            combined.setVcodec(video.getVcodec());
            combined.setAcodec(bestAudio.getAcodec());
            
            // Размер файла - сумма видео и аудио (приблизительно)
            combined.setFilesize(video.getFilesize() + bestAudio.getFilesize());
            
            // Note с информацией о синтетическом формате
            combined.setNote("Video + Audio combined");
            combined.setResolution(video.getResolution());

            // Добавляем в начало списка, чтобы был более видимым
            allFormats.add(0, combined);
            syntheticCount++;

            log.debug("✨ Created synthetic format: {} → {} + {}", 
                    combined.getFormatId(), video.getFormatId(), bestAudio.getFormatId());
        }

        log.info("✨ Created {} synthetic video+audio formats", syntheticCount);
    }
}
