import { computed, type MaybeRefOrGetter, toValue } from 'vue'
import { useQuery } from '@tanstack/vue-query'

import { listVideoNotes } from '../api/note'
import type { EntityId } from '../api/types'

export function useVideoNotesQuery(
  videoId: MaybeRefOrGetter<EntityId>,
  enabled: MaybeRefOrGetter<boolean> = true,
) {
  return useQuery({
    queryKey: computed(() => ['video-notes', toValue(videoId)]),
    queryFn: () => listVideoNotes(toValue(videoId)),
    enabled: computed(() => toValue(enabled) && !!toValue(videoId)),
    refetchOnWindowFocus: false,
    retry: false,
  })
}