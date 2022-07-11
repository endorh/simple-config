package endorh.simpleconfig.ui.api;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class EntryFlag implements Comparable<EntryFlag> {
	public static final EntryFlag REQUIRES_RESTART = new EntryFlag(
	  10, SimpleConfigIcons.Entries.REQUIRES_RESTART, () -> Lists.newArrayList(
		 new TranslationTextComponent("simpleconfig.config.help.requires_restart")));
	public static final EntryFlag NON_PERSISTENT = new EntryFlag(
	  20, SimpleConfigIcons.Entries.NOT_PERSISTENT, () -> Lists.newArrayList(
		 new TranslationTextComponent("simpleconfig.config.help.not_persistent_entry")));
	
	private static int STAMP;
	private final int id = STAMP++;
	public final int order;
	public final Icon icon;
	public final Supplier<List<ITextComponent>> tooltip;
	
	public EntryFlag(int order, Icon icon, Supplier<List<ITextComponent>> tooltip) {
		this.order = order;
		this.icon = icon;
		this.tooltip = tooltip;
	}
	
	@Override public int compareTo(@NotNull EntryFlag o) {
		return new CompareToBuilder()
		  .append(order, o.order)
		  .append(id, o.id).build();
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EntryFlag entryFlag = (EntryFlag) o;
		return order == entryFlag.order && Objects.equals(icon, entryFlag.icon) &&
		       Objects.equals(tooltip, entryFlag.tooltip);
	}
	
	@Override public int hashCode() {
		return Objects.hash(order, icon, tooltip);
	}
}
