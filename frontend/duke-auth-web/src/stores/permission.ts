import { defineStore } from 'pinia'
import { getMenuTree } from '@/api/auth'
import type { RouteRecordRaw } from 'vue-router'
import type { MenuTreeVO, ButtonVO } from '@/types/menu'

// @ts-ignore
const modules = import.meta.glob('@/views/**/*.vue')

function menuToRoute(menu: MenuTreeVO): RouteRecordRaw | null {
  if (menu.menuType === 3) return null
  const component = menu.component
    ? modules[`/src/views/${menu.component}/index.vue`] || modules[`/src/views/${menu.component}.vue`]
    : undefined
  if (menu.component && !component) return null
  const route: RouteRecordRaw = {
    path: menu.path || '',
    name: `menu_${menu.id}`,
    component: component,
    meta: { title: menu.menuName, icon: menu.icon, menuId: menu.id }
  }
  if (menu.children?.length) {
    // @ts-ignore
    route.children = menu.children
      .map(menuToRoute)
      .filter(Boolean) as RouteRecordRaw[]
  }
  return route
}

function flattenRoutes(menus: MenuTreeVO[]): RouteRecordRaw[] {
  const result: RouteRecordRaw[] = []
  for (const menu of menus) {
    if (menu.menuType === 1) {
      if (menu.children?.length) {
        result.push(...flattenRoutes(menu.children))
      }
    } else if (menu.menuType === 2) {
      const r = menuToRoute(menu)
      if (r) result.push(r)
    }
  }
  return result
}

function buildButtonMap(menus: MenuTreeVO[], map: Record<string, ButtonVO[]> = {}) {
  for (const menu of menus) {
    if (menu.menuType === 2 && menu.path && menu.buttons?.length) {
      map[menu.path] = menu.buttons
    }
    if (menu.children?.length) {
      buildButtonMap(menu.children, map)
    }
  }
  return map
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    routes: [] as RouteRecordRaw[],
    buttonMap: {} as Record<string, ButtonVO[]>,
    loaded: false
  }),
  actions: {
    async generateRoutes() {
      const res = await getMenuTree()
      const menus: MenuTreeVO[] = res.data
      this.routes = menus.map(menuToRoute).filter(Boolean) as RouteRecordRaw[]
      this.buttonMap = buildButtonMap(menus)
      this.loaded = true
      return flattenRoutes(menus)
    },
    reset() {
      this.routes = []
      this.buttonMap = {}
      this.loaded = false
    }
  }
})
