package cn.inrhor.questengine.utlis.location

/**
 * @param long 距离
 * @param boxY 显示物品的高度
 */
class FixedHoloHitBox(val offset: Float, val multiply: Double, val height: Double,
                      val minX: Double,
                      val maxX: Double,
                      val minY: Double,
                      val maxY: Double,
                      val minZ: Double,
                      val maxZ: Double,
                      val long: Int,
                      val itemID: String,
                      val boxY: Double) {
}