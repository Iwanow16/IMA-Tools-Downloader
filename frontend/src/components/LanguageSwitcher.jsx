import React from 'react'
import { useTranslation } from 'react-i18next'
import { useLanguage } from '../contexts/LanguageContext'
import { FaGlobe, FaCheck } from 'react-icons/fa'
import '../styles/LanguageSwitcher.css'

const LanguageSwitcher = () => {
  const { t } = useTranslation()
  const { currentLanguage, changeLanguage, supportedLanguages } = useLanguage()
  
  const languageNames = {
    en: t('language.english'),
    ru: t('language.russian')
  }

  return (
    <div className="language-switcher">
      <div className="language-button">
        <FaGlobe className="globe-icon" />
        <span>{languageNames[currentLanguage] || currentLanguage}</span>
      </div>
      
      <div className="language-dropdown">
        {supportedLanguages.map((lang) => (
          <button
            key={lang}
            onClick={() => changeLanguage(lang)}
            className={`language-option ${currentLanguage === lang ? 'active' : ''}`}
          >
            <span>{languageNames[lang] || lang}</span>
            {currentLanguage === lang && <FaCheck className="check-icon" />}
          </button>
        ))}
      </div>
    </div>
  )
}

export default LanguageSwitcher