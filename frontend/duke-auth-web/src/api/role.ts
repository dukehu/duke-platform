import request from '@/utils/request'
import type { RoleQueryDTO } from '@/types/role'

export function getRolePage(params: RoleQueryDTO) {
  return request.get('/auth/role/page', { params })
}

export function getRoleList() {
  return request.get('/auth/role/list')
}

export function createRole(data: unknown) {
  return request.post('/auth/role', data)
}

export function updateRole(data: unknown) {
  return request.put('/auth/role', data)
}

export function deleteRole(id: number) {
  return request.delete(`/auth/role/${id}`)
}

export function assignMenus(id: number, menuIds: number[]) {
  return request.post(`/auth/role/${id}/menus`, menuIds)
}

export function assignApis(id: number, apiIds: number[]) {
  return request.post(`/auth/role/${id}/apis`, apiIds)
}

export function getRoleMenuIds(id: number) {
  return request.get(`/auth/role/${id}/menus`)
}

export function getRoleApiIds(id: number) {
  return request.get(`/auth/role/${id}/apis`)
}

export function getRoleButtonIds(id: number) {
  return request.get(`/auth/role/${id}/buttons`)
}

export function assignButtons(id: number, buttonIds: number[]) {
  return request.post(`/auth/role/${id}/buttons`, buttonIds)
}
