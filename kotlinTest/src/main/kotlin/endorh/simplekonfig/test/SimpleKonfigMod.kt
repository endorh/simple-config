package endorh.simplekonfig.test

import endorh.simpleconfig.konfig.SimpleKonfig
import net.minecraftforge.fml.common.Mod

@Mod(SimpleKonfigMod.MOD_ID)
object SimpleKonfigMod {
    const val MOD_ID = "simplekonfig"
    
    init {
        SimpleKonfig.registerConfig(ClientKonfig)
    }
}