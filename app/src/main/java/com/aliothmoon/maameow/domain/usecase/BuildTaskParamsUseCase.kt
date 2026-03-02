package com.aliothmoon.maameow.domain.usecase

import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.maa.task.MaaTaskParams

class BuildTaskParamsUseCase(private val chainState: TaskChainState) {
    operator fun invoke(): List<MaaTaskParams> {
        return chainState.chain.value
            .filter { it.enabled }
            .sortedBy { it.order }
            .map {
                it.config.toTaskParams()
            }
    }
}
