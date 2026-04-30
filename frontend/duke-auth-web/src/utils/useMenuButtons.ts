import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { usePermissionStore } from '@/stores/permission'
import { useUserStore } from '@/stores/user'
import { SUPER_ADMIN_ROLE } from '@/constants/role'
import type { ButtonVO } from '@/types/menu'

export function useMenuButtons() {
  const route = useRoute()
  const permStore = usePermissionStore()
  const userStore = useUserStore()

  const isSuperAdmin = computed(() => userStore.roles.includes(SUPER_ADMIN_ROLE))

  const buttons = computed<ButtonVO[]>(() => {
    if (isSuperAdmin.value) return permStore.buttonMap[route.path] || []
    const all = permStore.buttonMap[route.path] || []
    return all.filter(b => userStore.buttons.includes(b.buttonCode))
  })

  const headerButtons = computed(() => buttons.value.filter(b => b.buttonType === 1))
  const rowButtons = computed(() => buttons.value.filter(b => b.buttonType === 2))

  return { headerButtons, rowButtons }
}
