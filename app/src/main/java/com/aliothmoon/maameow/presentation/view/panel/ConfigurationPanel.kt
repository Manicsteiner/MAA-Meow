package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aliothmoon.maameow.data.model.AwardConfig
import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.data.model.InfrastConfig
import com.aliothmoon.maameow.data.model.MallConfig
import com.aliothmoon.maameow.data.model.ReclamationConfig
import com.aliothmoon.maameow.data.model.RecruitConfig
import com.aliothmoon.maameow.data.model.RoguelikeConfig
import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.data.model.WakeUpConfig
import com.aliothmoon.maameow.data.model.TaskParamProvider
import com.aliothmoon.maameow.presentation.components.EmptyConfigHint
import com.aliothmoon.maameow.presentation.view.panel.fight.FightConfigPanel
import com.aliothmoon.maameow.presentation.view.panel.mall.MallConfigPanel
import com.aliothmoon.maameow.presentation.view.panel.roguelike.RoguelikeConfigPanel

@Composable
fun TaskConfigPanel(
    selectedNode: TaskChainNode?,
    onConfigChange: (TaskParamProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (selectedNode == null) {
            EmptyConfigHint()
            return@Box
        }
        when (val cfg = selectedNode.config) {
            is WakeUpConfig -> WakeUpConfigPanel(
                config = cfg,
                onConfigChange = onConfigChange
            )

            is RecruitConfig -> RecruitConfigPanel(
                config = cfg,
                onConfigChange = onConfigChange
            )

            is InfrastConfig -> InfrastConfigPanel(
                config = cfg,
                onConfigChange = onConfigChange
            )

            is FightConfig -> FightConfigPanel(
                config = cfg,
                onConfigChange = onConfigChange
            )

            is MallConfig -> MallConfigPanel(
                config = cfg,
                onConfigChange = onConfigChange
            )

            is AwardConfig -> AwardConfigPanel(
                config = cfg,
                onConfigChange = onConfigChange
            )

            is RoguelikeConfig -> RoguelikeConfigPanel(
                config = cfg,
                onConfigChange = onConfigChange
            )

            is ReclamationConfig -> ReclamationConfigPanel(
                config = cfg,
                onConfigChange = onConfigChange
            )
        }
    }
}
