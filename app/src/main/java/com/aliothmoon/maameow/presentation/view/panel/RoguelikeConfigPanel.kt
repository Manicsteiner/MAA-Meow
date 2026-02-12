package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.data.model.RoguelikeConfig
import com.aliothmoon.maameow.data.resource.CharacterDataManager
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.CoreCharSelector
import com.aliothmoon.maameow.presentation.components.ITextField
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipContent
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipIcon
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 自动肉鸽配置面板 - 迁移自 WPF RoguelikeSettingsUserControl.xaml
 *
 * 布局结构：
 * - Tab 1 (常规设置): 主题/难度/模式/分队/阵容/核心干员
 * - Tab 2 (高级设置): 投资/助战/开局次数/模式特殊选项
 */
@Composable
fun RoguelikeConfigPanel(
    config: RoguelikeConfig,
    onConfigChange: (RoguelikeConfig) -> Unit,
    characterDataManager: CharacterDataManager = koinInject()
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 4.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Tab 行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "常规设置",
                style = MaterialTheme.typography.bodyMedium,
                color = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else Color.Gray,
                fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clickable {
                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                }
            )
            Text(
                text = "高级设置",
                style = MaterialTheme.typography.bodyMedium,
                color = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else Color.Gray,
                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clickable {
                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))

        HorizontalPager(
            pageSize = PageSize.Fill,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            userScrollEnabled = true
        ) { page ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(end = 12.dp, bottom = 8.dp)
            ) {
                when (page) {
                    0 -> {
                        item { BasicRoguelikeSettings(config, onConfigChange, characterDataManager) }
                    }
                    1 -> {
                        item { AdvancedRoguelikeSettings(config, onConfigChange) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BasicRoguelikeSettings(
    config: RoguelikeConfig,
    onConfigChange: (RoguelikeConfig) -> Unit,
    characterDataManager: CharacterDataManager
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 主题选择 - 使用按钮组
        RoguelikeButtonGroup(
            label = "肉鸽主题",
            selectedValue = config.theme,
            options = RoguelikeConfig.THEME_OPTIONS,
            onValueChange = { newTheme ->
                // 切换主题时重置分队和模式（如果当前值不在新主题支持列表中）
                val newSquads = RoguelikeConfig.getSquadOptionsForTheme(newTheme)
                val newSquad = if (config.squad in newSquads) config.squad else newSquads.firstOrNull() ?: ""

                // 验证当前模式是否在新主题支持的模式列表中
                val newMode = if (RoguelikeConfig.isModeValidForTheme(config.mode, newTheme)) {
                    config.mode
                } else {
                    RoguelikeConfig.getModeOptionsForTheme(newTheme).firstOrNull()?.first ?: "Exp"
                }

                onConfigChange(config.copy(theme = newTheme, squad = newSquad, mode = newMode))
            }
        )

        // 难度选择 - 使用按钮组
        RoguelikeDifficultyButtonGroup(
            label = "难度",
            selectedValue = config.difficulty,
            theme = config.theme,
            onValueChange = { onConfigChange(config.copy(difficulty = it)) }
        )

        // 策略模式选择 - 使用按钮组（根据主题动态变化）
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            RoguelikeButtonGroup(
                label = "策略",
                selectedValue = config.mode,
                options = RoguelikeConfig.getModeOptionsForTheme(config.theme),
                onValueChange = { onConfigChange(config.copy(mode = it)) }
            )

            // 模式说明 - 紧跟在策略按钮组下方
            val modeDescription = RoguelikeConfig.MODE_OPTIONS.find { it.first == config.mode }?.second ?: ""
            if (modeDescription.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        modeDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        // 分队选择 - 使用按钮组
        RoguelikeSquadButtonGroup(
            label = "起始分队",
            selectedValue = config.squad,
            theme = config.theme,
            onValueChange = { onConfigChange(config.copy(squad = it)) }
        )

        // 职业阵容 - 使用按钮组
        RoguelikeButtonGroup(
            label = "起始阵容",
            selectedValue = config.roles,
            options = RoguelikeConfig.ROLES_OPTIONS,
            onValueChange = { onConfigChange(config.copy(roles = it)) }
        )

        // 核心干员选择 - 带校验和自动补全
        CoreCharSelector(
            value = config.coreChar,
            onValueChange = { onConfigChange(config.copy(coreChar = it)) },
            theme = config.theme,
            characterDataManager = characterDataManager,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AdvancedRoguelikeSettings(config: RoguelikeConfig, onConfigChange: (RoguelikeConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 投资相关
        Text("投资设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

        CheckBoxWithLabel(
            checked = config.investmentEnabled,
            onCheckedChange = { onConfigChange(config.copy(investmentEnabled = it)) },
            label = "启用投资"
        )

        AnimatedVisibility(visible = config.investmentEnabled) {
            Column(
                modifier = Modifier.padding(start = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("投资次数上限", style = MaterialTheme.typography.bodySmall)
                    ITextField(
                        value = config.investCount.toString(),
                        onValueChange = { onConfigChange(config.copy(investCount = it.toIntOrNull() ?: 999)) },
                        placeholder = "999",
                        modifier = Modifier.width(80.dp)
                    )
                }

                CheckBoxWithLabel(
                    checked = config.stopWhenInvestmentFull,
                    onCheckedChange = { onConfigChange(config.copy(stopWhenInvestmentFull = it)) },
                    label = "投资存款满时停止"
                )

                if (config.mode == "Investment") {
                    CheckBoxWithLabel(
                        checked = config.investmentWithMoreScore,
                        onCheckedChange = { onConfigChange(config.copy(investmentWithMoreScore = it)) },
                        label = "投资模式下刷更多分数"
                    )
                }
            }
        }

        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)

        // 助战相关
        Text("助战设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

        var supportTipExpanded by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckBoxWithLabel(
                    checked = config.useSupport,
                    onCheckedChange = { onConfigChange(config.copy(useSupport = it)) },
                    label = "核心干员使用助战"
                )
                Spacer(modifier = Modifier.width(4.dp))
                ExpandableTipIcon(expanded = supportTipExpanded, onExpandedChange = { supportTipExpanded = it })
            }
            ExpandableTipContent(
                visible = supportTipExpanded,
                tipText = "需先填写「核心干员」",
                modifier = Modifier.padding(start = 28.dp)
            )
        }

        AnimatedVisibility(visible = config.useSupport) {
            CheckBoxWithLabel(
                checked = config.enableNonfriendSupport,
                onCheckedChange = { onConfigChange(config.copy(enableNonfriendSupport = it)) },
                label = "可以使用非好友助战",
                modifier = Modifier.padding(start = 24.dp)
            )
        }

        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)

        // 开局次数
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("开局次数限制", style = MaterialTheme.typography.bodySmall)
            ITextField(
                value = config.startsCount.toString(),
                onValueChange = { onConfigChange(config.copy(startsCount = it.toIntOrNull() ?: 99999)) },
                placeholder = "99999",
                modifier = Modifier.width(100.dp)
            )
        }

        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)

        // 模式特殊设置
        ModeSpecificSettings(config, onConfigChange)
    }
}

@Composable
private fun ModeSpecificSettings(config: RoguelikeConfig, onConfigChange: (RoguelikeConfig) -> Unit) {
    when (config.mode) {
        "Exp" -> {
            Text("刷等级模式设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            CheckBoxWithLabel(
                checked = config.stopAtFinalBoss,
                onCheckedChange = { onConfigChange(config.copy(stopAtFinalBoss = it)) },
                label = "在第五层 BOSS 前暂停"
            )
            CheckBoxWithLabel(
                checked = config.stopAtMaxLevel,
                onCheckedChange = { onConfigChange(config.copy(stopAtMaxLevel = it)) },
                label = "满级后自动停止"
            )
        }

        "Investment" -> {
            // 投资模式设置已在上面处理
        }

        "Collectible" -> {
            Text("刷开局模式设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

            RoguelikeSquadButtonGroup(
                label = "烧水使用分队",
                selectedValue = config.collectibleModeSquad,
                theme = config.theme,
                onValueChange = { onConfigChange(config.copy(collectibleModeSquad = it)) }
            )

            CheckBoxWithLabel(
                checked = config.collectibleModeShopping,
                onCheckedChange = { onConfigChange(config.copy(collectibleModeShopping = it)) },
                label = "刷开局模式启用购物"
            )

            CheckBoxWithLabel(
                checked = config.startWithEliteTwo,
                onCheckedChange = { onConfigChange(config.copy(startWithEliteTwo = it)) },
                label = "凹「核心干员」直升精二"
            )

            AnimatedVisibility(visible = config.startWithEliteTwo) {
                CheckBoxWithLabel(
                    checked = config.onlyStartWithEliteTwo,
                    onCheckedChange = { onConfigChange(config.copy(onlyStartWithEliteTwo = it)) },
                    label = "只凹精二开局，不进行作战",
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
        }

        "Squad" -> {
            Text("月度小队模式设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            CheckBoxWithLabel(
                checked = config.monthlySquadAutoIterate,
                onCheckedChange = { onConfigChange(config.copy(monthlySquadAutoIterate = it)) },
                label = "月度小队自动切换"
            )
            CheckBoxWithLabel(
                checked = config.monthlySquadCheckComms,
                onCheckedChange = { onConfigChange(config.copy(monthlySquadCheckComms = it)) },
                label = "月度小队通讯"
            )
        }

        "Exploration" -> {
            Text("深入调查模式设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            CheckBoxWithLabel(
                checked = config.deepExplorationAutoIterate,
                onCheckedChange = { onConfigChange(config.copy(deepExplorationAutoIterate = it)) },
                label = "深入调查自动切换"
            )
        }

        "CLP_PDS" -> {
            if (config.theme == "Sami") {
                Text("刷坍缩范式设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                ITextField(
                    value = config.expectedCollapsalParadigms,
                    onValueChange = { onConfigChange(config.copy(expectedCollapsalParadigms = it)) },
                    label = "坍缩范式列表",
                    placeholder = "用英文分号 ; 隔开",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        "FindPlaytime" -> {
            if (config.theme == "JieGarden") {
                Text("刷常乐节点设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                RoguelikeButtonGroup(
                    label = "目标常乐节点",
                    selectedValue = config.findPlaytimeTarget,
                    options = RoguelikeConfig.PLAYTIME_TARGET_OPTIONS,
                    onValueChange = { onConfigChange(config.copy(findPlaytimeTarget = it)) }
                )
            }
        }
    }

    // 主题特殊设置
    ThemeSpecificSettings(config, onConfigChange)
}

@Composable
private fun ThemeSpecificSettings(config: RoguelikeConfig, onConfigChange: (RoguelikeConfig) -> Unit) {
    when (config.theme) {
        "Mizuki" -> {
            HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
            Text("水月专用设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            CheckBoxWithLabel(
                checked = config.refreshTraderWithDice,
                onCheckedChange = { onConfigChange(config.copy(refreshTraderWithDice = it)) },
                label = "骰子刷新商人"
            )
        }

        "Sami" -> {
            HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
            Text("萨米专用设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

            CheckBoxWithLabel(
                checked = config.firstFloorFoldartal,
                onCheckedChange = { onConfigChange(config.copy(firstFloorFoldartal = it)) },
                label = "凹第一层远见密文板，不进行作战"
            )

            AnimatedVisibility(visible = config.firstFloorFoldartal) {
                ITextField(
                    value = config.firstFloorFoldartals,
                    onValueChange = { onConfigChange(config.copy(firstFloorFoldartals = it)) },
                    placeholder = "密文板名称",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp)
                )
            }

            CheckBoxWithLabel(
                checked = config.newSquad2StartingFoldartal,
                onCheckedChange = { onConfigChange(config.copy(newSquad2StartingFoldartal = it)) },
                label = "生活队凹开局密文板"
            )

            AnimatedVisibility(visible = config.newSquad2StartingFoldartal) {
                Column(modifier = Modifier.padding(start = 24.dp)) {
                    ITextField(
                        value = config.newSquad2StartingFoldartals,
                        onValueChange = { onConfigChange(config.copy(newSquad2StartingFoldartals = it)) },
                        placeholder = "用英文分号 ; 隔开，最多三个",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // 通用高级设置
    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
    Text("通用高级设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    CheckBoxWithLabel(
        checked = config.delayAbortUntilCombatComplete,
        onCheckedChange = { onConfigChange(config.copy(delayAbortUntilCombatComplete = it)) },
        label = "战斗结束前延迟「停止」动作"
    )
}

// ========================================
// 按钮组组件 (替换下拉框)
// ========================================

/**
 * 通用按钮组组件
 * 使用 FlowRow 自动换行平铺显示选项
 * 样式与 FightConfigPanel 中的按钮组保持一致
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoguelikeButtonGroup(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { (value, displayName) ->
                val isSelected = value == selectedValue
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onValueChange(value) },
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.White else Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * 难度按钮组
 * 根据主题动态生成难度选项
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoguelikeDifficultyButtonGroup(
    label: String,
    selectedValue: Int,
    theme: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = RoguelikeConfig.getDifficultyOptions(theme)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { (value, displayName) ->
                val isSelected = value == selectedValue
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onValueChange(value) },
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.White else Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * 分队按钮组
 * 根据主题动态生成分队选项
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoguelikeSquadButtonGroup(
    label: String,
    selectedValue: String,
    theme: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = RoguelikeConfig.getSquadOptionsForTheme(theme)
    val displayValue = if (selectedValue.isEmpty()) options.firstOrNull() ?: "" else selectedValue

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { squad ->
                val isSelected = squad == displayValue
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onValueChange(squad) },
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = squad,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.White else Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
