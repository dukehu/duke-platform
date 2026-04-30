export interface SysApi {
  id?: number
  appId: number
  controllerClass: string
  controllerName: string
  apiName: string
  apiPath: string
  apiMethod: string
  permission?: string
  status: number
}

export interface ApiQueryDTO {
  keyword?: string
  controllerClass?: string
  status?: number
  current?: number
  size?: number
}
