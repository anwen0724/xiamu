export type EntityId = string

export type BaseResponse<T> = {
  code: number
  data: T
  message: string
}

export type PageResponse<T> = {
  records: T[]
  total: string
  size: string
  current: string
  pages: string
}

export type UserVO = {
  id: EntityId
  userAccount: string
  userName: string
  createTime: string
}

export type VideoVO = {
  id: EntityId
  title: string
  url: string
  duration: number
  fileSize: number
  status: string
  type: string
  format: string
  createTime: string
}

export type VideoDashboardVO = {
  id: EntityId
  title: string
  status: string
  duration: number
  fileSize: number
  format: string
  summary: string | null
  createTime: string
}

export type VideoProcessStatusVO = {
  id: EntityId
  title: string
  duration: number
  status: string
  stage: string
  stageMessage: string | null
  errorMessage: string | null
  updateTime: string | null
  hasTranscript: boolean
  hasSummary: boolean
  hasChapters: boolean
}

export type VideoChapterVO = {
  title: string
  startSeconds: number
  endSeconds: number
  summary: string | null
}

export type VideoResultVO = {
  id: EntityId
  title: string
  status: string
  duration: number
  fileSize: number
  format: string
  summary: string | null
  transcript: string | null
  errorMessage: string | null
  chapters: VideoChapterVO[]
  createTime: string
  updateTime: string
}

export type NoteVO = {
  id: EntityId
  videoId: EntityId
  title: string
  content: string
  timeSeconds: number | null
  createTime: string
  updateTime: string
}