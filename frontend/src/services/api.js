import axios from 'axios'
import config from '../utils/config'

const api = axios.create({
  baseURL: config.api.baseUrl,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept-Language': localStorage.getItem('i18nextLng') || config.app.defaultLanguage
  }
})

// Request interceptor
api.interceptors.request.use(
  (config) => {
    console.log(`API Request: ${config.method.toUpperCase()} ${config.url}`)
    return config
  },
  (error) => {
    console.error('API Request Error:', error)
    return Promise.reject(error)
  }
)

// Response interceptor
api.interceptors.response.use(
  (response) => {
    console.log(`API Response: ${response.status} ${response.config.url}`)
    return response
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message
    
    // Log specific error codes
    if (status === 403) {
      console.error('API Error 403 Forbidden:', message)
    } else if (status === 404) {
      console.error('API Error 404 Not Found:', message)
    } else if (status === 401) {
      console.error('API Error 401 Unauthorized:', message)
    } else {
      console.error('API Response Error:', error.response?.data || message)
    }
    
    return Promise.reject(error)
  }
)

export const downloadAPI = {
  // Get video info
  getVideoInfo: async (url) => {
    try {
      const response = await api.get(config.api.endpoints.info, {
        params: { url }
      })
      return response.data?.data || response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || 'Failed to fetch video info')
    }
  },

  // Start download
  startDownload: async (url, formatId, quality) => {
    try {
      const response = await api.post(config.api.endpoints.download, {
        url,
        formatId,
        quality
      })
      return response.data?.data || response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || 'Failed to start download')
    }
  },

  // Get tasks
  getTasks: async () => {
    try {
      const response = await api.get(config.api.endpoints.tasks)
      const tasks = response.data?.data || response.data || []
      // Ensure we always return an array
      return Array.isArray(tasks) ? tasks : []
    } catch (error) {
      // If task not found (404) or forbidden (403), return empty list
      if (error.response?.status === 404 || error.response?.status === 403) {
        return []
      }
      throw new Error(error.response?.data?.message || 'Failed to fetch tasks')
    }
  },

  // Cancel task
  cancelTask: async (taskId) => {
    try {
      await api.delete(`${config.api.endpoints.cancel}/${taskId}`)
    } catch (error) {
      throw new Error(error.response?.data?.message || 'Failed to cancel task')
    }
  },

  // Get supported services
  getSupportedServices: async () => {
    try {
      const response = await api.get(config.api.endpoints.services)
      return response.data?.data || response.data || []
    } catch (error) {
      console.error('Failed to fetch supported services:', error)
      throw error  // Пробросить ошибку, не fallback
    }
  },

  // Download file
  downloadFile: (filename) => {
    return `${config.api.baseUrl}/api/downloads/${filename}`
  }
}

export default api