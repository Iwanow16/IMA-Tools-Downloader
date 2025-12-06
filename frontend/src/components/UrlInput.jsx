import React, { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDownload } from '../contexts/DownloadContext'
import config from '../utils/config'
import { FaSearch, FaYoutube } from 'react-icons/fa'
import '../styles/UrlInput.css'

const UrlInput = () => {
  const { t } = useTranslation()
  const { fetchVideoInfo, loading, error, setError } = useDownload()
  const [url, setUrl] = useState('')
  const [localError, setLocalError] = useState('')

  const validateYouTubeUrl = (inputUrl) => {
    if (!inputUrl.trim()) {
      return t('urlInput.invalidUrl')
    }
    
    if (inputUrl.length > config.validation.maxUrlLength) {
      return 'URL is too long'
    }
    
    if (!config.validation.youtubeRegex.test(inputUrl)) {
      return t('urlInput.invalidUrl')
    }
    
    return ''
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLocalError('')
    setError(null)

    const validationError = validateYouTubeUrl(url)
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
      const validationError = validateYouTubeUrl(value)
      if (!validationError) {
        setLocalError('')
      }
    }
  }

  return (
    <div className="url-input-container">
      <div className="url-input-header">
        <FaYoutube className="youtube-icon" />
        <h2>{t('app.title')}</h2>
      </div>
      
      <p className="url-input-description">{t('app.description')}</p>

      <form onSubmit={handleSubmit} className="url-input-form">
        <div className="url-input-group">
          <input
            type="text"
            value={url}
            onChange={handleInputChange}
            placeholder={t('urlInput.placeholder')}
            className={`url-input ${localError ? 'error' : ''}`}
            disabled={loading}
          />
          
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
            {localError || error}
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
        <p>{t('urlInput.instructions')}</p>
      </div>
    </div>
  )
}

export default UrlInput