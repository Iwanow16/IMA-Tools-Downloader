package com.iwanow16.backend.extractor;

import com.iwanow16.backend.model.dto.VideoInfoDto;
import com.iwanow16.backend.service.strategy.DownloadStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Сервис для извлечения информации о видео с разных ресурсов.
 * Использует подходящий экстрактор на основе URL.
 */
@Service
public class VideoExtractorService {
    private static final Logger log = LoggerFactory.getLogger(VideoExtractorService.class);

    @Autowired
    private List<VideoExtractor> extractors;

    @Autowired
    private DownloadStrategyFactory strategyFactory;

    public VideoExtractorService() {
    }

    @PostConstruct
    public void init() {
        log.info("VideoExtractorService initialized with {} extractors", 
                extractors != null ? extractors.size() : 0);
    }

    /**
     * Извлечь информацию о видео с использованием подходящего экстрактора.
     * @param url URL видео
     * @return Информация о видео
     * @throws Exception Если извлечение не удалось
     * @throws IllegalArgumentException Если не найден подходящий экстрактор
     */
    public VideoInfoDto extractInfo(String url) throws Exception {
        log.info("Extracting info for URL: {}", url);

        // Найти экстрактор для данного URL
        for (VideoExtractor extractor : extractors) {
            if (extractor.supports(url)) {
                log.debug("Using extractor: {} for URL: {}", extractor.getServiceName(), url);
                return extractor.extractInfo(url);
            }
        }

        // Если ничего не нашли, выбросить исключение
        throw new IllegalArgumentException("No extractor found for URL. Please check if the URL is valid and belongs to a supported service.");
    }

    /**
     * Получить список поддерживаемых сервисов.
     * @return Список имен сервисов
     */
    public List<String> getSupportedServices() {
        return extractors.stream()
                .map(VideoExtractor::getServiceName)
                .distinct()
                .toList();
    }
}
