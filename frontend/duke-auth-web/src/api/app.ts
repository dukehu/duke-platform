import request from '@/utils/request'

export function getAppPage(params: unknown) {
  return request.get('/auth/app/page', { params })
}

export function getAppList() {
  return request.get('/auth/app/list')
}

export function createApp(data: unknown) {
  return request.post('/auth/app', data)
}

export function updateApp(data: unknown) {
  return request.put('/auth/app', data)
}

export function deleteApp(id: number) {
  return request.delete(`/auth/app/${id}`)
}
