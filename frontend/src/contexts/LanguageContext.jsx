import React, { createContext, useState, useContext, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import config from '../utils/config';

const LanguageContext = createContext();

export const useLanguage = () => {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useLanguage must be used within LanguageProvider');
  }
  return context;
};

export const LanguageProvider = ({ children }) => {
  const { i18n } = useTranslation();

  const [currentLanguage, setCurrentLanguage] = useState(
    localStorage.getItem('preferredLanguage') || config.app.defaultLanguage
  );

  useEffect(() => {
    const applyLanguage = async () => {
      await i18n.changeLanguage(currentLanguage);
      localStorage.setItem('preferredLanguage', currentLanguage);
    };

    applyLanguage();
  }, [currentLanguage, i18n]);

  const changeLanguage = (lang) => {
    if (config.app.supportedLanguages.includes(lang)) {
      setCurrentLanguage(lang);
    }
  };

  const value = {
    currentLanguage,
    changeLanguage,
    supportedLanguages: config.app.supportedLanguages
  };

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  );
};
