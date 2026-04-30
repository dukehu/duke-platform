export interface ButtonVO {
  id: number
  buttonName: string
  buttonCode: string
  buttonType: number  // 1=头部 2=行操作
  sortOrder: number
}

export interface SysMenu {
  id?: number
  parentId: number
  appId?: number
  menuName: string
  menuType: number
  path?: string
  component?: string
  icon?: string
  sortOrder?: number
  status: number
  visible: number
  children?: SysMenu[]
}

export interface MenuTreeVO extends SysMenu {
  buttons?: ButtonVO[]
  children: MenuTreeVO[]
}
