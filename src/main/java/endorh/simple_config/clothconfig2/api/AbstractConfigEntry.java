package endorh.simple_config.clothconfig2.api;

import com.google.common.collect.Lists;
import com.ibm.icu.impl.Pair;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.AbstractConfigScreen;
import endorh.simple_config.clothconfig2.gui.entries.BaseListEntry;
import endorh.simple_config.clothconfig2.gui.widget.DynamicElementListWidget;
import endorh.simple_config.clothconfig2.gui.widget.DynamicEntryListWidget.INavigableTarget;
import endorh.simple_config.clothconfig2.impl.EditHistory;
import endorh.simple_config.clothconfig2.impl.ISeekableComponent;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.min;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractConfigEntry<T>
  extends DynamicElementListWidget.ElementEntry
  implements ReferenceProvider {
	private static final Logger LOGGER = LogManager.getLogger();
	private WeakReference<AbstractConfigScreen> screen = new WeakReference<>(null);
	protected @Nullable BaseListEntry<?, ?, ?> listParent = null;
	@NotNull private Supplier<T> defaultSupplier = () -> null;
	@Nullable private Supplier<Optional<ITextComponent>> errorSupplier = null;
	@Nullable private List<ReferenceProvider> referencableEntries = null;
	@Nullable protected Consumer<T> saveConsumer = null;
	@Nullable protected T original = null;
	protected boolean ignoreEdits = false;
	protected Supplier<Boolean> editableSupplier;
	
	protected String category = null;
	protected AbstractConfigEntry<?> parentEntry = null;
	protected String name = "";
	protected long lastHistoryTime;
	protected boolean lastHistoryError = false;
	protected int historyApplyColor = 0x804242FF;
	protected int historyErrorColor = 0x80FF4242;
	
	// Search
	protected String matchedText = null;
	protected String matchedValueText = null;
	protected boolean focusedMatch = false;
	protected boolean isSelected = false;
	
	protected int matchColor = 0x42ffff42;
	protected int focusedMatchColor = 0x80ffBD42;
	protected WeakReference<IExpandable> expandableParent = new WeakReference<>(null);
	protected @Nullable INavigableTarget navigableParent = null;
	
	public final void setReferenceProviderEntries(
	  @Nullable List<ReferenceProvider> referencableEntries
	) {
		this.referencableEntries = referencableEntries;
	}
	
	public void requestReferenceRebuilding() {
		final AbstractConfigScreen screen = this.screen.get();
		if (screen instanceof ReferenceBuildingConfigScreen)
			((ReferenceBuildingConfigScreen) screen).requestReferenceRebuilding();
	}
	
	@Override @NotNull public AbstractConfigEntry<?> provideReferenceEntry() {
		return this;
	}
	
	@Nullable @Internal public final List<ReferenceProvider> getReferenceProviderEntries() {
		return this.referencableEntries;
	}
	
	public String getPath() {
		if (parentEntry != null) return parentEntry.getPath() + "." + name;
		return category + ":" + name;
	}
	
	public String getCategory() {
		if (category != null) return category;
		if (parentEntry != null) return parentEntry.getCategory();
		if (listParent != null) return listParent.getCategory();
		return "";
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setParentEntry(AbstractConfigEntry<?> parentEntry) {
		this.parentEntry = parentEntry;
	}
	
	public void setCategory(ConfigCategory category) {
		this.category = category.getName();
	}
	
	public abstract boolean isRequiresRestart();
	
	public abstract void setRequiresRestart(boolean requiresRestart);
	
	public boolean isSelected() {
		return isSelected;
	}
	
	public void setExpandableParent(IExpandable parent) {
		expandableParent = new WeakReference<>(parent);
	}
	
	public @Nullable BaseListEntry<?, ?, ?> getListParent() {
		return listParent;
	}
	
	public void setListParent(@Nullable BaseListEntry<?, ?, ?> listParent) {
		this.listParent = listParent;
	}
	
	public @Nullable IExpandable getExpandableParent() {
		return expandableParent.get();
	}
	
	@Internal protected void doExpandParents() {
		final IExpandable parent = expandableParent.get();
		if (this instanceof IExpandable)
			((IExpandable) this).setExpanded(true);
		if (parent instanceof AbstractConfigEntry) {
			((AbstractConfigEntry<?>) parent).doExpandParents();
		} else if (parent != null) parent.setExpanded(true);
	}
	
	public void expandParents() {
		final IExpandable parent = expandableParent.get();
		if (parent != null) {
			if (parent instanceof AbstractConfigEntry)
				((AbstractConfigEntry<?>) parent).doExpandParents();
			else parent.setExpanded(true);
		}
	}
	
	public abstract ITextComponent getFieldName();
	
	public ITextComponent getDisplayedFieldName() {
		boolean hasError = this.getConfigError().isPresent();
		boolean isEdited = this.isEdited();
		IFormattableTextComponent text = this.getFieldName().deepCopy();
		if (matchedText != null && !matchedText.isEmpty()) {
			final String title = getUnformattedString(getFieldName());
			final int i = title.indexOf(matchedText);
			if (i != -1) {
				text = new StringTextComponent(title.substring(0, i))
				  .append(new StringTextComponent(title.substring(i, i + matchedText.length()))
				            .mergeStyle(focusedMatch? TextFormatting.GOLD : TextFormatting.YELLOW)
				            // .mergeStyle(TextFormatting.BOLD)
				            .mergeStyle(TextFormatting.UNDERLINE))
				  .appendString(title.substring(i + matchedText.length()));
			}
		}
		if (hasError)
			text = text.mergeStyle(TextFormatting.RED);
		if (isEdited)
			text = text.mergeStyle(TextFormatting.ITALIC);
		if (!hasError && !isEdited)
			text = text.mergeStyle(TextFormatting.GRAY);
		return text;
	}
	
	public abstract T getValue();
	public abstract void setValue(T value);
	
	public void resetValue() {
		resetValue(true);
	}
	public void resetValue(boolean commit) {
		final EditHistory history = getConfigScreen().getHistory();
		if (commit)
			commit = preserveState();
		setValue(getDefaultValue());
		if (commit) {
			history.saveState(getConfigScreen());
			history.preserveState(this);
		}
	}
	
	public void restoreValue() {
		restoreValue(true);
	}
	public void restoreValue(boolean commit) {
		final EditHistory history = getConfigScreen().getHistory();
		if (commit) commit = preserveState();
		setValue(original);
		if (commit) {
			history.saveState(getConfigScreen());
			history.preserveState(this);
		}
	}
	
	public void restoreValue(Object storedValue) {
		try {
			//noinspection unchecked
			setValue((T) storedValue);
			lastHistoryError = false;
		} catch (RuntimeException e) {
			lastHistoryError = true;
			LOGGER.warn("Could not revert value of config entry with path " + getPath() + ": " + e.getMessage());
		}
		lastHistoryTime = System.currentTimeMillis();
	}
	
	/**
	 * Subclasses should instead override {@link AbstractConfigEntry#getError()}
	 */
	@Override public Optional<ITextComponent> getConfigError() {
		if (this.errorSupplier != null) {
			final Optional<ITextComponent> opt = this.errorSupplier.get();
			if (opt.isPresent())
				return opt;
		}
		return this.getError();
	}
	
	public void setErrorSupplier(@Nullable Supplier<Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
	}
	
	@Internal public Optional<ITextComponent> getError() {
		return Optional.empty();
	}
	
	public @Nullable T getDefaultValue() {
		return defaultSupplier.get();
	}
	
	public void setDefaultValue(Supplier<T> defaultSupplier) {
		this.defaultSupplier = defaultSupplier;
	}
	
	public @Nullable T getOriginal() {
		return original;
	}
	
	public void setOriginal(@Nullable T original) {
		this.original = original;
	}
	
	public void setSaveConsumer(@Nullable Consumer<T> saveConsumer) {
		this.saveConsumer = saveConsumer;
	}
	
	public boolean isIgnoreEdits() {
		return ignoreEdits;
	}
	
	public void setIgnoreEdits(boolean ignoreEdits) {
		this.ignoreEdits = ignoreEdits;
	}
	
	@NotNull public final AbstractConfigScreen getConfigScreen() {
		final AbstractConfigScreen screen = this.screen.get();
		if (screen == null)
			throw new IllegalStateException("Cannot get config screen so early!");
		return screen;
	}
	
	@Internal public @Nullable final AbstractConfigScreen getConfigScreenOrNull() {
		return screen.get();
	}
	
	protected final void addTooltip(@NotNull Tooltip tooltip) {
		getConfigScreen().addTooltip(tooltip);
	}
	
	@Internal public boolean preserveState() {
		if (!(this instanceof IEntryHolder)) {
			if (!(this instanceof IChildListEntry && ((IChildListEntry) this).isChild())) {
				getConfigScreen().getHistory().preserveState(this);
			} else if (listParent != null) {
				listParent.setListener(this);
				listParent.preserveState();
			}
		}
		return true;
	}
	
	public void updateSelected(boolean isSelected) {
		if (isSelected && !this.isSelected) preserveState();
		this.isSelected = isSelected;
		if (!isSelected) {
			final IGuiEventListener listener = getListener();
			if (listener instanceof Widget && ((Widget) listener).isFocused())
				forceUnFocus(listener);
			setListener(null);
		}
	}
	
	@Internal public void setScreen(AbstractConfigScreen screen) {
		this.screen = new WeakReference<>(screen);
	}
	
	public abstract boolean isEditable();
	
	public abstract void setEditable(boolean editable);
	
	public void setEditableSupplier(Supplier<Boolean> editableSupplier) {
		this.editableSupplier = editableSupplier;
	}
	
	public void save() {
		if (!ignoreEdits && saveConsumer != null)
			saveConsumer.accept(getValue());
	}
	
	public boolean isEdited() {
		return !ignoreEdits && (this.getConfigError().isPresent()
		                        || !Objects.equals(getValue(), getOriginal()));
	}
	
	public boolean isResettable() {
		if (!isEditable()) return false;
		return !Objects.equals(getValue(), getDefaultValue());
	}
	
	public boolean isRestorable() {
		if (!isEditable()) return false;
		return !Objects.equals(getValue(), getOriginal());
	}
	
	@Override public int getItemHeight() {
		return 24;
	}
	
	public int getInitialReferenceOffset() {
		return 0;
	}
	
	@Override public void render(
	  MatrixStack mStack, int index, int y, int x, int w, int h, int mouseX, int mouseY,
	  boolean isHovered, float delta
	) {
		renderBg(mStack, index, y, x, w, h, mouseX, mouseY, isHovered, delta);
		renderEntry(mStack, index, y, x, w, h, mouseX, mouseY, isHovered, delta);
		renderOverlay(mStack, index, y, x, w, h, mouseX, mouseY, isHovered, delta);
	}
	
	public void renderBg(
	  MatrixStack mStack, int index, int y, int x, int w, int h, int mouseX, int mouseY,
	  boolean isHovered, float delta
	) {
		if (shouldHighlight())
			fill(mStack, 16, y, x + w, y + getCaptionHeight(), focusedMatch? focusedMatchColor : matchColor);
		final long t = System.currentTimeMillis() - lastHistoryTime;
		if (t < 1000) {
			int color = lastHistoryError? historyErrorColor : historyApplyColor;
			fill(mStack, 16, y, x + w, y + getCaptionHeight(),
			     color & 0xFFFFFF | (int) ((color >> 24 & 0xFF) * ((1000 - t) / 1000D)) << 24);
		}
	}
	
	@Internal public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int w, int h, int mouseX, int mouseY,
	  boolean isHovered, float delta
	) {
		if (editableSupplier != null) {
			final Boolean editable = editableSupplier.get();
			if (editable != null) setEditable(editable);
		}
	}
	
	public void renderOverlay(
	  MatrixStack mStack, int index, int y, int x, int w, int h, int mouseX, int mouseY,
	  boolean isHovered, float delta
	) {}
	
	@Override public void claimFocus() {
		getConfigScreen().setSelectedCategory(getCategory());
		IExpandable parent = expandableParent.get();
		LinkedList<AbstractConfigEntry<?>> parents = Lists.newLinkedList();
		while (parent instanceof AbstractConfigEntry) {
			final AbstractConfigEntry<?> p = (AbstractConfigEntry<?>) parent;
			parents.add(0, p);
			parent = p.expandableParent.get();
		}
		parents.add(this);
		AbstractConfigEntry<?> p = parents.get(0);
		getParent().setListener(p);
		for (int i = 1; i < parents.size(); i++) {
			AbstractConfigEntry<?> n = parents.get(i);
			p.setListener(n);
			p = n;
		}
		getParent().scrollTo(this);
		acquireFocus();
		getParent().setSelectedTarget(this);
	}
	
	// Search
	
	protected boolean shouldHighlight() {
		return matchedText != null && !matchedText.isEmpty();
	}
	
	@Override public List<ISeekableComponent> search(Pattern query) {
		final boolean matchesSelf = searchSelf(query);
		final List<Pair<ISeekableComponent, List<ISeekableComponent>>> children =
		  seekableChildren().stream().map(c -> Pair.of(c, c.search(query)))
			 .filter(p -> !p.second.isEmpty()).collect(Collectors.toList());
		final List<ISeekableComponent> result =
		  children.stream().flatMap(p -> p.second.stream()).collect(Collectors.toList());
		if (matchesSelf)
			result.add(0, this);
		return result;
	}
	
	protected boolean searchSelf(Pattern query) {
		boolean matches = false;
		final String text = seekableText();
		matchedText = matchedValueText = null;
		if (!text.isEmpty()) {
			final Matcher m = query.matcher(text);
			while (m.find()) {
				if (!m.group().isEmpty()) {
					matches = true;
					matchedText = m.group();
					break;
				}
			}
		}
		final String valueText = seekableValueText();
		if (!valueText.isEmpty()) {
			final Matcher m = query.matcher(valueText);
			while (m.find()) {
				if (!m.group().isEmpty()) {
					matches = true;
					matchedValueText = m.group();
					break;
				}
			}
		}
		return matches;
	}
	
	@Internal public String seekableText() {
		if (this instanceof IChildListEntry && ((IChildListEntry) this).isChild())
			return "";
		return getUnformattedString(getDisplayedFieldName());
	}
	
	@Internal public String seekableValueText() {
		final T value = getValue();
		if (value != null)
			return value.toString();
		return "";
	}
	
	protected List<ISeekableComponent> seekableChildren() {
		return Collections.emptyList();
	}
	
	@Override public boolean isFocusedMatch() {
		return focusedMatch;
	}
	
	@Override public void setFocusedMatch(boolean isFocusedMatch) {
		focusedMatch = isFocusedMatch;
		expandParents();
		claimFocus();
	}
	
	@Override public @Nullable INavigableTarget getNavigableParent() {
		if (navigableParent != null)
			return navigableParent;
		final IExpandable parent = getExpandableParent();
		if (parent instanceof INavigableTarget)
			return ((INavigableTarget) parent);
		return null;
	}
	
	public void setNavigableParent(@Nullable INavigableTarget parent) {
		this.navigableParent = parent;
	}
	
	@Override public void onNavigate() {
		expandParents();
		claimFocus();
	}
	
	protected void acquireFocus() {
		final List<? extends IGuiEventListener> listeners = getEventListeners();
		if (!listeners.isEmpty()) {
			final IGuiEventListener listener = listeners.get(0);
			setListener(listener);
			forceFocus(listener);
		}
	}
	
	private static final Pattern STYLE_ESCAPE = Pattern.compile("§[0-9a-f]");
	protected static String getUnformattedString(ITextComponent component) {
		return STYLE_ESCAPE.matcher(component.getString()).replaceAll("");
	}
	
	protected static void drawBorder(
	  MatrixStack mStack, int x, int y, int w, int h, int borderWidth, int color
	) {
		int maxX = x + w;
		int maxY = y + h;
		int bw = min(min(borderWidth, w), h);
		fill(mStack, x, y, maxX, y + bw, color);
		fill(mStack, x, y + bw, x + bw, maxY - bw, color);
		fill(mStack, maxX - bw, y + bw, maxX, maxY - bw, color);
		fill(mStack, x, maxY - bw, maxX, maxY, color);
	}
	
	protected static void forceUnFocus(IGuiEventListener... listeners) {
		for (IGuiEventListener listener : listeners)
			forceUnFocus(listener);
	}
	
	protected static void forceUnFocus(IGuiEventListener listener) {
		if (listener instanceof Widget && !((Widget) listener).isFocused())
			return;
		// FIXME when IGuiEventListener gets a `setFocused` method or equivalent
		for (int i = 0; i < 1000; i++) // Hanging here would be awkward
			if (!listener.changeFocus(true)) break;
	}
	
	protected static void forceFocus(IGuiEventListener listener) {
		if (listener instanceof Widget && ((Widget) listener).isFocused())
			return;
		forceUnFocus(listener);
		listener.changeFocus(true);
	}
	
	protected static void forceSetFocus(IGuiEventListener listener, boolean focus) {
		if (listener instanceof Widget && ((Widget) listener).isFocused() == focus)
			return;
		if (focus)
			forceFocus(listener);
		else forceUnFocus(listener);
	}
}
