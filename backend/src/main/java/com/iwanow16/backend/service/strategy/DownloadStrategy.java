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
     * Скачать контент с временным диапазоном.
     * @param url URL контента
     * @param outputDir Директория для сохранения файла
     * @param formatId ID формата
     * @param taskId ID задачи
     * @param startTime Время начала в секундах
     * @param endTime Время конца в секундах
     * @return Путь к скачанному файлу
     * @throws Exception Если скачивание не удалось
     */
    Path downloadTimeRange(String url, Path outputDir, String formatId, String taskId, 
                          String startTime, String endTime) throws Exception;

    /**
     * Извлечь кадр из видео в PNG формате.
     * @param url URL видео
     * @param outputDir Директория для сохранения кадра
     * @param taskId ID задачи
     * @param frameTime Время кадра в секундах
     * @return Путь к извлеченному кадру (PNG)
     * @throws Exception Если извлечение не удалось
     */
    Path extractFrame(String url, Path outputDir, String taskId, String frameTime) throws Exception;

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
