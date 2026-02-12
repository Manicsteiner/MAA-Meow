package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.data.resource.ItemInfo
import com.aliothmoon.maameow.data.resource.MaaResourceManager
import com.aliothmoon.maameow.data.resource.MaaResourceManager.StageGroup
import com.aliothmoon.maameow.data.resource.MaaResourceManager.StageItem
import com.aliothmoon.maameow.data.resource.StageAliasMapper
import com.aliothmoon.maameow.presentation.components.CheckBoxWithExpandableTip
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.INumericField
import com.aliothmoon.maameow.presentation.components.ITextFieldWithFocus
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipContent
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipIcon
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import timber.log.Timber
import java.time.LocalDate

/**
 * 刷理智配置面板
 *
 * 包含常规设置和高级设置两个Tab
 *
 * 功能特性:
 * 1. 常规设置:
 *    - 理智药/源石/次数控制
 *    - 指定材料掉落
 *    - 代理倍率选择
 *    - 关卡选择（支持备选关卡）
 *    - 剩余理智关卡
 *
 * 2. 高级设置:
 *    - 自定义剿灭
 *    - 博朗台模式
 *    - 自定义关卡代码
 *    - 使用备选关卡
 *    - 使用剩余理智关卡
 *    - 允许保存源石使用
 *    - 使用即将过期的理智药
 *    - 隐藏不可用关卡
 *    - 隐藏代理倍率
 *    - 游戏掉线时自动重连
 *
 * @param config 当前刷理智配置
 * @param onConfigChange 配置更改回调
 * @param modifier 修饰符
 */
@Composable
fun FightConfigPanel(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    // 获取 MaaResourceManager
    val context = LocalContext.current
    val resourceManager: MaaResourceManager = koinInject()

    // 使用合并后的关卡列表（活动关卡 + 常驻关卡）
    // stageGroups: 分组后的关卡列表（用于分组显示）
    // allStageItems: 扁平的关卡列表（用于剩余理智关卡选择）
    var stageGroups by remember { mutableStateOf<List<StageGroup>>(emptyList()) }
    var allStageItems by remember { mutableStateOf<List<StageItem>>(emptyList()) }
    var dropItemsList by remember { mutableStateOf<List<ItemInfo>>(emptyList()) }
    var isResourceCollectionOpen by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // 加载材料列表和关卡列表
        dropItemsList = resourceManager.getDropItems()
        allStageItems = resourceManager.getMergedStageList(filterByToday = false)
        isResourceCollectionOpen = resourceManager.isResourceCollectionOpen()

        // 调试日志
        val today = LocalDate.now().dayOfWeek
        Timber.d("=== 关卡列表加载 ===")
        Timber.d("今天是: $today")
        Timber.d("关卡总数: ${allStageItems.size}")
        Timber.d("资源收集活动开放: $isResourceCollectionOpen")
    }

    // 根据 hideUnavailableStage 加载分组列表
    LaunchedEffect(config.hideUnavailableStage) {
        stageGroups =
            resourceManager.getMergedStageGroups(filterByToday = config.hideUnavailableStage)
        Timber.d("hideUnavailableStage: ${config.hideUnavailableStage}")
        Timber.d("分组数: ${stageGroups.size}, 关卡总数: ${stageGroups.sumOf { it.stages.size }}")
    }

    // 过滤后的扁平关卡列表（用于主关卡选择）
    val filteredStageItems = remember(stageGroups) {
        stageGroups.flatMap { it.stages }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 4.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { 2 }
        )
        val coroutineScope = rememberCoroutineScope()

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
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                }
            )
            Text(
                text = "高级设置",
                style = MaterialTheme.typography.bodyMedium,
                color = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else Color.Gray,
                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                }
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(
                top = 2.dp,
                bottom = 4.dp
            )
        )

        // Tab 内容区
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
                modifier = Modifier.fillMaxSize()
            ) {
                when (page) {
                    // 常规设置 Tab
                    0 -> {
                        // 今日开放关卡提示
                        item {
                            TodayStagesHint(
                                stageGroups = stageGroups,
                                isResourceCollectionOpen = isResourceCollectionOpen
                            )
                        }
                        item {
                            // 理智药/源石/次数
                            MedicineAndStoneSection(config, onConfigChange)
                        }
                        item {
                            HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                        }
                        item {
                            // 指定材料掉落
                            SpecifiedDropsSection(config, onConfigChange, dropItemsList)
                        }
                        item {
                            HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                        }
                        // 代理倍率（HideSeries=false 时显示）
                        if (!config.hideSeries) {
                            item {
                                SeriesSection(config, onConfigChange)
                            }
                            item {
                                HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                            }
                        }
                        item {
                            // 关卡选择
                            // stageGroups: 分组后的关卡列表（用于分组显示）
                            // allStageItems: 完整列表（用于剩余理智关卡选择）
                            GroupedStageSelectionSection(
                                config = config,
                                onConfigChange = onConfigChange,
                                stageGroups = stageGroups,
                                allStageItems = allStageItems
                            )
                        }
                    }

                    // 高级设置 Tab
                    else -> {
                        item {
                            // 自定义剿灭
                            CustomAnnihilationSection(config, onConfigChange)
                        }
                        item {
                            // 博朗台模式
                            CheckBoxWithExpandableTip(
                                checked = config.isDrGrandet,
                                onCheckedChange = { onConfigChange(config.copy(isDrGrandet = it)) },
                                label = "博朗台模式",
                                tipText = "等待理智恢复后再开始行动，在理智即将溢出时开始作战（无限吃药时不生效）"
                            )
                        }
                        item {
                            // 自定义关卡代码
                            CheckBoxWithExpandableTip(
                                checked = config.customStageCode,
                                onCheckedChange = { onConfigChange(config.copy(customStageCode = it)) },
                                label = "自定义关卡代码",
                                tipText = "启用后可手动输入关卡代码，支持简写（如\"龙门币\"→\"CE-6\"）"
                            )
                        }
                        item {
                            // 使用备选关卡
                            CheckBoxWithExpandableTip(
                                checked = config.useAlternateStage,
                                onCheckedChange = {
                                    onConfigChange(
                                        config.copy(
                                            useAlternateStage = it,
                                            // 启用备选关卡时，自动禁用隐藏不可用关卡
                                            hideUnavailableStage = if (it) false else config.hideUnavailableStage
                                        )
                                    )
                                },
                                label = "使用备选关卡",
                                tipText = "首选关卡不可用时自动使用备选关卡"
                            )
                        }
                        item {
                            // 使用剩余理智关卡
                            CheckBoxWithExpandableTip(
                                checked = config.useRemainingSanityStage,
                                onCheckedChange = {
                                    onConfigChange(
                                        config.copy(
                                            useRemainingSanityStage = it
                                        )
                                    )
                                },
                                label = "使用剩余理智关卡",
                                tipText = "主要关卡刷完后，将剩余理智用于指定关卡"
                            )
                        }
                        item {
                            // 允许保存源石使用
                            AllowUseStoneSaveSection(config, onConfigChange)
                        }
                        item {
                            // 使用即将过期的理智药
                            CheckBoxWithExpandableTip(
                                checked = config.useExpiringMedicine,
                                onCheckedChange = { onConfigChange(config.copy(useExpiringMedicine = it)) },
                                label = "使用即将过期的理智药",
                                tipText = "优先使用48小时内过期的理智药"
                            )
                        }
                        item {
                            // 隐藏不可用关卡
                            CheckBoxWithExpandableTip(
                                checked = config.hideUnavailableStage,
                                onCheckedChange = {
                                    onConfigChange(
                                        config.copy(
                                            hideUnavailableStage = it,
                                            // 启用隐藏不可用关卡时，自动禁用使用备选关卡
                                            useAlternateStage = if (it) false else config.useAlternateStage
                                        )
                                    )
                                },
                                label = "隐藏不可用关卡",
                                tipText = "在关卡列表中隐藏当前不可用的关卡"
                            )
                        }
                        item {
                            // 隐藏代理倍率
                            CheckBoxWithLabel(
                                checked = config.hideSeries,
                                onCheckedChange = { onConfigChange(config.copy(hideSeries = it)) },
                                label = "隐藏代理倍率"
                            )
                        }
                        item {
                            // 游戏掉线时自动重连
                            CheckBoxWithExpandableTip(
                                checked = config.autoRestartOnDrop,
                                onCheckedChange = { onConfigChange(config.copy(autoRestartOnDrop = it)) },
                                label = "游戏掉线时自动重连",
                                tipText = "检测到游戏掉线时会自动尝试重连并继续作战"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 今日开放关卡提示
 * 显示当日开放的活动关卡和资源本
 */
@Composable
private fun TodayStagesHint(
    stageGroups: List<StageGroup>,
    isResourceCollectionOpen: Boolean
) {
    // 收集活动关卡分组（包含剩余天数）
    val activityGroups = stageGroups.filter { it.title != "常驻关卡" }

    val todayOpenStages = stageGroups
        .find { it.title == "常驻关卡" }
        ?.stages
        ?.filter { it.isOpenToday }
        ?: emptyList()

    // 如果没有活动关卡也没有今日开放的资源关卡，不显示
    if (activityGroups.isEmpty() && todayOpenStages.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val todayName = remember {
        when (java.time.LocalDate.now().dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "周一"
            java.time.DayOfWeek.TUESDAY -> "周二"
            java.time.DayOfWeek.WEDNESDAY -> "周三"
            java.time.DayOfWeek.THURSDAY -> "周四"
            java.time.DayOfWeek.FRIDAY -> "周五"
            java.time.DayOfWeek.SATURDAY -> "周六"
            java.time.DayOfWeek.SUNDAY -> "周日"
            else -> ""
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFE8F5E9),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日开放（$todayName）",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2E7D32)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 显示活动关卡提示（带剩余天数）
                    activityGroups.forEach { group ->
                        val daysText = group.daysLeftText?.let { " (剩余$it)" } ?: ""
                        Text(
                            text = "· ${group.title}$daysText",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE65100)  // 活动用橙色
                        )
                    }

                    // 显示资源收集活动提示
                    if (isResourceCollectionOpen) {
                        Text(
                            text = "· 资源收集活动进行中（资源本全开放）",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1976D2)  // 蓝色
                        )
                    }

                    // 显示今日开放的资源本
                    val resourceStages = todayOpenStages.filter {
                        it.code.startsWith("CE-") || it.code.startsWith("LS-") ||
                                it.code.startsWith("CA-") || it.code.startsWith("AP-") ||
                                it.code.startsWith("SK-") || it.code.startsWith("PR-")
                    }
                    if (resourceStages.isNotEmpty()) {
                        Text(
                            text = resourceStages.joinToString("  ") { it.displayName },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF388E3C)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 理智药/源石/次数区域
 */
@Composable
private fun MedicineAndStoneSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 使用理智药
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckBoxWithLabel(
                checked = config.useMedicine,
                onCheckedChange = {
                    onConfigChange(
                        config.copy(
                            useMedicine = it,
                            // 关闭理智药时，同时关闭源石
                            useStone = if (!it) false else config.useStone
                        )
                    )
                },
                label = "使用理智药",
                enabled = !config.useStone,  // 使用源石时禁用理智药
                modifier = Modifier.weight(1f)
            )
            INumericField(
                value = config.medicineNumber,
                onValueChange = { onConfigChange(config.copy(medicineNumber = it)) },
                minimum = 0,
                maximum = 999,
                enabled = config.useMedicine && !config.useStone,
                modifier = Modifier.width(80.dp)
            )
        }

        // 使用源石 TODO 暂时不支持使用
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            UseStoneSection(config, onConfigChange, modifier = Modifier.weight(1f))
//            INumericField(
//                value = config.stoneNumber,
//                onValueChange = { onConfigChange(config.copy(stoneNumber = it)) },
//                minimum = 0,
//                maximum = 999,
//                enabled = config.useStone,
//                modifier = Modifier.width(80.dp)
//            )
//        }

        // 战斗次数限制
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckBoxWithLabel(
                checked = config.hasTimesLimited,
                onCheckedChange = { onConfigChange(config.copy(hasTimesLimited = it)) },
                label = "限制战斗次数",
                modifier = Modifier.weight(1f)
            )
            INumericField(
                value = config.maxTimes,
                onValueChange = { onConfigChange(config.copy(maxTimes = it)) },
                minimum = 0,
                maximum = 999,
                enabled = config.hasTimesLimited,
                modifier = Modifier.width(80.dp)
            )
        }

        // 代理倍率整除警告
        val showSeriesWarning = config.hasTimesLimited &&
                config.series > 0 &&
                config.maxTimes % config.series != 0

        if (showSeriesWarning) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "战斗次数 ${config.maxTimes} 无法被代理倍率 ${config.series} 整除，可能无法完全消耗理智",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF57C00),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

/**
 * 指定材料掉落区域
 */
@Composable
private fun SpecifiedDropsSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit,
    dropItems: List<ItemInfo>
) {
    // 构建材料 ID 到名称的映射
    val itemNameMap = remember(dropItems) {
        dropItems.associate { it.id to it.name }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 启用指定掉落复选框
        CheckBoxWithExpandableTip(
            checked = config.isSpecifiedDrops,
            onCheckedChange = {
                onConfigChange(
                    config.copy(
                        isSpecifiedDrops = it,
                        // 取消时清空材料设置
                        dropsItemId = if (!it) "" else config.dropsItemId,
                        dropsQuantity = if (!it) 5 else config.dropsQuantity
                    )
                )
            },
            label = "指定材料掉落",
            tipText = "刷到指定数量的材料后停止作战"
        )

        // 材料选择和数量输入（启用后显示）
        if (config.isSpecifiedDrops) {
            // 材料选择说明提示
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "该选项不会自动计算最优关卡，请手动选择关卡",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF57C00),
                    modifier = Modifier.padding(8.dp)
                )
            }

            // 材料选择（平铺展示）
            val itemIds = if (dropItems.isNotEmpty()) {
                dropItems.map { it.id }
            } else {
                getDropItemsList()
            }

            ItemButtonGroup(
                label = "材料",
                selectedValue = config.dropsItemId,
                items = itemIds,
                onItemSelected = { onConfigChange(config.copy(dropsItemId = it)) },
                displayMapper = { id -> itemNameMap[id] ?: id }
            )

            // 材料数量
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "目标数量",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                INumericField(
                    value = config.dropsQuantity,
                    onValueChange = { onConfigChange(config.copy(dropsQuantity = it)) },
                    minimum = 1,
                    maximum = 1145141919,
                    modifier = Modifier.width(100.dp)
                )
            }
        }
    }
}

/**
 * 代理倍率选择区域
 * 使用 RadioButton 单选按钮组，FlowRow 自动换行
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeriesSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit
) {
    var tipExpanded by remember { mutableStateOf(false) }
    val seriesTipText = """
代理倍率设置说明：
• AUTO - 自动选择最优倍率
• 6~1 - 固定使用指定倍率（如6表示6倍速）
• 不切换 - 保持当前游戏设置

注意事项：
• 高倍率可能导致漏怪，建议难度较高的关卡使用低倍率
• 剿灭作战推荐使用 AUTO 或手动设置较低倍率
• 倍率设置仅影响代理指挥，不影响手动操作
    """.trimIndent()

    // 代理倍率选项 (对应WPF的SeriesList)
    val seriesOptions = listOf(
        0 to "AUTO",
        6 to "6",
        5 to "5",
        4 to "4",
        3 to "3",
        2 to "2",
        1 to "1",
        -1 to "不切换"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "代理倍率",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            ExpandableTipIcon(
                expanded = tipExpanded,
                onExpandedChange = { tipExpanded = it }
            )
        }

        ExpandableTipContent(
            visible = tipExpanded,
            tipText = seriesTipText
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            seriesOptions.forEach { (value, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .widthIn(min = 56.dp)
                        .clickable { onConfigChange(config.copy(series = value)) }
                ) {
                    RadioButton(
                        selected = config.series == value,
                        onClick = { onConfigChange(config.copy(series = value)) },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * 分组关卡选择区域（新版）
 * 支持活动关卡和常驻关卡分组显示
 *
 * @param stageGroups 分组后的关卡列表
 * @param allStageItems 完整关卡列表（用于剩余理智关卡选择）
 */
@Composable
private fun GroupedStageSelectionSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit,
    stageGroups: List<StageGroup>,
    allStageItems: List<StageItem>
) {
    var tipExpanded by remember { mutableStateOf(false) }
    val tipText = "可手动输入关卡代码，支持简写\n例如：龙门币→CE-6，经验→LS-6"

    // 扁平的关卡代码列表（用于输入框模式）
    val stageCodes = remember(stageGroups) {
        stageGroups.flatMap { group -> group.stages.map { it.code } }
    }

    // 完整关卡代码列表（用于剩余理智关卡，排除「当前/上次」选项）
    val allStageCodes = remember(allStageItems) {
        allStageItems.filter { it.code.isNotEmpty() }.map { it.code }
    }

    // 构建关卡代码到 StageItem 的映射
    val stageMap = remember(allStageItems) {
        allStageItems.associateBy { it.code }
    }

    // 检查关卡是否今日开放
    fun isStageOpenToday(stageCode: String): Boolean {
        if (stageCode.isBlank()) return true
        return stageMap[stageCode]?.isOpenToday ?: true
    }

    // 剩余理智关卡列表（使用完整列表 + "不使用"选项）
    // 注意：这里的空字符串表示「不使用」，与首选关卡的空字符串（当前/上次）含义不同
    val remainingStageCodes = remember(allStageCodes) {
        listOf("") + allStageCodes
    }

    // 关卡代码到显示名称的映射函数
    val stageDisplayMapper: (String) -> String = { code ->
        if (code.isEmpty()) {
            "不使用"
        } else {
            stageMap[code]?.displayName ?: code
        }
    }

    // 检查首选关卡开放状态
    val stage1Open = isStageOpenToday(config.stage1)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "关卡选择",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (config.customStageCode) {
                    ExpandableTipIcon(
                        expanded = tipExpanded,
                        onExpandedChange = { tipExpanded = it }
                    )
                }
            }
            if (config.customStageCode) {
                ExpandableTipContent(
                    visible = tipExpanded,
                    tipText = tipText
                )
            }
        }

        // 首选关卡
        if (config.customStageCode) {
            // 文本输入模式
            StageInputField(
                value = config.stage1,
                onValueChange = { onConfigChange(config.copy(stage1 = it)) },
                label = "首选关卡",
                placeholder = "例如: CE-6 或 龙门币",
                stageCodes = stageCodes
            )
        } else {
            // 分组按钮选择模式
            GroupedStageButtonGroup(
                label = "首选关卡",
                selectedValue = config.stage1,
                stageGroups = stageGroups,
                onItemSelected = { onConfigChange(config.copy(stage1 = it)) }
            )
        }

        // 首选关卡不开放时显示警告
        if (!stage1Open && config.stage1.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "「${config.stage1}」今日不开放" +
                            if (config.useAlternateStage) "，将使用备选关卡" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF57C00),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // 备选关卡（UseAlternateStage 启用时显示）
        if (config.useAlternateStage) {
            if (config.customStageCode) {
                StageInputField(
                    value = config.stage2,
                    onValueChange = { onConfigChange(config.copy(stage2 = it)) },
                    label = "备选关卡2",
                    placeholder = "例如: CE-5",
                    stageCodes = stageCodes
                )
                StageInputField(
                    value = config.stage3,
                    onValueChange = { onConfigChange(config.copy(stage3 = it)) },
                    label = "备选关卡3",
                    placeholder = "例如: CE-4",
                    stageCodes = stageCodes
                )
                StageInputField(
                    value = config.stage4,
                    onValueChange = { onConfigChange(config.copy(stage4 = it)) },
                    label = "备选关卡4",
                    placeholder = "例如: CE-3",
                    stageCodes = stageCodes
                )
            } else {
                GroupedStageButtonGroup(
                    label = "备选关卡2",
                    selectedValue = config.stage2,
                    stageGroups = stageGroups,
                    onItemSelected = { onConfigChange(config.copy(stage2 = it)) }
                )
                GroupedStageButtonGroup(
                    label = "备选关卡3",
                    selectedValue = config.stage3,
                    stageGroups = stageGroups,
                    onItemSelected = { onConfigChange(config.copy(stage3 = it)) }
                )
                GroupedStageButtonGroup(
                    label = "备选关卡4",
                    selectedValue = config.stage4,
                    stageGroups = stageGroups,
                    onItemSelected = { onConfigChange(config.copy(stage4 = it)) }
                )
            }
        }

        // 剩余理智关卡（UseRemainingSanityStage 启用时显示）
        if (config.useRemainingSanityStage) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "在完成主任务后刷取指定关卡，不使用理智药/碎石",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (config.customStageCode) {
                StageInputField(
                    value = config.remainingSanityStage,
                    onValueChange = { onConfigChange(config.copy(remainingSanityStage = it)) },
                    label = "剩余理智关卡",
                    placeholder = "留空表示不使用",
                    stageCodes = stageCodes
                )
            } else {
                // 剩余理智关卡使用扁平列表（带"不使用"选项）
                StageButtonGroup(
                    label = "剩余理智关卡",
                    selectedValue = config.remainingSanityStage,
                    items = remainingStageCodes,
                    onItemSelected = { onConfigChange(config.copy(remainingSanityStage = it)) },
                    displayMapper = stageDisplayMapper
                )
            }
        }
    }
}

/**
 * 分组关卡选择按钮组
 * 显示分组标题，每个分组下的关卡自动换行平铺
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupedStageButtonGroup(
    label: String,
    selectedValue: String,
    stageGroups: List<StageGroup>,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        // 显示每个分组
        stageGroups.forEach { group ->
            // 分组标题
            Text(
                text = group.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (group.title == "常驻关卡") Color(0xFF388E3C) else Color(0xFFE65100),
                modifier = Modifier.padding(top = 4.dp)
            )

            // 分组内的关卡（自动换行平铺）
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                group.stages.forEach { stage ->
                    val isSelected = stage.code == selectedValue
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onItemSelected(stage.code) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(
                            0xFFE0E0E0
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stage.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) Color.White else Color.DarkGray,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 自定义剿灭区域
 * 使用 RadioButton 按钮组替代下拉框
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomAnnihilationSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit
) {
    // 剿灭关卡选项
    val annihilationOptions = getAnnihilationStageList()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CheckBoxWithLabel(
            checked = config.useCustomAnnihilation,
            onCheckedChange = { onConfigChange(config.copy(useCustomAnnihilation = it)) },
            label = "使用自定义剿灭"
        )

        // 剿灭关卡选择（启用时显示）
        AnimatedVisibility(
            visible = config.useCustomAnnihilation,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "剿灭关卡",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    annihilationOptions.forEach { (displayName, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onConfigChange(config.copy(annihilationStage = value)) }
                        ) {
                            RadioButton(
                                selected = config.annihilationStage == value,
                                onClick = { onConfigChange(config.copy(annihilationStage = value)) },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 允许保存源石使用区域
 * 使用内嵌式确认面板替代 AlertDialog（悬浮窗不支持 Dialog）
 */
@Composable
private fun AllowUseStoneSaveSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit
) {
    var showWarningPanel by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CheckBoxWithLabel(
            checked = config.allowUseStoneSave,
            onCheckedChange = { checked ->
                if (checked) {
                    // 启用前显示警告面板
                    showWarningPanel = true
                } else {
                    onConfigChange(config.copy(allowUseStoneSave = false))
                }
            },
            label = "允许保存源石使用"
        )

        // 内嵌式警告确认面板
        AnimatedVisibility(
            visible = showWarningPanel,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE57373))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "警告",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    Text(
                        text = "启用此选项后，源石使用设置将被保存。\n这可能导致意外消耗源石，请谨慎操作！",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        OutlinedButton(
                            onClick = { showWarningPanel = false },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Text("取消", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                onConfigChange(config.copy(allowUseStoneSave = true))
                                showWarningPanel = false
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            )
                        ) {
                            Text("确认启用", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 获取掉落材料列表（硬编码兜底）
 * 动态资源不可用时使用
 */
private fun getDropItemsList(): List<String> = listOf(
    "30011", "30012", "30013", "30014",  // 技能类材料
    "30021", "30022", "30023", "30024",
    "30031", "30032", "30033", "30034",
    "30041", "30042", "30043", "30044",
    "30051", "30052", "30053", "30054",
    "30061", "30062", "30063", "30064"
)

/**
 * 获取剿灭关卡列表
 */
private fun getAnnihilationStageList(): List<Pair<String, String>> = listOf(
    "剿灭" to "Annihilation",
    "切尔诺伯格" to "Chernobog@Annihilation",
    "龙门外环" to "LungmenOutskirts@Annihilation",
    "龙门市区" to "LungmenDowntown@Annihilation"
)

/**
 * 使用源石区域
 * 带小i图标展开提示（未保存设置警告）
 */
@Composable
private fun UseStoneSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var tipExpanded by remember { mutableStateOf(false) }
    val tipText = "此设置不会被保存，下次启动时将重置为关闭状态"

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CheckBoxWithLabel(
                checked = config.useStone,
                onCheckedChange = {
                    onConfigChange(
                        config.copy(
                            useStone = it,
                            // 启用源石时，理智药自动设为 999
                            medicineNumber = if (it) 999 else config.medicineNumber,
                            useMedicine = if (it) true else config.useMedicine
                        )
                    )
                },
                label = "使用源石"
            )
            // 未保存设置时显示小i图标
            if (!config.allowUseStoneSave) {
                ExpandableTipIcon(
                    expanded = tipExpanded,
                    onExpandedChange = { tipExpanded = it }
                )
            }
        }
        // 未保存设置的警告提示
        ExpandableTipContent(
            visible = tipExpanded && !config.allowUseStoneSave,
            tipText = tipText,
            modifier = Modifier.padding(start = 32.dp)
        )
    }
}

/**
 * 关卡代码输入框
 * 支持别名自动映射：失去焦点时自动转换别名为实际关卡代码
 *
 * 例如：龙门币 → CE-6，经验 → LS-6
 *
 * 使用 AdaptiveTextFieldWithFocus 自动适配悬浮窗/普通环境
 */
@Composable
private fun StageInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    stageCodes: List<String>,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { mutableStateOf(value) }
    var showConvertedHint by remember { mutableStateOf(false) }
    var convertedCode by remember { mutableStateOf("") }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ITextFieldWithFocus(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                // 检查是否是已知别名，显示转换提示
                val mapped = StageAliasMapper.mapToStageCode(newValue, stageCodes)
                if (mapped != newValue.uppercase() && newValue.isNotBlank()) {
                    showConvertedHint = true
                    convertedCode = mapped
                } else {
                    showConvertedHint = false
                }
            },
            onFocusLost = {
                if (textValue.isNotBlank()) {
                    // 失去焦点时应用别名映射
                    val mapped = StageAliasMapper.mapToStageCode(textValue, stageCodes)
                    textValue = mapped
                    onValueChange(mapped)
                    showConvertedHint = false
                }
            },
            label = label,
            placeholder = placeholder,
            singleLine = true,
            supportingText = if (showConvertedHint) {
                { Text("将转换为: $convertedCode", color = MaterialTheme.colorScheme.primary) }
            } else null
        )
    }
}

/**
 * 关卡选择按钮组
 * 使用 FlowRow 自动换行平铺显示关卡选项
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StageButtonGroup(
    label: String,
    selectedValue: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    displayMapper: (String) -> String = { it },
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                val isSelected = item == selectedValue
                val displayText = displayMapper(item)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onItemSelected(item) },
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = displayText,
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
 * 材料选择按钮组
 * 使用 FlowRow 自动换行平铺显示材料选项
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemButtonGroup(
    label: String,
    selectedValue: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    displayMapper: (String) -> String = { it },
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                val isSelected = item == selectedValue
                val displayText = displayMapper(item)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onItemSelected(item) },
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.White else Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
