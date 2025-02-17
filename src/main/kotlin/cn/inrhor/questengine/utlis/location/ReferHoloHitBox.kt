package cn.inrhor.questengine.utlis.location

import cn.inrhor.questengine.common.dialog.animation.item.ItemDialogPlay

/**
 * @param long 距离
 * @param boxY 显示物品的高度
 */
class ReferHoloHitBox(val offset: Float, val multiply: Double, val height: Double,
                      val minX: Double,
                      val maxX: Double,
                      val minY: Double,
                      val maxY: Double,
                      val minZ: Double,
                      val maxZ: Double,
                      val long: Int,
                      val itemID: String,
                      val type: ItemDialogPlay.Type,
                      val boxY: Double) {
}