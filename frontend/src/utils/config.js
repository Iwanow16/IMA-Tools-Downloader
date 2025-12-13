const config = {
  api: {
    baseUrl:
      import.meta.env.VITE_APP_API_BASE_URL ||
      import.meta.env.REACT_APP_API_BASE_URL ||
      'http://localhost:8080',
    endpoints: {
      info: '/api/info',
      download: '/api/download',
      tasks: '/api/tasks',
      cancel: '/api/tasks'
    }
  },
  app: {
    pollingInterval: parseInt(import.meta.env.VITE_APP_POLLING_INTERVAL) || 3000,
    maxFilenameLength: parseInt(import.meta.env.VITE_APP_MAX_FILENAME_LENGTH) || 100,
    defaultLanguage: import.meta.env.VITE_APP_DEFAULT_LANGUAGE || 'en',
    maxConcurrentDownloads: 3,
    supportedLanguages: ['en', 'ru'],
    maxRetries: 3,
    retryDelay: 1000
  },
  // Поддерживаемые сервисы
  supportedServices: [
    {
      id: 'youtube',
      name: 'YouTube',
      domains: ['youtube.com', 'youtu.be', 'youtube-nocookie.com'],
      icon: 'FaYoutube',
      color: '#FF0000',
      regex: /^(https?:\/\/)?(www\.)?(youtube\.com|youtu\.?be)\/.+$/
    }
  ],
  validation: {
    maxUrlLength: 500,
    // Динамически создаем regex из всех поддерживаемых доменов
    getAllowedDomainsRegex: () => {
      const domains = config.supportedServices.flatMap(service => service.domains)
      const escapedDomains = domains.map(domain => domain.replace('.', '\\.'))
      return new RegExp(`^(https?://)?(www\\.)?(${escapedDomains.join('|')})/.+$`)
    },
    // Функция для определения сервиса по URL
    getServiceByUrl: (url) => {
      return config.supportedServices.find(service => service.regex.test(url))
    }
  }
}

export default config