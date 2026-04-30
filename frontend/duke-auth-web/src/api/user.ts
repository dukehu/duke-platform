import request from '@/utils/request'
import type { UserQueryDTO } from '@/types/user'

export function getUserPage(params: UserQueryDTO) {
  return request.get('/auth/user/page', { params })
}

export function getUserById(id: number) {
  return request.get(`/auth/user/${id}`)
}

export function createUser(data: unknown) {
  return request.post('/auth/user', data)
}

export function updateUser(data: unknown) {
  return request.put('/auth/user', data)
}

export function deleteUser(id: number) {
  return request.delete(`/auth/user/${id}`)
}

export function updateUserStatus(id: number, status: number) {
  return request.put(`/auth/user/${id}/status`, { status })
}

export function resetPassword(id: number, password: string) {
  return request.put(`/auth/user/${id}/password`, { password })
}

export function assignRoles(id: number, roleIds: number[]) {
  return request.post(`/auth/user/${id}/roles`, roleIds)
}

export function assignDepts(id: number, deptIds: number[], primaryDeptId: number) {
  return request.post(`/auth/user/${id}/depts`, { deptIds, primaryDeptId })
}
