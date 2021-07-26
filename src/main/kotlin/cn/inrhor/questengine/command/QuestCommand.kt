package cn.inrhor.questengine.command

import cn.inrhor.questengine.common.quest.manager.QuestManager
import cn.inrhor.questengine.command.quest.QuestAccept
import cn.inrhor.questengine.command.quest.QuestFinish
import cn.inrhor.questengine.command.quest.QuestQuit
import io.izzel.taboolib.module.command.base.Argument
import io.izzel.taboolib.module.command.base.BaseSubCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import java.util.*

class QuestCommand: BaseSubCommand() {

    override fun getArguments() = arrayOf(
        Argument("@COMMAND.BASE.ARGUMENT", true) { arrayListOf("accept", "finish", "quit") },
        Argument("@COMMAND.QUEST.QUEST_ID", true) { QuestManager.questMap.map { it.key } },
        Argument("@COMMAND.BASE.PLAYER", true)
    )

    override fun onCommand(sender: CommandSender, command: Command, label : String, args: Array<out String>) {
        when (args[0].lowercase(Locale.getDefault())) {
            "accept" -> QuestAccept().onCommand(sender, args)
            "finish" -> QuestFinish().onCommand(sender, args)
            "quit" -> QuestQuit().onCommand(sender, args)
        }
        return
    }

}