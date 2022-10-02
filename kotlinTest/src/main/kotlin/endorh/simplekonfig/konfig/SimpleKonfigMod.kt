package endorh.simplekonfig.konfig

import endorh.simpleconfig.konfig.SimpleKonfig
import net.minecraftforge.fml.common.Mod

@Mod(SimpleKonfigMod.MOD_ID)
object SimpleKonfigMod {
    const val MOD_ID = "simplekonfig"
    
    init {
        SimpleKonfig.buildAndRegister(ClientKonfig)
    }
}