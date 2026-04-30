export interface SysUser {
  id?: number
  username: string
  realName: string
  email?: string
  phone?: string
  avatar?: string
  status: number
  createTime?: string
  roleIds?: number[]
  deptIds?: number[]
  primaryDeptId?: number
}

export interface UserQueryDTO {
  keyword?: string
  status?: number
  deptId?: number
  current?: number
  size?: number
}
