import request from '@/utils/request'

export function getDeptTree() {
  return request.get('/auth/dept/tree')
}

export function createDept(data: unknown) {
  return request.post('/auth/dept', data)
}

export function updateDept(data: unknown) {
  return request.put('/auth/dept', data)
}

export function deleteDept(id: number) {
  return request.delete(`/auth/dept/${id}`)
}
