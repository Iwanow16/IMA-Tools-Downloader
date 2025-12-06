import React, { useState } from 'react'
import { useTranslation } from 'react-i18next'
import config from '../utils/config'
import {
  FaYoutube,
  FaCopy,
  FaCheck,
  FaExternalLinkAlt
} from 'react-icons/fa'

const SupportedServices = () => {
  const { t } = useTranslation()
  const [copiedDomain, setCopiedDomain] = useState(null)

  const iconMap = {
    FaYoutube: FaYoutube,
  }

  const copyToClipboard = (domain) => {
    navigator.clipboard.writeText(domain)
    setCopiedDomain(domain)
    setTimeout(() => setCopiedDomain(null), 2000)
  }

  const openServiceInfo = (serviceId) => {
    const urls = {
      youtube: 'https://www.youtube.com/',
    }
    if (urls[serviceId]) {
      window.open(urls[serviceId], '_blank')
    }
  }

  return (
    <div className="supported-services-container">
      <div className="services-header">
        <h3>{t('services.supportedServices')}</h3>
        <span className="services-count">
          {t('services.count', { count: config.supportedServices.length })}
        </span>
      </div>

      <div className="services-grid">
        {config.supportedServices.map((service) => {
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
                <div className="domains-title">{t('urlInput.detectedService', { service: service.name })}</div>
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
                  example.com/video-id
                </span>
                <FaExternalLinkAlt className="external-icon" />
              </div>
            </div>
          )
        })}
      </div>

      <div className="services-footer">
        <p className="services-note">
          More platforms are being added regularly. 
          All downloads comply with platform terms of service.
        </p>
      </div>
    </div>
  )
}

export default SupportedServices