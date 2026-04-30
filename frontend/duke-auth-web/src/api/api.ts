import request from '@/utils/request'
import type { ApiQueryDTO } from '@/types/api'

export function getApiPage(params: ApiQueryDTO) {
  return request.get('/auth/api/page', { params })
}

export function getControllers() {
  return request.get('/auth/api/controllers')
}

export function getGroupedApis() {
  return request.get<Record<string, Record<string, any[]>>>('/auth/api/grouped')
}

export function syncApis() {
  return request.post('/auth/api/sync')
}

export function updateApiStatus(id: number, status: number) {
  return request.put(`/auth/api/${id}/status`, { status })
}

export function updateApiPermission(id: number, permission: string) {
  return request.put(`/auth/api/${id}/permission`, { permission })
}
