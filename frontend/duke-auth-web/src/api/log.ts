import request from '@/utils/request'

export function getLogPage(params: unknown) {
  return request.get('/auth/log/page', { params })
}

export function deleteLog(id: number) {
  return request.delete(`/auth/log/${id}`)
}
