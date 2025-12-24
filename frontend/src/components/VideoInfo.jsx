import React, { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useDownload } from '../contexts/DownloadContext'
import config from '../utils/config'
import '../styles/components/VideoInfo.css'
import { 
  FaDownload, 
  FaVideo, 
  FaMusic, 
  FaInfoCircle, 
  FaCheck,
  FaGlobe
} from 'react-icons/fa'

const VideoInfo = () => {
  const { t } = useTranslation()
  const { 
    videoInfo, 
    loading, 
    selectedFormat, 
    setSelectedFormat, 
    startDownload,
    tasks 
  } = useDownload()
  
  const { isDownloading, downloaded } = useMemo(() => {
    if (!videoInfo || !videoInfo.url) {
      return { isDownloading: false, downloaded: false }
    }
    
    const isInQueue = tasks.some(task => 
      task.url === videoInfo.url && 
      (task.status === 'pending' || task.status === 'downloading')
    )
    
    const isDownloaded = tasks.some(task => 
      task.url === videoInfo.url && 
      task.status === 'completed'
    )
    
    return {
      isDownloading: isInQueue,
      downloaded: isDownloaded
    }
  }, [videoInfo, tasks])

  const detectedService = useMemo(() => {
    if (!videoInfo || !videoInfo.url) return null
    return config.validation.getServiceByUrl(videoInfo.url)
  }, [videoInfo])

  if (!videoInfo || loading) {
    return null
  }

  const formatDuration = (seconds) => {
    if (!seconds) return '--:--'
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const secs = Math.floor(seconds % 60)
    
    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`
  }

  const formatFileSize = (bytes) => {
    if (!bytes) return 'Unknown size'
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const handleFormatSelect = (format) => {
    setSelectedFormat(format)
  }

  const handleDownload = async () => {
    if (!selectedFormat || !videoInfo.url) return
    
    try {
      await startDownload(
        videoInfo.url,
        selectedFormat.format_id,
        selectedFormat.quality || selectedFormat.resolution
      )
    } catch (error) {
      console.error('Download failed:', error)
    }
  }

  const getFormatIcon = (format) => {
    if (format.vcodec === 'none') return <FaMusic />
    if (format.acodec === 'none') return <FaVideo />
    return <FaVideo />
  }

  const getFormatLabel = (format) => {
    // Определяем тип формата
    let type = 'Video + Audio'
    if (format.vcodec === 'none') {
      type = 'Audio Only'
    } else if (format.acodec === 'none') {
      type = 'Video Only'
    }
    
    // Получаем качество
    let quality = format.quality || format.resolution || format.format_note || 'Unknown Quality'
    
    return {
      type,
      quality: quality.toString().trim() || 'Unknown Quality'
    }
  }

  return (
    <div className="video-info-container">
      <div className="video-info-header">
        <div className="header-left">
          <FaInfoCircle className="info-icon" />
          <h3>{t('videoInfo.formats')}</h3>
        </div>
        
        {detectedService && (
          <div className="platform-badge" style={{ backgroundColor: detectedService.color }}>
            <FaGlobe className="platform-icon" />
            <span>{detectedService.name}</span>
          </div>
        )}
      </div>

      <div className="video-metadata">
        <div className="metadata-item">
          <span className="metadata-label">{t('videoInfo.platform')}:</span>
          <span className="metadata-value">
            {detectedService ? detectedService.name : 'Unknown'}
          </span>
        </div>
        
        <div className="metadata-item">
          <span className="metadata-label">{t('videoInfo.title')}:</span>
          <span className="metadata-value" title={videoInfo.title}>
            {videoInfo.title}
          </span>
        </div>
        
        {videoInfo.author && (
          <div className="metadata-item">
            <span className="metadata-label">{t('videoInfo.author')}:</span>
            <span className="metadata-value">{videoInfo.author}</span>
          </div>
        )}
        
        {videoInfo.duration && (
          <div className="metadata-item">
            <span className="metadata-label">{t('videoInfo.duration')}:</span>
            <span className="metadata-value">
              {formatDuration(videoInfo.duration)}
            </span>
          </div>
        )}
      </div>

      {videoInfo.formats && videoInfo.formats.length > 0 ? (
        <>
          <div className="formats-grid">
            {videoInfo.formats.map((format) => {
              const { type, quality } = getFormatLabel(format)
              return (
                <div
                  key={format.format_id}
                  className={`format-card ${selectedFormat?.format_id === format.format_id ? 'selected' : ''}`}
                  onClick={() => handleFormatSelect(format)}
                >
                  <div className="format-icon">
                    {getFormatIcon(format)}
                  </div>
                  
                  <div className="format-details">
                    <div className="format-quality">
                      {quality}
                    </div>
                    
                    <div className="format-type">
                      {type}
                    </div>
                    
                    <div className="format-info">
                      <span className="format-extension">
                        {format.ext?.toUpperCase() || 'Unknown'}
                      </span>
                      
                      <span className="format-size">
                        {formatFileSize(format.filesize)}
                      </span>
                    </div>
                  </div>
                  
                  {selectedFormat?.format_id === format.format_id && (
                    <div className="format-check">
                      <FaCheck />
                    </div>
                  )}
                </div>
              )
            })}
          </div>

          <div className="download-section">
            <div className="selected-format-info">
              {selectedFormat && (() => {
                const { type, quality } = getFormatLabel(selectedFormat)
                return (
                  <>
                    <span>{t('videoInfo.selectFormat')}: </span>
                    <strong>
                      {quality} • {type} • 
                      {selectedFormat.ext?.toUpperCase()} • 
                      {formatFileSize(selectedFormat.filesize)}
                    </strong>
                  </>
                )
              })()}
            </div>
            
            <button
              onClick={handleDownload}
              className="download-button"
              disabled={!selectedFormat || isDownloading}
            >
              {isDownloading ? (
                <>
                  <span className="loading-spinner-small"></span>
                  <span>{t('videoInfo.startingDownload')}</span>
                </>
              ) : downloaded ? (
                <>
                  <FaDownload />
                  <span>{t('downloadQueue.downloadAgain')}</span>
                </>
              ) : (
                <>
                  <FaDownload />
                  <span>{t('videoInfo.download')}</span>
                </>
              )}
            </button>
            
            {(isDownloading || downloaded) && (
              <div className="download-status">
                {isDownloading && (
                  <span className="status-downloading">
                    ⏳ {t('downloadQueue.status.downloading')}
                  </span>
                )}
                {downloaded && (
                  <span className="status-completed">
                    ✅ {t('downloadQueue.status.completed')}
                  </span>
                )}
              </div>
            )}
          </div>
        </>
      ) : (
        <div className="no-formats">
          <p>{t('videoInfo.noFormats')}</p>
        </div>
      )}
    </div>
  )
}

export default VideoInfo