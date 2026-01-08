import React, { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useDownload } from '../contexts/DownloadContext'
import { FaClock, FaImage, FaChevronDown, FaChevronUp } from 'react-icons/fa'
import '../styles/components/DownloadOptions.css'

const DownloadOptions = ({ isOpen, onToggle, videoDuration }) => {
  const { t } = useTranslation()
  const { downloadOptions, updateDownloadOption } = useDownload()

  const isTimeRangeValid = useMemo(() => {
    if (!downloadOptions.timeRangeEnabled) return true
    
    const start = parseFloat(downloadOptions.startTime) || 0
    const end = parseFloat(downloadOptions.endTime) || videoDuration
    
    if (videoDuration && (start >= end || end > videoDuration)) {
      return false
    }
    
    return start >= 0 && end > 0
  }, [downloadOptions, videoDuration])

  const isFrameTimeValid = useMemo(() => {
    if (!downloadOptions.frameExtractionEnabled) return true
    
    const frameTime = parseFloat(downloadOptions.frameTime) || 0
    
    if (videoDuration && frameTime > videoDuration) {
      return false
    }
    
    return frameTime >= 0
  }, [downloadOptions, videoDuration])

  const handleTimeRangeToggle = () => {
    if (!downloadOptions.timeRangeEnabled) {
      // When enabling, reset frame extraction
      updateDownloadOption('frameExtractionEnabled', false)
    }
    updateDownloadOption('timeRangeEnabled', !downloadOptions.timeRangeEnabled)
  }

  const handleFrameExtractionToggle = () => {
    if (!downloadOptions.frameExtractionEnabled) {
      // When enabling, reset time range
      updateDownloadOption('timeRangeEnabled', false)
    }
    updateDownloadOption('frameExtractionEnabled', !downloadOptions.frameExtractionEnabled)
  }

  return (
    <div className="download-options-container">
      <button className="options-toggle" onClick={onToggle}>
        <span>{t('downloadOptions.title')}</span>
        {isOpen ? <FaChevronUp /> : <FaChevronDown />}
      </button>

      {isOpen && (
        <div className="options-content">
          {/* Time Range Option */}
          <div className="option-group">
            <label className="option-label">
              <div className="option-header">
                <input
                  type="checkbox"
                  checked={downloadOptions.timeRangeEnabled}
                  onChange={handleTimeRangeToggle}
                  className="option-checkbox"
                />
                <FaClock className="option-icon" />
                <span>{t('downloadOptions.enableTimeRange')}</span>
              </div>
            </label>

            {downloadOptions.timeRangeEnabled && (
              <div className="option-inputs">
                <div className="time-input-group">
                  <div className="time-input-field">
                    <label htmlFor="startTime">{t('downloadOptions.startTime')}</label>
                    <input
                      id="startTime"
                      type="number"
                      min="0"
                      step="0.5"
                      value={downloadOptions.startTime}
                      onChange={(e) => updateDownloadOption('startTime', e.target.value)}
                      placeholder="0"
                      className="time-input"
                    />
                  </div>

                  <div className="time-input-field">
                    <label htmlFor="endTime">{t('downloadOptions.endTime')}</label>
                    <input
                      id="endTime"
                      type="number"
                      min="0"
                      step="0.5"
                      value={downloadOptions.endTime}
                      onChange={(e) => updateDownloadOption('endTime', e.target.value)}
                      placeholder={videoDuration?.toString() || '0'}
                      className="time-input"
                    />
                  </div>
                </div>

                {!isTimeRangeValid && (
                  <div className="validation-error">
                    {t('downloadOptions.hint')}
                  </div>
                )}

                {videoDuration && (
                  <div className="duration-info">
                    {t('videoInfo.duration')}: {formatDuration(videoDuration)}
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Frame Extraction Option */}
          <div className="option-group">
            <label className="option-label">
              <div className="option-header">
                <input
                  type="checkbox"
                  checked={downloadOptions.frameExtractionEnabled}
                  onChange={handleFrameExtractionToggle}
                  className="option-checkbox"
                />
                <FaImage className="option-icon" />
                <span>{t('downloadOptions.enableFrameExtraction')}</span>
              </div>
            </label>

            {downloadOptions.frameExtractionEnabled && (
              <div className="option-inputs">
                <div className="time-input-field">
                  <label htmlFor="frameTime">{t('downloadOptions.frameTime')}</label>
                  <input
                    id="frameTime"
                    type="number"
                    min="0"
                    step="0.5"
                    value={downloadOptions.frameTime}
                    onChange={(e) => updateDownloadOption('frameTime', e.target.value)}
                    placeholder="0"
                    className="time-input"
                  />
                </div>

                {!isFrameTimeValid && (
                  <div className="validation-error">
                    {t('downloadOptions.hint')}
                  </div>
                )}

                {videoDuration && (
                  <div className="duration-info">
                    {t('videoInfo.duration')}: {formatDuration(videoDuration)}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
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

export default DownloadOptions
