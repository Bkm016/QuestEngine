package cn.inrhor.questengine.command.collaboration

import cn.inrhor.questengine.common.collaboration.TeamManager
import cn.inrhor.questengine.common.collaboration.ui.chat.HasTeam
import cn.inrhor.questengine.common.collaboration.ui.chat.NoTeam
import org.bukkit.entity.Player
import taboolib.common.platform.subCommand

object TeamOpen {

    val open = subCommand {
        execute<Player> { sender, _, _ ->
            val pUUID = sender.uniqueId
            val player = sender as Player
            player.sendMessage("command open")
            if (TeamManager.hasTeam(pUUID)) {
                HasTeam.openInfo(player)
                return@execute
            }
            NoTeam.openHome(player)
        }
    }
}