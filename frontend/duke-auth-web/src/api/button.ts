import request from '@/utils/request'

export function listButtons(menuId: number) {
  return request.get('/auth/button/list', { params: { menuId } })
}

export function createButton(data: unknown) {
  return request.post('/auth/button', data)
}

export function updateButton(data: unknown) {
  return request.put('/auth/button', data)
}

export function deleteButton(id: number) {
  return request.delete(`/auth/button/${id}`)
}
