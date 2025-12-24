import React, { createContext, useState, useContext, useCallback } from 'react';
import { downloadAPI } from '../services/api';

const DownloadContext = createContext();

export const useDownload = () => {
  const context = useContext(DownloadContext);
  if (!context) {
    throw new Error('useDownload must be used within DownloadProvider');
  }
  return context;
};

export const DownloadProvider = ({ children }) => {
  const [tasks, setTasks] = useState([]);
  const [videoInfo, setVideoInfo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedFormat, setSelectedFormat] = useState(null);

  // Fetch video info
  const fetchVideoInfo = useCallback(async (url) => {
    setLoading(true);
    setError(null);

    try {
      const info = await downloadAPI.getVideoInfo(url);
      setVideoInfo(info);
      setSelectedFormat(info.formats?.[0] || null);
      return info;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  // Start download
  const startDownload = useCallback(async (url, formatId, quality) => {
    try {
      const response = await downloadAPI.startDownload(url, formatId, quality);

      const newTask = {
        id: response.id || response.taskId,
        url,
        formatId,
        quality,
        status: 'pending',
        progress: 0,
        filename: null,
        title: videoInfo?.title || 'Video', // Добавляем title видео
        error: null,
        createdAt: new Date().toISOString(),
        estimatedTime: null,
        downloadSpeed: null
      };

      setTasks((prev) => [...prev, newTask]);
      return newTask;
    } catch (err) {
      setError(err.message);
      throw err;
    }
  }, [videoInfo]);

  // Update task
  const updateTask = useCallback((taskId, updates) => {
    setTasks((prev) =>
      prev.map((task) =>
        task.id === taskId ? { ...task, ...updates } : task
      )
    );
  }, []);

  // Cancel task
  const cancelTask = useCallback(
    async (taskId) => {
      try {
        await downloadAPI.cancelTask(taskId);
        updateTask(taskId, { status: 'cancelled' });
      } catch (err) {
        setError(err.message);
        throw err;
      }
    },
    [updateTask]
  );

  // Remove task
  const removeTask = useCallback((taskId) => {
    setTasks((prev) => prev.filter((task) => task.id !== taskId));
  }, []);

  // Clear completed tasks
  const clearCompleted = useCallback(() => {
    setTasks((prev) =>
      prev.filter(
        (task) => task.status !== 'completed' && task.status !== 'failed'
      )
    );
  }, []);

  // Refresh tasks from server
  const refreshTasks = useCallback(async () => {
    try {
      const serverTasks = await downloadAPI.getTasks();
      
      // Ensure serverTasks is always an array
      if (!Array.isArray(serverTasks)) {
        console.warn('Invalid serverTasks format:', serverTasks);
        return;
      }

      setTasks((prev) => {
        const updated = prev.map(task => ({ ...task })); // Копируем текущие задачи

        serverTasks.forEach((serverTask) => {
          const taskId = serverTask.id || serverTask.taskId;
          const idx = updated.findIndex((t) => t.id === taskId);

          if (idx >= 0) {
            // Обновляем только незавершенные задачи с информацией с сервера
            // Для завершенных задач используем локальное состояние
            if (updated[idx].status === 'completed' || updated[idx].status === 'failed' || updated[idx].status === 'cancelled') {
              // Не перезаписываем статус для завершенных задач
              updated[idx] = { 
                ...updated[idx], 
                ...serverTask, 
                id: taskId,
                status: updated[idx].status // Сохраняем локальный статус
              };
            } else {
              // Для активных задач обновляем всю информацию
              updated[idx] = { ...updated[idx], ...serverTask, id: taskId };
            }
          } else {
            // Новая задача от сервера - добавляем только если не завершена
            if (serverTask.status !== 'completed' && serverTask.status !== 'failed' && serverTask.status !== 'cancelled') {
              updated.push({ ...serverTask, id: taskId });
            }
          }
        });

        return updated;
      });
    } catch (err) {
      console.error('Failed to refresh tasks:', err.message);
      // Don't propagate error, just log it
    }
  }, []);

  const value = {
    tasks,
    videoInfo,
    loading,
    error,
    selectedFormat,
    setSelectedFormat,
    fetchVideoInfo,
    startDownload,
    updateTask,
    cancelTask,
    removeTask,
    clearCompleted,
    refreshTasks,
    setError
  };

  return (
    <DownloadContext.Provider value={value}>
      {children}
    </DownloadContext.Provider>
  );
};
