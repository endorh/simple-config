package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

public interface ColorEntryBuilder
  extends ConfigEntryBuilder<@NotNull Color, String, Integer, ColorEntryBuilder>,
          AtomicEntryBuilder {
	/**
	 * Allow transparent colors.
	 */
	@Contract(pure=true) @NotNull ColorEntryBuilder alpha();
	
	/**
	 * Configure if transparent colors are allowed.
	 */
	@Contract(pure=true) @NotNull ColorEntryBuilder alpha(boolean hasAlpha);
}
