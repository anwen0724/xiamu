import { apiClient } from './client'
import type { BaseResponse, EntityId, NoteVO } from './types'

type AddNotePayload = {
  videoId: EntityId
  title: string
  content: string
  timeSeconds?: number
}

function unwrapResponse<T>(response: BaseResponse<T>): T {
  if (response.code !== 0) {
    throw new Error(response.message || '请求失败')
  }

  return response.data
}

export async function listVideoNotes(videoId: EntityId) {
  const { data } = await apiClient.get<BaseResponse<NoteVO[]>>('/note/list/video', {
    params: { videoId },
  })
  return unwrapResponse(data)
}

export async function addNote(payload: AddNotePayload) {
  const { data } = await apiClient.post<BaseResponse<number>>('/note/add', payload)
  return unwrapResponse(data)
}

export async function deleteNote(id: EntityId) {
  const { data } = await apiClient.post<BaseResponse<boolean>>('/note/delete', { id })
  return unwrapResponse(data)
}
