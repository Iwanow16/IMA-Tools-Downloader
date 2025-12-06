import React from 'react'
import { useTranslation } from 'react-i18next'
import UrlInput from './components/UrlInput'
import VideoInfo from './components/VideoInfo'
import DownloadQueue from './components/DownloadQueue'
import LanguageSwitcher from './components/LanguageSwitcher'
import { DownloadProvider } from './contexts/DownloadContext'
import { LanguageProvider } from './contexts/LanguageContext'
import { FaYoutube, FaGithub } from 'react-icons/fa'
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
              <FaYoutube className="logo" />
              <h1 className="app-title">{t('app.title')}</h1>
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
                © {new Date().getFullYear()} YouTube Downloader. For educational purposes only.
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