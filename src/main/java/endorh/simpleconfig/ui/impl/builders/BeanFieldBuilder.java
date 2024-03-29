package endorh.simpleconfig.ui.impl.builders;

import com.google.common.collect.Maps;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.core.entry.BeanProxy;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.BeanListEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class BeanFieldBuilder<B> extends FieldBuilder<B, BeanListEntry<B>, BeanFieldBuilder<B>> {
	private final BeanProxy<B> proxy;
	protected final Map<String, FieldBuilder<?, ?, ?>> entries = Maps.newLinkedHashMap();
	protected @Nullable String caption;
	protected @Nullable Function<B, Icon> iconProvider;
	protected @Nullable Boolean overrideEquals;

	public BeanFieldBuilder(
	  BeanProxy<B> proxy, ConfigFieldBuilder builder, Component name, B value
	) {
		super(BeanListEntry.class, builder, name, value);
		this.proxy = proxy;
	}
	
	public BeanFieldBuilder<B> add(String name, FieldBuilder<?, ?, ?> builder) {
		entries.put(name, builder);
		return this;
	}
	
	public <
	  T, E extends AbstractConfigListEntry<T> & IChildListEntry,
	  F extends FieldBuilder<T, E, F>
	> BeanFieldBuilder<B> caption(String name, F builder) {
		add(name, builder);
		caption = name;
		return this;
	}
	
	public BeanFieldBuilder<B> withoutCaption() {
		caption = null;
		return this;
	}
	
	public BeanFieldBuilder<B> withIcon(@Nullable Function<B, Icon> iconProvider) {
		this.iconProvider = iconProvider;
		return this;
	}
	
	public BeanFieldBuilder<B> withIcon(@Nullable Icon icon) {
		return withIcon(icon != null? b -> icon : null);
	}

	public BeanFieldBuilder<B> overrideEquals(@Nullable Boolean overrideEquals) {
		this.overrideEquals = overrideEquals;
		return this;
	}

	public BeanFieldBuilder<B> overrideEquals() {
		return overrideEquals(true);
	}
	
	@Override protected BeanListEntry<B> buildEntry() {
		LinkedHashMap<String, AbstractConfigListEntry<?>> entries = new LinkedHashMap<>();
		this.entries.forEach((name, b) -> entries.put(name, b.build()));
		BeanListEntry<B> entry = new BeanListEntry<>(fieldNameKey, value, proxy, entries, caption, iconProvider);
		if (overrideEquals != null) entry.setOverrideEquals(overrideEquals);
		return entry;
	}
}
