package com.aliothmoon.maameow.data.model


import com.aliothmoon.maameow.maa.task.MaaTaskParams
import com.aliothmoon.maameow.maa.task.MaaTaskType
import com.aliothmoon.maameow.maa.task.TaskParamProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
/**
 * 基建换班配置
 *
 * 完整迁移自 WPF InfrastTask.cs 和 InfrastSettingsUserControlModel.cs
 * 支持常规模式（Normal）和队列轮换模式（Rotation）
 *
 * WPF 源文件:
 * - Model: InfrastTask.cs
 * - ViewModel: InfrastSettingsUserControlModel.cs
 * - View: InfrastSettingsUserControl.xaml
 */
@Serializable
data class InfrastConfig(
    // ============ 基建模式 ============

    /**
     * 基建切换模式
     * 对应 WPF: InfrastMode (enum)
     * - "Normal": 常规模式
     * - "Rotation": 队列轮换模式
     * 注意: 不包含 Custom 自定义模式（该模式暂不迁移）
     */
    val mode: String = "Normal",

    // ============ 设施列表（有序） ============

    /**
     * 基建设施优先级列表（有序）
     * 对应 WPF: InfrastItemViewModels (ObservableCollection<DragItemViewModel>)
     *
     * 设施名称（与WPF一致）:
     * - "Mfg": 制造站
     * - "Trade": 贸易站
     * - "Control": 控制中枢
     * - "Power": 发电站
     * - "Reception": 会客室
     * - "Office": 办公室
     * - "Dorm": 宿舍
     * - "Training": 训练室
     * - "Processing": 加工站
     *
     * 注意: 列表顺序代表换班优先级，支持拖拽调整
     */
    val facilities: List<String> = listOf(
        "Mfg", "Trade", "Control", "Power", "Reception",
        "Office", "Dorm", "Training", "Processing"
    ),

    // ============ 无人机用途 ============

    /**
     * 无人机使用方式
     * 对应 WPF: UsesOfDrones (string)
     *
     * 选项:
     * - "_NotUse": 不使用无人机
     * - "Money": 龙门币（制造站）
     * - "SyntheticJade": 合成玉（制造站）
     * - "CombatRecord": 作战记录（制造站）
     * - "PureGold": 赤金（贸易站）
     * - "OriginStone": 源石碎片（贸易站）
     * - "Chip": 芯片（贸易站）
     */
    val usesOfDrones: String = "Money",

    // ============ 心情阈值 ============

    /**
     * 宿舍心情阈值（百分比）
     * 对应 WPF: DormThreshold (int, 0-100%)
     *
     * 干员心情低于此值时将被替换下班休息
     * 范围: 0-100（百分比值）
     *
     * 注意:
     * - WPF中DormThreshold是百分比值(0-100)
     * - 传递给MAA Core时需要除以100转换为0.0-1.0浮点数
     * - Rotation模式下此参数不显示和使用
     */
    val dormThreshold: Int = 30,

    // ============ 高级设置 ============

    /**
     * 宿舍空位补信赖
     * 对应 WPF: DormTrustEnabled (bool)
     *
     * 启用后，宿舍有空位时会优先安排信赖未满的干员进入宿舍
     * 注意: Rotation模式下不显示此选项
     */
    val dormTrustEnabled: Boolean = false,

    /**
     * 不将已进驻干员放入宿舍
     * 对应 WPF: DormFilterNotStationedEnabled (bool)
     *
     * 启用后，已在其他设施工作的干员不会被安排进宿舍休息
     * 注意: Rotation模式下不显示此选项
     */
    val dormFilterNotStationedEnabled: Boolean = true,

    /**
     * 制造站搓玉自动补货
     * 对应 WPF: OriginiumShardAutoReplenishment (bool)
     *
     * 启用后，制造站合成玉生产线会自动补充原料（源石碎片）
     */
    val originiumShardAutoReplenishment: Boolean = true,

    /**
     * 会客室留言板领取信用
     * 对应 WPF: ReceptionMessageBoardReceive (bool)
     *
     * 启用后，会在会客室领取留言板的信用点
     */
    val receptionMessageBoardReceive: Boolean = true,

    /**
     * 会客室线索交流
     * 对应 WPF: ReceptionClueExchange (bool)
     *
     * 启用后，会自动进行线索交流
     */
    val receptionClueExchange: Boolean = true,

    /**
     * 会客室赠送线索
     * 对应 WPF: ReceptionSendClue (bool)
     *
     * 启用后，会自动向好友赠送线索
     */
    val receptionSendClue: Boolean = true,

    /**
     * 继续专精
     * 对应 WPF: ContinueTraining (bool)
     *
     * 启用后，技能专精完成后会继续进行下一个专精任务
     */
    val continueTraining: Boolean = false
) : TaskParamProvider {
    /**
     * 获取已启用的设施列表
     *
     * 注意: 当前数据模型中设施列表没有独立的"启用"标志，
     * 所有列表中的设施都视为已启用。
     * 如需支持单独禁用某些设施，需要改为 Map<String, Boolean> 结构
     */
    fun getEnabledFacilities(): List<String> = facilities

    /**
     * 将心情阈值转换为MAA Core需要的浮点数格式(0.0-1.0)
     */
    fun getDormThresholdAsFloat(): Double = dormThreshold / 100.0
    override fun toTaskParams(): MaaTaskParams {
        // 根据模式确定设施列表
        val facilityList = when (mode) {
            "Rotation" -> listOf("Mfg", "Trade", "Control", "Power", "Reception", "Office", "Dorm")
            else -> getEnabledFacilities()
        }

        // 根据模式确定心情阈值
        val threshold = when (mode) {
            "Rotation" -> 0.0  // Rotation 模式不使用心情阈值
            else -> getDormThresholdAsFloat()
        }

        val paramsJson = buildJsonObject {
            put("facility", JsonArray(facilityList.map { JsonPrimitive(it) }))
            put("drones", usesOfDrones)
            put("threshold", threshold)
            put("replenish", originiumShardAutoReplenishment)
        }

        return MaaTaskParams(MaaTaskType.INFRAST, paramsJson.toString())
    }
}