import React from 'react'
import '../styles/ProgressBar.css'

const ProgressBar = ({ progress = 0, height = 8, showLabel = true }) => {
  const clampedProgress = Math.min(100, Math.max(0, progress))
  
  return (
    <div className="progress-bar-container">
      <div 
        className="progress-bar"
        style={{ height: `${height}px` }}
      >
        <div 
          className="progress-fill"
          style={{ 
            width: `${clampedProgress}%`,
            height: `${height}px`
          }}
        >
          {showLabel && clampedProgress > 20 && (
            <span className="progress-text">
              {Math.round(clampedProgress)}%
            </span>
          )}
        </div>
      </div>
      
      {showLabel && clampedProgress <= 20 && (
        <span className="progress-label">
          {Math.round(clampedProgress)}%
        </span>
      )}
    </div>
  )
}

export default ProgressBar