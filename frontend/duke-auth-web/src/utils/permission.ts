import { useUserStore } from '@/stores/user'
import { SUPER_ADMIN_ROLE } from '@/constants/role'

export function hasPermission(permission: string): boolean {
  const userStore = useUserStore()
  return userStore.buttons.includes(permission) || userStore.roles.includes(SUPER_ADMIN_ROLE)
}
