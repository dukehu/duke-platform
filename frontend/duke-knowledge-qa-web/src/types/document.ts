export interface Document {
  id: number
  title: string
  category: string
  tags: string[]
  fileType: string
  fileUrl: string
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  createdBy: number
  createTime: string
  updateTime: string
}

export interface DocumentQueryDTO {
  category?: string
  keyword?: string
  status?: string
  current?: number
  size?: number
}
