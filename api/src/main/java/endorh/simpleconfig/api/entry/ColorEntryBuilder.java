package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;

import java.awt.Color;

public interface ColorEntryBuilder
  extends ConfigEntryBuilder<Color, String, Integer, ColorEntryBuilder>, KeyEntryBuilder<Integer> {
	@Contract(pure=true) ColorEntryBuilder alpha();
	
	@Contract(pure=true) ColorEntryBuilder alpha(boolean hasAlpha);
}
