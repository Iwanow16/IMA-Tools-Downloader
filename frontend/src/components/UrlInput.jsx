import React, { useState, useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useDownload } from '../contexts/DownloadContext'
import config from '../utils/config'
import { FaSearch, FaVideo, FaGlobe, FaInfoCircle, FaYoutube } from 'react-icons/fa'
import '../styles/components/UrlInput.css'

const UrlInput = () => {
  const { t } = useTranslation()
  const { fetchVideoInfo, loading, error, setError } = useDownload()
  const [url, setUrl] = useState('')
  const [localError, setLocalError] = useState('')
  const [detectedService, setDetectedService] = useState(null)

  const detectService = useMemo(() => {
    return (inputUrl) => {
      if (!config.supportedServices || config.supportedServices.length === 0) {
        return null
      }
      return config.supportedServices.find(service => {
        if (!service || !service.regex) {
          return false
        }
        return service.regex.test(inputUrl)
      }) || null
    }
  }, [])

  const validateVideoUrl = (inputUrl) => {
    if (!inputUrl.trim()) {
      return t('urlInput.invalidUrl')
    }
    
    if (inputUrl.length > config.validation.maxUrlLength) {
      return t('errors.urlTooLong')
    }
    
    const service = detectService(inputUrl)
    if (!service) {
      return t('errors.unsupported')
    }
    
    return ''
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLocalError('')
    setError(null)

    const validationError = validateVideoUrl(url)
    if (validationError) {
      setLocalError(validationError)
      return
    }

    try {
      await fetchVideoInfo(url)
    } catch (err) {
      setLocalError(err.message)
    }
  }

  const handleInputChange = (e) => {
    const value = e.target.value
    setUrl(value)
    
    // Clear error when user starts typing
    if (localError) {
      const validationError = validateVideoUrl(value)
      if (!validationError) {
        setLocalError('')
      }
    }
  }

  // Update detected service when URL changes
  useEffect(() => {
    if (url) {
      const service = detectService(url)
      setDetectedService(service)
    } else {
      setDetectedService(null)
    }
  }, [url, detectService])

  return (
    <div className="url-input-container">
      <div className="url-input-header">
        <FaVideo className="video-icon" />
        <div className="header-content">
          <h2>{t('app.title')}</h2>
          <p className="app-subtitle">{t('app.description')}</p>
        </div>
      </div>

      <div className="service-detection">
        {detectedService && (
          <div className="detected-service" style={{ borderColor: detectedService.color }}>
            <FaYoutube className="service-icon" style={{ color: detectedService.color }} />
            <span>{t('urlInput.detectedService').replace('{service}', detectedService.name)}</span>
          </div>
        )}
      </div>

      <form onSubmit={handleSubmit} className="url-input-form">
        <div className="url-input-group">
          <div className="input-wrapper">
            <FaGlobe className="input-icon" />
            <input
              type="text"
              value={url}
              onChange={handleInputChange}
              placeholder={t('urlInput.placeholder')}
              className={`url-input ${localError ? 'error' : ''}`}
              disabled={loading}
            />
          </div>
          
          <button
            type="submit"
            className="url-submit-button"
            disabled={loading || !url.trim()}
          >
            {loading ? (
              <span className="loading-spinner"></span>
            ) : (
              <>
                <FaSearch />
                <span>{t('urlInput.button')}</span>
              </>
            )}
          </button>
        </div>

        {(localError || error) && (
          <div className="error-message">
            <FaInfoCircle className="error-icon" />
            <span>{localError || error}</span>
          </div>
        )}

        {loading && (
          <div className="loading-indicator">
            <div className="spinner"></div>
            <span>{t('urlInput.fetching')}</span>
          </div>
        )}
      </form>

      <div className="instructions">
        <FaInfoCircle className="info-icon" />
        <p>{t('urlInput.instructions')}</p>
      </div>
    </div>
  )
}

export default UrlInput