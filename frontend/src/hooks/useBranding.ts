import { useState, useEffect } from 'react'
import axios from 'axios'

interface BrandingInfo {
  companyName: string
  hasLogo: boolean
}

let cachedBranding: BrandingInfo | null = null

export function useBranding() {
  const [branding, setBranding] = useState<BrandingInfo>(
    cachedBranding ?? { companyName: '', hasLogo: false }
  )

  useEffect(() => {
    if (cachedBranding) return
    let cancelled = false
    axios
      .get<BrandingInfo>('/api/branding/info')
      .then((r) => {
        if (!cancelled) {
          cachedBranding = r.data
          setBranding(r.data)
        }
      })
      .catch(() => {
        // Ignore — defaults are fine
      })
    return () => { cancelled = true }
  }, [])

  return branding
}

export function invalidateBrandingCache() {
  cachedBranding = null
}
