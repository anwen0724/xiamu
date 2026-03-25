export function formatDuration(totalSeconds?: number | null) {
  if (!totalSeconds || totalSeconds <= 0) {
    return '时长待生成'
  }

  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  if (hours > 0) {
    return `${hours}小时${minutes.toString().padStart(2, '0')}分钟`
  }

  if (minutes > 0) {
    return `${minutes}分钟${seconds.toString().padStart(2, '0')}秒`
  }

  return `${seconds}秒`
}

export function formatRelativeDate(dateTime?: string | null) {
  if (!dateTime) {
    return '时间未知'
  }

  const value = new Date(dateTime).getTime()
  if (Number.isNaN(value)) {
    return dateTime
  }

  const diff = Date.now() - value
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) {
    return '刚刚'
  }

  if (diff < hour) {
    return `${Math.floor(diff / minute)} 分钟前`
  }

  if (diff < day) {
    return `${Math.floor(diff / hour)} 小时前`
  }

  if (diff < 7 * day) {
    return `${Math.floor(diff / day)} 天前`
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}
