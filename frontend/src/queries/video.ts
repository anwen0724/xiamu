import { computed, type MaybeRefOrGetter, toValue } from 'vue'
import { useQuery } from '@tanstack/vue-query'

import { getMyVideos, getVideoResult, getVideoStatus } from '../api/video'
import type { EntityId } from '../api/types'

export function useDashboardVideosQuery(status: MaybeRefOrGetter<string>) {
  return useQuery({
    queryKey: computed(() => ['dashboard-videos', toValue(status)]),
    queryFn: () =>
      getMyVideos({
        current: 1,
        pageSize: 12,
        status: toValue(status) || undefined,
      }),
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    retry: false,
  })
}

export function useVideoStatusQuery(videoId: MaybeRefOrGetter<EntityId>, enabled = true) {
  return useQuery({
    queryKey: computed(() => ['video-status', toValue(videoId)]),
    queryFn: () => getVideoStatus(toValue(videoId)),
    enabled: computed(() => enabled && !!toValue(videoId)),
    refetchInterval: (query) => {
      if (query.state.error) {
        return false
      }
      const status = query.state.data?.status
      return status === 'SUCCESS' || status === 'FAILED' ? false : 3000
    },
    refetchOnWindowFocus: false,
    retry: false,
  })
}

export function useVideoResultQuery(videoId: MaybeRefOrGetter<EntityId>, enabled = true) {
  return useQuery({
    queryKey: computed(() => ['video-result', toValue(videoId)]),
    queryFn: () => getVideoResult(toValue(videoId)),
    enabled: computed(() => enabled && !!toValue(videoId)),
    refetchOnWindowFocus: false,
    retry: false,
  })
}
