package com.iwanow16.backend.service.strategy;

import java.nio.file.Path;

/**
 * Интерфейс для стратегии скачивания контента с разных сервисов.
 * Каждый сервис (YouTube, etc.) имеет свою реализацию.
 */
public interface DownloadStrategy {
    /**
     * Скачать контент с указанного URL в указанную директорию.
     * @param url URL контента
     * @param outputDir Директория для сохранения файла
     * @param formatId ID формата (может быть null для лучшего качества)
     * @param taskId ID задачи для отслеживания прогресса
     * @return Путь к скачанному файлу
     * @throws Exception Если скачивание не удалось
     */
    Path download(String url, Path outputDir, String formatId, String taskId) throws Exception;

    /**
     * Проверить, поддерживает ли эта стратегия данный URL.
     * @param url URL для проверки
     * @return true, если URL поддерживается
     */
    boolean supports(String url);

    /**
     * Получить имя сервиса.
     * @return Имя сервиса (e.g., "youtube", "tiktok")
     */
    String getServiceName();
}
