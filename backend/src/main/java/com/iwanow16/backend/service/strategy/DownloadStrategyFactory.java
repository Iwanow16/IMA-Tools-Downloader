package com.iwanow16.backend.service.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Фабрика для выбора подходящей стратегии скачивания по URL.
 */
@Service
public class DownloadStrategyFactory {
    private static final Logger log = LoggerFactory.getLogger(DownloadStrategyFactory.class);

    @Autowired
    private List<DownloadStrategy> strategies;

    /**
     * Получить стратегию скачивания для данного URL.
     * @param url URL контента
     * @return Подходящая DownloadStrategy
     * @throws IllegalArgumentException Если не найдена подходящая стратегия
     */
    public DownloadStrategy getStrategy(String url) {
        for (DownloadStrategy strategy : strategies) {
            if (strategy.supports(url)) {
                log.debug("Selected strategy: {} for URL: {}", strategy.getServiceName(), url);
                return strategy;
            }
        }
        throw new IllegalArgumentException("No download strategy found for URL: " + url);
    }

    /**
     * Проверить, поддерживается ли URL.
     * @param url URL для проверки
     * @return true, если есть подходящая стратегия
     */
    public boolean isSupported(String url) {
        return strategies.stream().anyMatch(s -> s.supports(url));
    }

    /**
     * Получить список всех поддерживаемых сервисов.
     * @return Список имен сервисов
     */
    public List<String> getSupportedServices() {
        return strategies.stream()
                .map(DownloadStrategy::getServiceName)
                .toList();
    }
}
