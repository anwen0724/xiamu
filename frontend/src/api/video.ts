import { apiClient } from './client'
import type {
  BaseResponse,
  EntityId,
  PageResponse,
  VideoDashboardVO,
  VideoProcessStatusVO,
  VideoResultVO,
  VideoVO,
} from './types'

function unwrapResponse<T>(response: BaseResponse<T>): T {
  if (response.code !== 0) {
    throw new Error(response.message || '请求失败')
  }

  return response.data
}

export async function uploadVideo(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  const { data } = await apiClient.post<BaseResponse<VideoVO>>('/video/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })

  return unwrapResponse(data)
}

export async function getMyVideos(params: {
  current?: number
  pageSize?: number
  status?: string
}) {
  const { data } = await apiClient.get<BaseResponse<PageResponse<VideoDashboardVO>>>('/video/my/page', {
    params,
  })

  return unwrapResponse(data)
}

export async function getVideoStatus(videoId: EntityId) {
  const { data } = await apiClient.get<BaseResponse<VideoProcessStatusVO>>(`/video/status/${videoId}`)
  return unwrapResponse(data)
}

export async function getVideoResult(videoId: EntityId) {
  const { data } = await apiClient.get<BaseResponse<VideoResultVO>>(`/video/result/${videoId}`)
  return unwrapResponse(data)
}
