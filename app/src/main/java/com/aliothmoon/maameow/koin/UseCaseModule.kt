package com.aliothmoon.maameow.koin

import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.domain.usecase.AnalyzeTaskChainUseCase
import com.aliothmoon.maameow.domain.usecase.PrepareTaskStartUseCase
import org.koin.dsl.module


val useCaseModule = module {
    factory {
        AnalyzeTaskChainUseCase(get())
    }
    factory {
        PrepareTaskStartUseCase(get(), get(), get())
    }
}
