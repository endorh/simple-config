package endorh.simplekonfig.konfig

import endorh.simpleconfig.api.SimpleConfig.Type
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons
import endorh.simpleconfig.konfig.SimpleKonfig
import endorh.simpleconfig.konfig.builders.*
import endorh.simpleconfig.konfig.category
import endorh.simpleconfig.konfig.group
import net.minecraft.world.item.Items
import java.awt.Color

// Entry builders are immutable, that is, all their methods return modified
//   copies, se you can reuse them without fear
private val letter = string().restrict("a", "b", "c", "d")

typealias C = ClientKonfig
object ClientKonfig : SimpleKonfig(
    Type.CLIENT, background = "textures/block/warped_planks.png"
) {
    val str by string("str").maxLength(10)
    val int by number(0)
    val baked by baked { int * 2 }
    val list by list(
        string(""), listOf("a", "b", "c", "d")
    ).caption(color(Color.BLUE))
    val map by map(
        pair(string(""), int()), triple(string(""), int(), regex())
    ).caption(pair(string(""), int()))
    val pair by pair(string(), int())
    val pairList by pairList(string(), int())
    val regex by regex("nice.*(?:regex)?").withOptions(RegexOption.IGNORE_CASE)
    
    val data by data(Data("<unnamed>", Color.BLUE, 10)) { bind {
        ::name caption string()
        ::color by color()
        ::number by baked { color.rgb }
        ::variable by string()
    }}
    
    object SubGroup : group(expand = true) {
        val caption by caption(number(0))
        val bool by yesNo(true)
        
        val letter1 by letter.withValue("b")
        val letter2 by letter.withValue("d")
        
        object SubSubGroup : group() {
            val item by item(Items.GOLDEN_APPLE)
        }
    }
    
    object SubCategory : category(
        background = "textures/block/bookshelf.png",
        color = 0xAA8080FF,
        icon = SimpleConfigIcons.Status.INFO
    ) {
        val bool by yesNo(true)
    }
    
    val number by int()
}

data class Data(
  val name: String,
  val color: Color,
  val number: Int
) {
    var variable = "$name+$number"
}