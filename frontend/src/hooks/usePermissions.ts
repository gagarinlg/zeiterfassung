import { useAuth } from './useAuth'

export function usePermissions() {
  const { hasPermission, hasRole, user } = useAuth()
  return { hasPermission, hasRole, user }
}
