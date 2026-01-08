import React, { useState, useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useDownload } from '../contexts/DownloadContext'
import { downloadAPI } from '../services/api'
import ProgressBar from './ProgressBar'
import '../styles/components/DownloadQueue.css'

import { 
  FaTrash, 
  FaTimes, 
  FaRedo, 
  FaDownload, 
  FaClock,
  FaBolt,
  FaHistory 
} from 'react-icons/fa'

const DownloadQueue = () => {
  const { t } = useTranslation()
  const { 
    tasks, 
    cancelTask, 
    removeTask, 
    clearCompleted,
    refreshTasks 
  } = useDownload()
  
  const pollingIntervalRef = useRef(null)
  const [autoRefresh, setAutoRefresh] = useState(true)

  // Set up polling for task updates
  useEffect(() => {
    if (autoRefresh) {
      const interval = setInterval(() => {
        refreshTasks()
      }, 3000)
      
      pollingIntervalRef.current = interval
      return () => clearInterval(interval)
    } else {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current)
        pollingIntervalRef.current = null
      }
    }
  }, [autoRefresh, refreshTasks])

  // Format time
  const formatTime = (dateString) => {
    const date = new Date(dateString)
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }

  // Format duration
  const formatDuration = (seconds) => {
    if (!seconds) return '--:--'
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const secs = Math.floor(seconds % 60)
    
    if (hours > 0) {
      return `${hours}h ${minutes}m`
    }
    return `${minutes}m ${secs}s`
  }

  // Get status color
  const getStatusColor = (status) => {
    switch (status) {
      case 'downloading': return 'status-downloading'
      case 'completed': return 'status-completed'
      case 'failed': return 'status-failed'
      case 'cancelled': return 'status-cancelled'
      default: return 'status-pending'
    }
  }

  // Get status icon
  const getStatusIcon = (status) => {
    switch (status) {
      case 'downloading': return <FaDownload className="spinning" />
      case 'completed': return <FaDownload />
      case 'failed': return <FaTimes />
      case 'cancelled': return <FaTimes />
      default: return <FaClock />
    }
  }

  // Handle task actions
  const handleCancel = async (taskId) => {
    try {
      await cancelTask(taskId)
    } catch (error) {
      console.error('Failed to cancel task:', error)
    }
  }

  const handleRemove = (taskId) => {
    removeTask(taskId)
  }

  const handleRetry = async (task) => {
    // Implement retry logic if needed
    console.log('Retry task:', task)
  }

  // Filter tasks by status
  const activeTasks = tasks.filter(task => 
    task.status === 'pending' || task.status === 'downloading'
  )
  
  const completedTasks = tasks.filter(task => 
    task.status === 'completed'
  )
  
  const failedTasks = tasks.filter(task => 
    task.status === 'failed' || task.status === 'cancelled'
  )

  return (
    <div className="download-queue-container">
      <div className="queue-header">
        <div className="header-left">
          <FaHistory className="queue-icon" />
          <h3>{t('downloadQueue.title')}</h3>
          <span className="task-count">({tasks.length})</span>
        </div>
        
        <div className="header-right">
          <button
            onClick={() => refreshTasks()}
            className="refresh-button"
            title="Refresh"
          >
            <FaRedo />
          </button>
          
          <div className="auto-refresh-toggle">
            <input
              type="checkbox"
              id="autoRefresh"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
            />
            <label htmlFor="autoRefresh">Auto-refresh</label>
          </div>
        </div>
      </div>

      {tasks.length === 0 ? (
        <div className="empty-queue">
          <p>{t('downloadQueue.noTasks')}</p>
        </div>
      ) : (
        <>
          {/* Active Downloads */}
          {activeTasks.length > 0 && (
            <div className="queue-section">
              <h4 className="section-title">
                {t('downloadQueue.active')} ({activeTasks.length})
              </h4>
              
              <div className="tasks-list">
                {activeTasks.map((task) => (
                  <div key={task.id} className="task-card active">
                    <div className="task-header">
                      <div className="task-status">
                        {getStatusIcon(task.status)}
                        <span className={`status-text ${getStatusColor(task.status)}`}>
                          {t(`downloadQueue.status.${task.status}`)}
                        </span>
                      </div>
                      
                      <div className="task-actions">
                        {task.status === 'downloading' && (
                          <button
                            onClick={() => handleCancel(task.id)}
                            className="action-button cancel"
                            title={t('downloadQueue.cancel')}
                          >
                            <FaTimes />
                          </button>
                        )}
                        
                        <button
                          onClick={() => handleRemove(task.id)}
                          className="action-button delete"
                          title={t('downloadQueue.remove')}
                        >
                          <FaTrash />
                        </button>
                      </div>
                    </div>
                    
                    <div className="task-info">
                      <div className="task-title" title={task.title || 'Unknown video'}>
                        <strong>📹 {task.title || 'Unknown video'}</strong>
                      </div>
                      
                      <div className="task-url" title={task.url}>
                        {task.url.substring(0, 50)}...
                      </div>
                      
                      <div className="task-details">
                        <span className="detail-item">
                          <strong>Format:</strong> {task.formatId} • {task.quality}
                        </span>
                        
                        {task.filename && (
                          <span className="detail-item">
                            <strong>File:</strong> {task.filename}
                          </span>
                        )}
                      </div>
                    </div>
                    
                    {task.status === 'downloading' && (
                      <div className="task-progress">
                        <ProgressBar progress={task.progress || 0} />
                        
                        <div className="progress-details">
                          {task.progress !== undefined && (
                            <span className="progress-percent">
                              {Math.round(task.progress)}%
                            </span>
                          )}
                          
                          {task.downloadSpeed && (
                            <span className="download-speed">
                              <FaBolt /> {task.downloadSpeed}
                            </span>
                          )}
                          
                          {task.estimatedTime && (
                            <span className="estimated-time">
                              <FaClock /> {formatDuration(task.estimatedTime)}
                            </span>
                          )}
                        </div>
                      </div>
                    )}
                    
                    <div className="task-footer">
                      <span className="task-time">
                        Started: {formatTime(task.createdAt)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Completed Downloads */}
          {completedTasks.length > 0 && (
            <div className="queue-section">
              <div className="section-header">
                <h4 className="section-title">
                  {t('downloadQueue.completed')} ({completedTasks.length})
                </h4>
                
                <button
                  onClick={clearCompleted}
                  className="clear-button"
                >
                  {t('downloadQueue.clearCompleted')}
                </button>
              </div>
              
              <div className="tasks-list completed">
                {completedTasks.slice(0, 5).map((task) => (
                  <div key={task.id} className="task-card completed">
                    <div className="task-header">
                      <div className="task-status">
                        {getStatusIcon(task.status)}
                        <span className={`status-text ${getStatusColor(task.status)}`}>
                          {t(`downloadQueue.status.${task.status}`)}
                        </span>
                      </div>
                    </div>
                    
                    <div className="task-info">
                      <div className="task-filename" title={task.title || task.filename || 'Unknown'}>
                        📁 {task.title || task.filename || 'Unknown file'}
                      </div>
                      
                      <div className="task-actions-completed">
                        {task.filename && (
                          <button
                            onClick={() => downloadAPI.triggerDownload(task.filename)}
                            className="download-link"
                            title="Download completed file"
                          >
                            <FaDownload /> Download
                          </button>
                        )}
                        
                        <button
                          onClick={() => handleRemove(task.id)}
                          className="action-button delete"
                          title={t('downloadQueue.remove')}
                        >
                          <FaTrash />
                        </button>
                      </div>
                    </div>
                    
                    <div className="task-footer">
                      <span className="task-time">
                        Completed: {task.completedAt ? formatTime(task.completedAt) : 'Unknown'}
                      </span>
                      
                      {task.fileSize && (
                        <span className="file-size">
                          Size: {task.fileSize}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Failed Downloads */}
          {failedTasks.length > 0 && (
            <div className="queue-section">
              <h4 className="section-title">
                {t('downloadQueue.failed')} ({failedTasks.length})
              </h4>
              
              <div className="tasks-list failed">
                {failedTasks.slice(0, 3).map((task) => (
                  <div key={task.id} className="task-card failed">
                    <div className="task-header">
                      <div className="task-status">
                        {getStatusIcon(task.status)}
                        <span className={`status-text ${getStatusColor(task.status)}`}>
                          {t(`downloadQueue.status.${task.status}`)}
                        </span>
                      </div>
                      
                      <div className="task-actions">
                        <button
                          onClick={() => handleRetry(task)}
                          className="action-button retry"
                          title={t('downloadQueue.downloadAgain')}
                        >
                          <FaRedo />
                        </button>
                        
                        <button
                          onClick={() => handleRemove(task.id)}
                          className="action-button delete"
                          title={t('downloadQueue.remove')}
                        >
                          <FaTrash />
                        </button>
                      </div>
                    </div>
                    
                    <div className="task-info">
                      <div className="task-url" title={task.url}>
                        {task.url.substring(0, 40)}...
                      </div>
                      
                      {task.error && (
                        <div className="error-message">
                          <strong>Error:</strong> {task.error}
                        </div>
                      )}
                    </div>
                    
                    <div className="task-footer">
                      <span className="task-time">
                        Failed: {task.failedAt ? formatTime(task.failedAt) : 'Unknown'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default DownloadQueue