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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.data.model.InfrastConfig
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipContent
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipIcon
import kotlinx.coroutines.launch

/**
 * 基建换班配置面板
 *
 * 包含常规设置和高级设置两个Tab，支持Normal和Rotation模式
 *
 * 功能特性:
 * 1. 常规设置:
 *    - 基建模式选择 (Normal/Rotation)
 *    - 无人机用途下拉框
 *    - 心情阈值滑块 (仅Normal模式显示)
 *    - 设施优先级列表 (支持拖拽排序)
 *    - 全选/清除按钮
 *
 * 2. 高级设置:
 *    - 宿舍信赖模式 (仅Normal模式显示)
 *    - 不将已进驻干员放入宿舍 (仅Normal模式显示)
 *    - 制造站搓玉自动补货
 *    - 会客室留言板领取信用
 *    - 会客室线索交流
 *    - 会客室赠送线索
 *    - 继续专精
 *
 * @param config 当前基建配置
 * @param onConfigChange 配置更改回调
 * @param modifier 修饰符
 */
@Composable
fun InfrastConfigPanel(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PaddingValues(start = 12.dp, top = 2.dp, bottom = 4.dp)),
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
                top = 4.dp,
                bottom = 8.dp
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
                        item {
                            // 基建模式选择
                            InfrastModeSection(config, onConfigChange)
                        }
                        item {
                            // 无人机用途
                            UsesOfDronesSection(config, onConfigChange)
                        }
                        item {
                            // 心情阈值 (仅 Normal 模式显示)
                            AnimatedVisibility(
                                visible = config.mode != "Rotation",
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                DormThresholdSection(config, onConfigChange)
                            }
                        }
                        item {
                            // 设施列表
                            FacilitiesSection(config, onConfigChange)
                        }
                    }

                    // 高级设置 Tab
                    else -> {
                        item {
                            // 宿舍信赖模式 (仅 Normal 模式显示)
                            AnimatedVisibility(
                                visible = config.mode != "Rotation",
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                DormTrustEnabledSection(config, onConfigChange)
                            }
                        }
                        item {
                            // 不将已进驻干员放入宿舍 (仅 Normal 模式显示)
                            AnimatedVisibility(
                                visible = config.mode != "Rotation",
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                DormFilterNotStationedSection(config, onConfigChange)
                            }
                        }
                        item {
                            // 制造站搓玉自动补货
                            OriginiumShardAutoReplenishmentSection(config, onConfigChange)
                        }
                        item {
                            // 会客室留言板领取信用
                            ReceptionMessageBoardReceiveSection(config, onConfigChange)
                        }
                        item {
                            // 会客室线索交流
                            ReceptionClueExchangeSection(config, onConfigChange)
                        }
                        item {
                            // 会客室赠送线索
                            ReceptionSendClueSection(config, onConfigChange)
                        }
                        item {
                            // 继续专精
                            ContinueTrainingSection(config, onConfigChange)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 基建模式选择区域
 * 使用 RadioButton 单选按钮组
 */
@Composable
private fun InfrastModeSection(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit
) {
    // 模式选项 (对应WPF的InfrastModeList)
    val modeOptions = listOf(
        "Normal" to "常规模式",
        "Rotation" to "轮换模式"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "基建模式",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modeOptions.forEach { (value, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = config.mode == value,
                        onClick = { onConfigChange(config.copy(mode = value)) },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Rotation 模式提示文字
        AnimatedVisibility(
            visible = config.mode == "Rotation",
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "轮换模式会自动轮换制造站和贸易站的工作内容，\n确保龙门币和赤金的均衡生产。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

/**
 * 无人机用途选择区域
 * 使用 RadioButton 单选按钮组，FlowRow 自动换行
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UsesOfDronesSection(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit
) {
    // 无人机用途选项 (对应WPF的UsesOfDronesList)
    val dronesOptions = listOf(
        "_NotUse" to "不使用",
        "Money" to "龙门币",
        "SyntheticJade" to "合成玉",
        "CombatRecord" to "作战记录",
        "PureGold" to "赤金",
        "OriginStone" to "源石碎片",
        "Chip" to "芯片"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "无人机用途",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            dronesOptions.forEach { (value, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .widthIn(min = 80.dp)
                        .clickable { onConfigChange(config.copy(usesOfDrones = value)) }
                ) {
                    RadioButton(
                        selected = config.usesOfDrones == value,
                        onClick = { onConfigChange(config.copy(usesOfDrones = value)) },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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
 * 心情阈值设置区域
 */
@Composable
private fun DormThresholdSection(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit
) {
    var tipExpanded by remember { mutableStateOf(false) }
    val tipText = "干员心情低于此值时将被替换下班休息\n阈值范围: 0-100%"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "宿舍心情阈值",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                ExpandableTipIcon(
                    expanded = tipExpanded,
                    onExpandedChange = { tipExpanded = it }
                )
            }
            Text(
                text = "${config.dormThreshold}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        ExpandableTipContent(
            visible = tipExpanded,
            tipText = tipText
        )

        Slider(
            value = config.dormThreshold.toFloat(),
            onValueChange = { onConfigChange(config.copy(dormThreshold = it.toInt())) },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 设施列表区域
 *
 * TODO: 拖拽排序功能暂不支持，需要引入 reorderable 库实现
 */
@Composable
private fun FacilitiesSection(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit
) {
    // 所有可用设施（固定顺序，用于展示）
    val allFacilities = listOf(
        "Mfg", "Trade", "Control", "Power", "Reception",
        "Office", "Dorm", "Training", "Processing"
    )

    var tipExpanded by remember { mutableStateOf(false) }
    val tipText = "勾选需要换班的设施\n设施顺序代表换班优先级"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "基建设施",
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
            tipText = tipText
        )

        // 设施列表（支持勾选）
        FacilityList(
            allFacilities = allFacilities,
            enabledFacilities = config.facilities,
            onFacilityToggle = { facility, enabled ->
                val newList = if (enabled) {
                    config.facilities + facility
                } else {
                    config.facilities - facility
                }
                onConfigChange(config.copy(facilities = newList))
            }
        )

        // 全选/清除按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onConfigChange(config.copy(facilities = allFacilities))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("全选")
            }

            OutlinedButton(
                onClick = {
                    onConfigChange(config.copy(facilities = emptyList()))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("清除")
            }
        }

        // 提示文字
        Text(
            text = "* 拖拽排序功能将在后续版本实现",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

/**
 * 设施列表展示（支持勾选）
 *
 * TODO: 后续版本需要支持拖拽排序，调整设施换班优先级
 *
 * @param allFacilities 所有可用设施列表
 * @param enabledFacilities 当前启用的设施列表
 * @param onFacilityToggle 设施勾选状态变化回调
 */
@Composable
private fun FacilityList(
    allFacilities: List<String>,
    enabledFacilities: List<String>,
    onFacilityToggle: (String, Boolean) -> Unit
) {
    // 设施显示名称映射
    val facilityDisplayNames = mapOf(
        "Mfg" to "制造站",
        "Trade" to "贸易站",
        "Control" to "控制中枢",
        "Power" to "发电站",
        "Reception" to "会客室",
        "Office" to "办公室",
        "Dorm" to "宿舍",
        "Training" to "训练室",
        "Processing" to "加工站"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFF5F5F5),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            allFacilities.forEach { facility ->
                val isEnabled = enabledFacilities.contains(facility)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFacilityToggle(facility, !isEnabled) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isEnabled,
                        onCheckedChange = { onFacilityToggle(facility, it) },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = facilityDisplayNames[facility] ?: facility,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * 宿舍信赖模式（仅Normal模式显示）
 */
@Composable
private fun DormTrustEnabledSection(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = config.dormTrustEnabled,
            onCheckedChange = { onConfigChange(config.copy(dormTrustEnabled = it)) },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "宿舍空余位置蹭信赖",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 不将已进驻干员放入宿舍（仅Normal模式显示）
 */
@Composable
private fun DormFilterNotStationedSection(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit
) {
    var tipExpanded by remember { mutableStateOf(false) }
    val tipText = "启用后，已在其他设施工作的干员\n不会被安排进宿舍休息"

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = config.dormFilterNotStationedEnabled,
                onCheckedChange = { onConfigChange(config.copy(dormFilterNotStationedEnabled = it)) },
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "不将已进驻的干员放入宿舍",
                style = MaterialTheme.typography.bodyMedium
            )
            ExpandableTipIcon(
                expanded = tipExpanded,
                onExpandedChange = { tipExpanded = it }
            )
        }
        ExpandableTipContent(
            visible = tipExpanded,
            tipText = tipText,
            modifier = Modifier.padding(start = 28.dp)
        )
    }
}

/**
 * 制造站搓玉自动补货
 */
@Composable
private fun OriginiumShardAutoReplenishmentSection(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = config.originiumShardAutoReplenishment,
            onCheckedChange = { onConfigChange(config.copy(originiumShardAutoReplenishment = it)) },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "源石碎片自动补货",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}

/**
 * 会客室留言板领取信用
 */
@Composable
private fun ReceptionMessageBoardReceiveSection(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = config.receptionMessageBoardReceive,
            onCheckedChange = { onConfigChange(config.copy(receptionMessageBoardReceive = it)) },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "会客室信息板收取信用",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}

/**
 * 会客室线索交流
 */
@Composable
private fun ReceptionClueExchangeSection(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = config.receptionClueExchange,
            onCheckedChange = { onConfigChange(config.copy(receptionClueExchange = it)) },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "进行线索交流",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}

/**
 * 会客室赠送线索
 */
@Composable
private fun ReceptionSendClueSection(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = config.receptionSendClue,
            onCheckedChange = { onConfigChange(config.copy(receptionSendClue = it)) },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "赠送线索",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}

/**
 * 继续专精
 */
@Composable
private fun ContinueTrainingSection(
    config: InfrastConfig,
    onConfigChange: (InfrastConfig) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = config.continueTraining,
            onCheckedChange = { onConfigChange(config.copy(continueTraining = it)) },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "训练完成后继续尝试专精当前技能",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}
