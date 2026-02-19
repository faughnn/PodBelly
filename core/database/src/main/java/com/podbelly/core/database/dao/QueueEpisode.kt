package com.podbelly.core.database.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.QueueItemEntity

data class QueueEpisode(
    @Embedded
    val queueItem: QueueItemEntity,

    @Relation(
        parentColumn = "episodeId",
        entityColumn = "id"
    )
    val episode: EpisodeEntity
)
