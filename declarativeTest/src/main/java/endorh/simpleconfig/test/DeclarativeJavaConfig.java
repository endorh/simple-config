package endorh.simpleconfig.test;

import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.annotation.*;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;

import java.util.List;

import static java.util.Arrays.asList;

@ConfigClass(
  modId=SimpleConfigTestMod.MOD_ID, type=Type.CLIENT,
  background="textures/block/dark_oak_planks.png",
  color=0x8080FF80
)
public class DeclarativeJavaConfig {
	@Bind private static Icon getIcon() {
		return SimpleConfigIcons.Status.INFO;
	}
	
	@Entry public static String str = "Hello, World!";
	@Entry public static int num = 42;
	@Entry public static List<@Min(0) @Max(10) Integer> list = asList(0, 1, 2, 3);
	
	@Category(
	  background="textures/block/cobblestone.png",
	  color=0x808080FF
	) public static class TestCategory {
		@Entry public static String str = "Hello There!";
		@Entry public static @Min(0) @Max(2) @Slider(min=0, max=1) float slider = 0.5F;
	}
}
