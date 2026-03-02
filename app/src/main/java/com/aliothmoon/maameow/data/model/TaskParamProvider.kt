package com.aliothmoon.maameow.data.model

import com.aliothmoon.maameow.maa.task.MaaTaskParams
import kotlinx.serialization.Serializable

@Serializable
sealed interface TaskParamProvider {
    fun toTaskParams(): MaaTaskParams
}