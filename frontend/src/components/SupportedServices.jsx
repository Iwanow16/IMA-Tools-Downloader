import React, { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { downloadAPI } from '../services/api'
import config from '../utils/config'
import '../styles/components/SupportedServices.css'
import {
  FaYoutube,
  FaVideo,
  FaCopy,
  FaCheck,
  FaExternalLinkAlt
} from 'react-icons/fa'

const SupportedServices = () => {
  const { t } = useTranslation()
  const [copiedDomain, setCopiedDomain] = useState(null)
  const [services, setServices] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const iconMap = {
    FaYoutube: FaYoutube,
    FaVideo: FaVideo,
  }

  // Service configurations for display
  const serviceConfigs = {
    youtube: {
      id: 'youtube',
      name: 'YouTube',
      domains: ['youtube.com', 'youtu.be', 'youtube-nocookie.com'],
      icon: 'FaYoutube',
      color: '#FF0000',
    },
    bilibili: {
      id: 'bilibili',
      name: 'Bilibili',
      domains: ['bilibili.com', 'b23.tv'],
      icon: 'FaVideo',
      color: '#00A1D6',
    }
  }

  // Загружаем поддерживаемые сервисы с backend при загрузке компонента
  useEffect(() => {
    const loadServices = async () => {
      try {
        setLoading(true)
        setError(null)
        const serviceNames = await downloadAPI.getSupportedServices()
        
        // Обновляем config.supportedServices для использования в других компонентах
        const loadedServices = serviceNames
          .map(name => serviceConfigs[name])
          .filter(Boolean)  // Фильтруем undefined
        
        setServices(loadedServices)
        
        // Также обновляем глобальный config
        config.supportedServices = loadedServices
      } catch (error) {
        console.error('Failed to load services:', error)
        setError(error)
        setServices([])
      } finally {
        setLoading(false)
      }
    }
    
    loadServices()
  }, [])

  const copyToClipboard = (domain) => {
    navigator.clipboard.writeText(domain)
    setCopiedDomain(domain)
    setTimeout(() => setCopiedDomain(null), 2000)
  }

  const openServiceInfo = (serviceId) => {
    const urls = {
      youtube: 'https://www.youtube.com/',
      bilibili: 'https://www.bilibili.com/',
    }
    if (urls[serviceId]) {
      window.open(urls[serviceId], '_blank')
    }
  }

  if (loading) {
    return (
      <div className="supported-services-container">
        <div className="services-header">
          <h3>{t('services.supportedServices')}</h3>
        </div>
        <div className="loading-placeholder">
          {t('app.loading')}
        </div>
      </div>
    )
  }

  if (error || services.length === 0) {
    return (
      <div className="supported-services-container">
        <div className="services-header">
          <h3>{t('services.supportedServices')}</h3>
        </div>
        <div className="no-services-placeholder">
          <div className="no-services-icon">⚠️</div>
          <h4>{t('services.noServicesAvailable') || 'No supported services available'}</h4>
          <p>{t('services.noServicesDescription') || 'Unable to load supported services from backend'}</p>
          {error && (
            <p className="error-details">
              {error.message}
            </p>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="supported-services-container">
      <div className="services-header">
        <h3>{t('services.supportedServices')}</h3>
        <span className="services-count">
          {t('services.count').replace('{count}', services.length)}
        </span>
      </div>

      <div className="services-grid">
        {services.map((service) => {
          const IconComponent = iconMap[service.icon]
          return (
            <div 
              key={service.id} 
              className="service-card"
              style={{ borderColor: service.color }}
              onClick={() => openServiceInfo(service.id)}
            >
              <div className="service-header">
                <div className="service-icon" style={{ color: service.color }}>
                  {IconComponent && <IconComponent />}
                </div>
                <div className="service-info">
                  <h4 className="service-name">{service.name}</h4>
                  <p className="service-description">
                    {t(`services.serviceDetails.${service.id}`)}
                  </p>
                </div>
              </div>

              <div className="service-domains">
                <div className="domains-title">
                  {t('urlInput.detectedService')?.replace('{service}', service.name)}
                </div>
                <div className="domains-list">
                  {service.domains.map((domain, idx) => (
                    <div 
                      key={idx} 
                      className="domain-item"
                      onClick={(e) => {
                        e.stopPropagation()
                        copyToClipboard(domain)
                      }}
                      title={t('services.clickToCopy')}
                    >
                      <span className="domain-text">{domain}</span>
                      {copiedDomain === domain ? (
                        <FaCheck className="copy-icon success" />
                      ) : (
                        <FaCopy className="copy-icon" />
                      )}
                    </div>
                  ))}
                </div>
              </div>

              <div className="service-footer">
                <span className="service-example">
                  {t('common.exampleUrl')}
                </span>
                <FaExternalLinkAlt className="external-icon" />
              </div>
            </div>
          )
        })}
      </div>

      <div className="services-footer">
        <p className="services-note">
          {t('app.description')}
        </p>
      </div>
    </div>
  )
}

export default SupportedServices