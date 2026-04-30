export interface SysDept {
  id?: number
  parentId: number
  deptName: string
  deptCode?: string
  leader?: string
  phone?: string
  email?: string
  sortOrder?: number
  status: number
  ancestors?: string
  children?: SysDept[]
}

export interface DeptTreeVO extends SysDept {
  children: DeptTreeVO[]
}
