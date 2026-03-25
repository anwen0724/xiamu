export type BaseResponse<T> = {
  code: number
  data: T
  message: string
}

export type UserVO = {
  id: number
  userAccount: string
  userName: string
  createTime: string
}
