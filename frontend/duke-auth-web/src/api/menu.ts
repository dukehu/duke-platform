import request from '@/utils/request'

export function getMenuTree(appId?: number) {
  return request.get('/auth/menu/tree', { params: { appId } })
}

export function getMenuById(id: number) {
  return request.get(`/auth/menu/${id}`)
}

export function createMenu(data: unknown) {
  return request.post('/auth/menu', data)
}

export function updateMenu(data: unknown) {
  return request.put('/auth/menu', data)
}

export function deleteMenu(id: number) {
  return request.delete(`/auth/menu/${id}`)
}
