import React from 'react'
import { useTranslation } from 'react-i18next'
import UrlInput from './components/UrlInput'
import VideoInfo from './components/VideoInfo'
import DownloadQueue from './components/DownloadQueue'
import LanguageSwitcher from './components/LanguageSwitcher'
import SupportedServices from './components/SupportedServices'
import { DownloadProvider } from './contexts/DownloadContext'
import { LanguageProvider } from './contexts/LanguageContext'
import { FaDownload, FaGithub, FaGlobe } from 'react-icons/fa'
import './services/i18n'
import './styles/App.css'

const App = () => {
  const { t } = useTranslation()

  return (
    <LanguageProvider>
      <DownloadProvider>
        <div className="app">
          {/* Header */}
          <header className="app-header">
            <div className="logo-container">
              <FaDownload className="logo" />
              <div className="header-content">
                <h1 className="app-title">{t('app.title')}</h1>
                <p className="app-subtitle">{t('app.description')}</p>
              </div>
            </div>
            
            <LanguageSwitcher />
          </header>

          {/* Main Content */}
          <main className="app-main">
            <div className="content-grid">
              {/* Left Column - URL Input & Video Info */}
              <div className="left-column">
                <UrlInput />
                <VideoInfo />
                <SupportedServices />
              </div>

              {/* Right Column - Download Queue */}
              <div className="right-column">
                <DownloadQueue />
              </div>
            </div>
          </main>

          {/* Footer */}
          <footer className="app-footer">
            <div className="footer-content">
              <div className="copyright">
                <FaGlobe className="footer-icon" />
                © {new Date().getFullYear()} Universal Video Downloader. For educational purposes only.
              </div>
              
              <div className="footer-links">
                <a 
                  href="https://github.com" 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="footer-link"
                >
                  <FaGithub /> GitHub
                </a>
              </div>
            </div>
          </footer>
        </div>
      </DownloadProvider>
    </LanguageProvider>
  )
}

export default App