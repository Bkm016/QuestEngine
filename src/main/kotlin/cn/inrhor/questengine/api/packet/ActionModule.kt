package cn.inrhor.questengine.api.packet

class ActionModule(val type: PacketActionType, val set: MutableList<String>, val trigger: MutableList<String>, val pass: MutableList<String>,
        val ratioEnable: Boolean) {
}

enum class PacketActionType {
    COLLECTION
}

fun String.toPacketAction(): PacketActionType {
    return when (this.uppercase()) {
        else -> PacketActionType.COLLECTION
    }
}