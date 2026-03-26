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
    placeholderData: (previousData) => previousData,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    retry: false,
  })
}

export function useVideoStatusQuery(
  videoId: MaybeRefOrGetter<EntityId>,
  enabled: MaybeRefOrGetter<boolean> = true,
) {
  return useQuery({
    queryKey: computed(() => ['video-status', toValue(videoId)]),
    queryFn: () => getVideoStatus(toValue(videoId)),
    enabled: computed(() => toValue(enabled) && !!toValue(videoId)),
    refetchOnWindowFocus: false,
    retry: false,
  })
}

export function useVideoResultQuery(
  videoId: MaybeRefOrGetter<EntityId>,
  enabled: MaybeRefOrGetter<boolean> = true,
) {
  return useQuery({
    queryKey: computed(() => ['video-result', toValue(videoId)]),
    queryFn: () => getVideoResult(toValue(videoId)),
    enabled: computed(() => toValue(enabled) && !!toValue(videoId)),
    refetchOnWindowFocus: false,
    retry: false,
  })
}
