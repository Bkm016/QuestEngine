package cn.inrhor.questengine.common.quest.manager

import cn.inrhor.questengine.api.quest.QuestInnerModule
import cn.inrhor.questengine.api.quest.QuestModule
import cn.inrhor.questengine.common.collaboration.TeamManager
import cn.inrhor.questengine.common.database.data.DataStorage
import cn.inrhor.questengine.common.database.data.PlayerData
import cn.inrhor.questengine.common.database.data.quest.*
import cn.inrhor.questengine.common.database.type.DatabaseManager
import cn.inrhor.questengine.common.database.type.DatabaseSQL
import cn.inrhor.questengine.common.database.type.DatabaseType
import cn.inrhor.questengine.common.quest.ModeType
import cn.inrhor.questengine.script.kether.KetherHandler
import cn.inrhor.questengine.common.quest.QuestState
import cn.inrhor.questengine.common.quest.QuestTarget
import io.izzel.taboolib.module.locale.TLocale
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.HashMap
import java.util.LinkedHashMap

object QuestManager {

    /**
     * 注册的任务模块内容
     */
    var questMap: HashMap<String, QuestModule> = LinkedHashMap()

    /**
     * 注册任务模块内容
     */
    fun register(questID: String, questModule: QuestModule) {
        questMap[questID] = questModule
    }

    /**
     * 得到任务模块内容
     */
    fun getQuestModule(questID: String): QuestModule? {
        return questMap[questID]
    }

    /**
     * 得到内部任务模块内容
     */
    fun getInnerQuestModule(questID: String, innerQuestID: String): QuestInnerModule? {
        val questModule = questMap[questID]?: return null
        questModule.innerQuestList.forEach {
            if (it.innerQuestID == innerQuestID) return it
        }
        return null
    }

    /**
     * 是否满足任务成员模式
     */
    fun matchQuestMode(questData: QuestData): Boolean {
        val questID = questData.questID
        val questModule = getQuestModule(questID)?: return false
        if (questModule.modeType == ModeType.PERSONAL) return true
        if (questModule.modeAmount <= 1) return true
        val tData = questData.teamData?: return false
        if (questModule.modeAmount >= TeamManager.getMemberAmount(tData)) return true
        return false
    }

    /**
     * 获取任务成员模式
     */
    fun getQuestMode(questID: String): ModeType {
        val questModule = getQuestModule(questID)?: return ModeType.PERSONAL
        return questModule.modeType
    }

    /**
     * 接受任务
     */
    fun acceptQuest(player: Player, questID: String) {
        val pData = DataStorage.getPlayerData(player)
        if (pData.questDataList.containsKey(questID)) {
            TLocale.sendTo(player, "QUEST.ALREADY_ACCEPT")
            return
        }
        val questModule = getQuestModule(questID) ?: return
        if (questModule.modeType == ModeType.COLLABORATION) {
            val tData = pData.teamData ?: return
            tData.members.forEach {
                val m = Bukkit.getPlayer(it) ?: return@forEach
                acceptQuest(m, questModule)
            }
            return
        }
        acceptQuest(player, questModule)
    }

    private fun acceptQuest(player: Player, questModule: QuestModule) {
        val startInnerQuest = questModule.getStartInnerQuest()?: return
        acceptInnerQuest(player, questModule.questID, startInnerQuest, true)
    }

    /**
     * 接受下一个内部任务
     *
     * 前提是已接受任务
     */
    private fun acceptNextInnerQuest(player: Player, questData: QuestData, innerQuestID: String) {
        val questID = questData.questID
        val questModule = getQuestModule(questID) ?: return
        if (questModule.modeType == ModeType.COLLABORATION) {
            val tData = questData.teamData?: return
            tData.members.forEach {
                val m = Bukkit.getPlayer(it)?: return@forEach
                nextInnerQuest(m, questData, innerQuestID)
            }
            return
        }
        nextInnerQuest(player, questData, innerQuestID)
    }

    private fun nextInnerQuest(player: Player, questData: QuestData, innerQuestID: String) {
        val questID = questData.questID
        val questInnerModule = getInnerQuestModule(questID, innerQuestID) ?: return
        val nextInnerID = questInnerModule.nextInnerQuestID
        val nextInnerModule = getInnerQuestModule(questID, nextInnerID) ?: return
        acceptInnerQuest(player, questID, nextInnerModule, false)
    }

    private fun acceptInnerQuest(player: Player, questID: String, innerQuest: QuestInnerModule, isNewQuest: Boolean) {
        val pData = DataStorage.getPlayerData(player)
        var state = QuestState.DOING
        if (isNewQuest && hasDoingInnerQuest(pData)) state = QuestState.IDLE
        val innerQuestID = innerQuest.innerQuestID
        val innerModule = getInnerQuestModule(questID, innerQuestID) ?: return
        val innerTargetData = getInnerModuleTargetMap(innerModule)
        val innerQuestData = QuestInnerData(questID, innerQuestID, innerTargetData, state)
        val questData = QuestData(questID, innerQuestData, state, pData.teamData, mutableListOf())
        pData.questDataList[questID] = questData
        saveControl(player, pData, innerQuestData)
        runControl(pData, questID, innerQuestID)
        if (isNewQuest) {
            if (DatabaseManager.type == DatabaseType.MYSQL) {
                DatabaseSQL().create(player, questData)
            }
        }
    }

    /**
     * 存储控制模块
     */
    fun saveControl(player: Player, pData: PlayerData, questInnerData: QuestInnerData) {
        if (questInnerData.state != QuestState.DOING) return
        val questID = questInnerData.questID
        val innerQuestID = questInnerData.innerQuestID
        val scriptList: MutableList<String>
        val controlID: String
        val mModule = getInnerQuestModule(questID, innerQuestID) ?: return
        val cModule = mModule.questControl
        controlID = cModule.controlID
        scriptList = cModule.scriptList
        if (controlID == "") return
        val controlData = QuestControlData(player, questInnerData, scriptList, 0, 0)
        pData.controlList[controlID] = controlData
    }

    fun generateControlID(questID: String, innerQuestID: String): String {
        return "[$questID]-[$innerQuestID]"
    }

    /**
     * 运行控制模块
     */
    fun runControl(player: Player, questID: String, innerQuestID: String) {
        val pData = DataStorage.getPlayerData(player)
        runControl(pData, questID, innerQuestID)
    }

    fun runControl(pData: PlayerData, questID: String, innerQuestID: String) {
        val id = generateControlID(questID, innerQuestID)
        val control = pData.controlList[id]?: return
        control.runScript()
    }

    /**
     * 检索是否已有处于 DOING 状态的内部任务
     */
    fun hasDoingInnerQuest(pData: PlayerData): Boolean {
        val questData = pData.questDataList
        questData.forEach { (_, u)->
            if (u.questInnerData.state == QuestState.DOING) return true
        }
        return false
    }

    /**
     * 结束任务，最终结束
     * 成功脚本在目标完成时运行
     *
     * @param state 设定任务成功与否
     * @param runFailReward 如果失败，是否执行当前内部任务失败脚本
     */
    fun endQuest(player: Player, questID: String, state: QuestState, runFailReward: Boolean) {
        val questData = getQuestData(player, questID) ?: return
        if (state == QuestState.FAILURE && runFailReward) {
            val innerQuestID = questData.questInnerData.innerQuestID
            val failReward = getReward(questID, innerQuestID, "", state) ?: return
            failReward.forEach {
                KetherHandler.eval(player, it)
            }
        }
    }

    /**
     * 结束当前内部任务，执行下一个内部任务或最终完成
     *
     * 最终完成请将 innerQuestID 设为 空
     */
    fun finishInnerQuest(player: Player, questID: String, innerQuestID: String) {
        val questData = getQuestData(player, questID) ?: return
        val questInnerModule = getInnerQuestModule(questID, innerQuestID) ?: return
        val nextInnerID = questInnerModule.nextInnerQuestID
        if (nextInnerID == "") {
            questData.state = QuestState.FINISH
            val questModule = getQuestModule(questID)?: return
            if (questModule.modeType == ModeType.COLLABORATION) {
                val tData = questData.teamData?: return
                tData.members.forEach {
                    val m = Bukkit.getPlayer(it)?: return@forEach
                    val mQuestData = getQuestData(m, questID)?: return@forEach
                    mQuestData.state = QuestState.FINISH
                }
            }
        }else {
            acceptNextInnerQuest(player, questData, nextInnerID)
        }
    }

    /**
     * 获得玩家任务数据
     */
    fun getQuestData(player: Player, questID: String): QuestData? {
        val pData = DataStorage.getPlayerData(player)
        return pData.questDataList[questID]
    }

    /**
     * 获得玩家当前内部任务数据
     */
    fun getInnerQuestData(player: Player, questID: String): QuestInnerData? {
        val questData = getQuestData(player, questID) ?: return null
        return questData.questInnerData
    }

    /**
     * 得到奖励脚本，成功与否
     * 成功的一般是在目标完成时得到
     */
    fun getReward(questID: String, innerQuestID: String, rewardID: String, type: QuestState): MutableList<String>? {
        val questModule = questMap[questID]!!
        for (m in questModule.innerQuestList) {
            if (m.innerQuestID == innerQuestID) {
                return if (type == QuestState.FINISH) {
                    m.questReward.finishReward[rewardID]!!
                }else m.questReward.failReward
            }
        }
        return null
    }

    /**
     * 获得触发的内部任务目标
     */
    fun getDoingTarget(player: Player, name: String): QuestTarget? {
        val questData = getDoingQuest(player) ?: return null
        val innerData = questData.questInnerData
        val targetData = innerData.targetsData[name]?: return null
        return targetData.questTarget
    }

    /**
     * 得到内部任务内容的任务目标，交给数据
     *
     * 此为初始值，可许更新
     */
    fun getInnerModuleTargetMap(innerModule: QuestInnerModule): MutableMap<String, TargetData> {
        val targetDataMap = mutableMapOf<String, TargetData>()
        innerModule.questTargetList.forEach { (name, questTarget) ->
            val targetData = TargetData(name, 0, 0, questTarget)
            targetDataMap[name] = targetData
        }
        return targetDataMap
    }

    /**
     * 获得正在进行中的任务
     */
    fun getDoingQuest(player: Player): QuestData? {
        val pData = DataStorage.getPlayerData(player)
        if (pData.questDataList.isEmpty()) return null
        pData.questDataList.forEach { (_, questData) ->
            if (questData.state == QuestState.DOING) {
                return questData
            }
        }
        return null
    }

    /**
     * 放弃和清空任务
     */
    fun quitQuest(player: Player, questID: String) {
        val uuid = player.uniqueId
        val pData = DataStorage.getPlayerData(uuid)
        val questData = pData.questDataList
        if (!questData.containsKey(questID)) return
        val questModule = getQuestModule(questID)?: return
        val qData = questData[questID]?: return
        if (questModule.modeType == ModeType.COLLABORATION) {
            val tData = qData.teamData?: run { questData.remove(questID); return }
            tData.members.forEach {
                if (uuid == it) return@forEach
                val mData = DataStorage.getPlayerData(it)
                val mQuestData = mData.questDataList
                if (!mQuestData.containsKey(questID)) return@forEach
                mQuestData.remove(questID)
            }
        }
        questData.remove(questID)
    }

}