import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    collapsed: false
  }),
  actions: {
    toggleCollapse() {
      this.collapsed = !this.collapsed
    }
  }
})
