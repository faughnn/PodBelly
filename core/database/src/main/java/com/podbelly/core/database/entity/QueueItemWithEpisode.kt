package com.podbelly.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class QueueItemWithEpisode(
    @Embedded
    val queueItem: QueueItemEntity,
    @Relation(
        parentColumn = "episodeId",
        entityColumn = "id"
    )
    val episode: EpisodeEntity
)

data class EpisodeWithPodcast(
    @Embedded
    val episode: EpisodeEntity,
    @Relation(
        parentColumn = "podcastId",
        entityColumn = "id"
    )
    val podcast: PodcastEntity
)
