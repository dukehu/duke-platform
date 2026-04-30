export interface SysRole {
  id?: number
  roleName: string
  roleCode: string
  dataScope: number
  status: number
  remark?: string
  createTime?: string
  menuIds?: number[]
  apiIds?: number[]
  deptIds?: number[]
}

export interface RoleQueryDTO {
  roleName?: string
  status?: number
  current?: number
  size?: number
}
