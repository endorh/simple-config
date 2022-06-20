package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.clothconfig2.api.*;
import endorh.simpleconfig.clothconfig2.gui.AbstractConfigScreen;
import endorh.simpleconfig.clothconfig2.gui.INavigableTarget;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
import endorh.simpleconfig.clothconfig2.gui.widget.DynamicEntryListWidget;
import endorh.simpleconfig.clothconfig2.gui.widget.ResetButton;
import endorh.simpleconfig.clothconfig2.impl.EditHistory.EditRecord;
import endorh.simpleconfig.clothconfig2.impl.ISeekableComponent;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Math.*;

@OnlyIn(value = Dist.CLIENT)
public class SubCategoryListEntry
  extends TooltipListEntry<Void>
  implements IExpandable, IEntryHoldingListEntry, IEntryHolder {
	protected static final ResourceLocation CONFIG_TEX =
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png");
	protected ResetButton resetButton;
	protected @Nullable AbstractConfigListEntry<?> heldEntry;
	protected final CaptionWidget label;
	protected final List<AbstractConfigListEntry<?>> entries;
	protected final List<IGuiEventListener> children;
	protected final List<IGuiEventListener> expandedChildren;
	protected boolean expanded;
	protected ToggleAnimator expandAnimator = new ToggleAnimator();
	
	@Internal public SubCategoryListEntry(
	  ITextComponent categoryName, List<AbstractConfigListEntry<?>> entries
	) {
		super(categoryName);
		this.entries = entries;
		entries.forEach(e -> e.setExpandableParent(this));
		entries.forEach(e -> e.setParentEntry(this));
		label = new CaptionWidget(this);
		resetButton = new ResetButton(this);
		children = Lists.newArrayList(label, resetButton);
		expandedChildren = Lists.newArrayList(label, resetButton);
		expandedChildren.addAll(entries);
		//noinspection unchecked
		setReferenceProviderEntries((List<ReferenceProvider>) (List<?>) entries);
	}
	
	@Override public boolean isExpanded() {
		return expanded;
	}
	
	@Override public void setExpanded(boolean expanded, boolean recursive) {
		if (this.expanded != expanded) {
			expandAnimator.setLength(min(250L, entries.size() * 25L));
			expandAnimator.setEaseOutTarget(expanded);
		}
		this.expanded = expanded;
		if (recursive)
			entries.stream().filter(e -> e instanceof IExpandable)
			  .forEach(e -> ((IExpandable) e).setExpanded(expanded, true));
	}
	
	@Override public boolean isRequiresRestart() {
		return heldEntry != null && heldEntry.isEdited() && heldEntry.isRequiresRestart()
		       || entries.stream().anyMatch(e -> e.isEdited() && e.isRequiresRestart());
	}
	
	@Override public void setRequiresRestart(boolean requiresRestart) {
		entries.forEach(e -> e.setRequiresRestart(requiresRestart));
		if (heldEntry != null)
			heldEntry.setRequiresRestart(requiresRestart);
	}
	
	public ITextComponent getCategoryName() {
		return getFieldName();
	}
	
	public List<AbstractConfigEntry<?>> getEntries() {
		//noinspection unchecked
		return (List<AbstractConfigEntry<?>>) (List<?>) entries;
	}
	
	@Override public Void getValue() {
		return null;
	}
	@Override public void setValue(Void value) {}
	
	@Override public void resetValue(boolean commit) {
		EditRecord record = null;
		if (commit) {
			getConfigScreen().getHistory().saveState(getConfigScreen());
			//noinspection unchecked
			record = EditRecord.of(this, ((List<AbstractConfigEntry<?>>) (List<?>) this.entries));
		}
		entries.forEach(e -> e.resetValue(false));
		if (commit) {
			record.flatten(getConfigScreen());
			if (record.size() > 0)
				getConfigScreen().getHistory().addRecord(record);
		}
	}
	
	@Override public void restoreValue(boolean commit) {
		EditRecord record = null;
		if (commit) {
			getConfigScreen().getHistory().saveState(getConfigScreen());
			//noinspection unchecked
			record = EditRecord.of(this, ((List<AbstractConfigEntry<?>>) (List<?>) this.entries));
		}
		entries.forEach(e -> e.restoreValue(false));
		if (commit) {
			record.flatten(getConfigScreen());
			if (record.size() > 0)
				getConfigScreen().getHistory().addRecord(record);
		}
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (isHeldEntryHovered(mouseX, mouseY))
			return Optional.empty();
		if (resetButton.isMouseOver(mouseX, mouseY))
			return resetButton.getTooltip(mouseX, mouseY);
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public void setParent(DynamicEntryListWidget<?> parent) {
		super.setParent(parent);
		for (AbstractConfigListEntry<?> entry : entries) entry.setParent(parent);
		if (heldEntry != null)
			heldEntry.setParent(parent);
	}
	
	@Override public void setScreen(AbstractConfigScreen screen) {
		super.setScreen(screen);
		for (AbstractConfigListEntry<?> entry : entries) entry.setScreen(screen);
		if (heldEntry != null)
			heldEntry.setScreen(screen);
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		label.area.setBounds(x - 24, y, heldEntry != null? entryWidth - 132 : entryWidth + 17 - resetButton.getWidth(), 20);
		WidgetUtils.forceSetFocus(label, isSelected && getFocused() == label);
		super.renderEntry(
		  mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		Minecraft.getInstance().getTextureManager().bind(CONFIG_TEX);
		RenderHelper.turnOff();
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		blit(
		  mStack, x - 15, y + 5, 24,
		  (label.area.contains(mouseX, mouseY) ? 18 : 0) + (expanded ? 9 : 0), 9, 9);
		Minecraft.getInstance().font.drawShadow(
		  mStack, getDisplayedFieldName().getVisualOrderText(), (float) x, (float) (y + 6),
		  label.area.contains(mouseX, mouseY) ? 0xffe6fe16 : 0xffffffff);
		resetButton.x = x + entryWidth - resetButton.getWidth();
		resetButton.y = y;
		final boolean animating = expandAnimator.isInProgress();
		if (expanded || animating) {
			if (animating) ScissorsHandler.INSTANCE.scissor(
			  new Rectangle(entryArea.x, entryArea.y, entryArea.width, getItemHeight()));
			int yy = y + 24;
			for (AbstractConfigListEntry<?> entry : entries) {
				entry.render(
				  mStack, -1, yy, x + 14, entryWidth - 14, entry.getItemHeight(), mouseX, mouseY,
				  isHovered && getFocused() == entry, delta);
				yy += entry.getItemHeight();
			}
			if (animating) ScissorsHandler.INSTANCE.removeLastScissor();
		}
		resetButton.render(mStack, mouseX, mouseY, delta);
		label.render(mStack, mouseX, mouseY, delta);
		if (heldEntry != null) {
			((IChildListEntry) heldEntry).renderChild(
			  mStack, x + entryWidth - 148, y, 144 - resetButton.getWidth(), 20, mouseX, mouseY, delta);
		}
	}
	
	public boolean isHeldEntryHovered(int mouseX, int mouseY) {
		return heldEntry != null && mouseX >= entryArea.getMaxX() - 148
		       && mouseX < entryArea.getMaxX() - 4 - resetButton.getWidth()
		       && mouseY >= entryArea.y && mouseY < entryArea.getMaxY();
	}
	
	@Override public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		if (heldEntry != null) {
			final boolean heldEntrySelected = isSelected && getFocused() == heldEntry;
			final boolean prevSelected = heldEntry.isSelected();
			heldEntry.updateSelected(heldEntrySelected);
			if (!prevSelected && heldEntrySelected) getConfigScreen().getHistory().preserveState(heldEntry);
		}
		for (AbstractConfigListEntry<?> entry : entries)
			entry.updateSelected(expanded && isSelected && getFocused() == entry);
	}
	
	@Override public boolean isEdited() {
		if (ignoreEdits) return false;
		return heldEntry != null && heldEntry.isEdited()
		       || entries.stream().anyMatch(AbstractConfigEntry::isEdited);
	}
	
	@Override public void setEditable(boolean editable) {
		super.setEditable(editable);
		if (heldEntry != null) heldEntry.setEditable(editable);
		for (AbstractConfigListEntry<?> entry : entries) entry.setEditable(editable);
	}
	
	@Override public boolean isResettable() {
		return isEditable() && entries.stream().anyMatch(AbstractConfigEntry::isResettable);
	}
	
	@Override public boolean isRestorable() {
		return isEditable() && entries.stream().anyMatch(AbstractConfigEntry::isRestorable);
	}
	
	@Override public int getExtraScrollHeight() {
		ArrayList<Integer> list = new ArrayList<>();
		int i = 24;
		if (heldEntry != null)
			list.add(i + heldEntry.getExtraScrollHeight());
		if (expanded) {
			for (AbstractConfigListEntry<?> entry : entries) {
				i += entry.getItemHeight();
				if (entry.getExtraScrollHeight() < 0) continue;
				list.add(i + entry.getExtraScrollHeight());
			}
			list.add(i);
		}
		return list.stream().max(Integer::compare).orElse(0) - i;
	}
	
	@Override public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
		return new Rectangle(getParent().left, y, getParent().right - getParent().left, 20);
	}
	
	@Override public int getItemHeight() {
		if (expanded || expandAnimator.isInProgress()) {
			int i = 24;
			for (AbstractConfigListEntry<?> entry : entries)
				i += entry.getItemHeight();
			return round(expandAnimator.getEaseOut() * (i - 24)) + 24;
		}
		return 24;
	}
	
	@Override public int getInitialReferenceOffset() {
		return 24;
	}
	
	public @NotNull List<? extends IGuiEventListener> children() {
		return expanded ? expandedChildren : children;
	}
	
	@Override public void save() {
		if (heldEntry != null) heldEntry.save();
		entries.forEach(AbstractConfigEntry::save);
	}
	
	@Internal @Override public Optional<ITextComponent> getErrorMessage() {
		return Optional.empty();
	}
	
	@Override public int getFocusedScroll() {
		final IGuiEventListener listener = getFocused();
		//noinspection SuspiciousMethodCalls
		if (!entries.contains(listener))
			return 0;
		int y = 24;
		//noinspection SuspiciousMethodCalls
		final int index = entries.indexOf(listener);
		if (index >= 0) {
			for (AbstractConfigListEntry<?> entry : entries.subList(0, index))
				y += entry.getItemHeight();
		}
		if (listener instanceof IExpandable)
			y += ((IExpandable) listener).getFocusedScroll();
		return y;
	}
	
	@Override public int getFocusedHeight() {
		final IGuiEventListener listener = getFocused();
		if (listener instanceof IExpandable)
			return ((IExpandable) listener).getFocusedHeight();
		if (listener instanceof AbstractConfigListEntry<?>)
			return ((AbstractConfigListEntry<?>) listener).getItemHeight();
		return 20;
	}
	
	@Override protected List<ISeekableComponent> seekableChildren() {
		final List<ISeekableComponent> children =
		  entries.stream().map(e -> ((ISeekableComponent) e)).collect(Collectors.toList());
		if (heldEntry != null)
			children.add(0, heldEntry);
		return children;
	}
	
	@Override public String seekableValueText() {
		return "";
	}
	
	// Handles the caption clicks and key-presses, but not the rendering
	public static class CaptionWidget implements IGuiEventListener {
		protected boolean focused = false;
		protected WeakReference<IExpandable> expandable;
		protected final Rectangle area = new Rectangle();
		protected int focusedColor = 0x80E0E0E0;
		
		protected CaptionWidget(IExpandable expandable) {
			this.expandable = new WeakReference<>(expandable);
		}
		
		protected IExpandable getParent() {
			return expandable.get();
		}
		
		public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
			if (focused)
				drawBorder(mStack, area.x + 4, area.y, area.width, area.height, 1, focusedColor);
		}
		
		public boolean isMouseOver(double mouseX, double mouseY) {
			return area.contains(mouseX, mouseY);
		}
		
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (area.contains(mouseX, mouseY)) {
				final IExpandable parent = getParent();
				parent.setExpanded(!parent.isExpanded(), Screen.hasShiftDown());
				Minecraft.getInstance().getSoundManager().play(
				  SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				return true;
			}
			return false;
		}
		
		@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			final IExpandable parent = getParent();
			switch (keyCode) {
				case 262: // Right
					if (!parent.isExpanded()) {
						parent.setExpanded(true, Screen.hasShiftDown());
						Minecraft.getInstance().getSoundManager().play(
						  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
						return true;
					}
					break;
				case 263: // Left
					if (parent.isExpanded()) {
						parent.setExpanded(false, Screen.hasShiftDown());
						Minecraft.getInstance().getSoundManager().play(
						  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
						return true;
					}
					break;
			}
			return IGuiEventListener.super.keyPressed(keyCode, scanCode, modifiers);
		}
		
		@Override public boolean changeFocus(boolean focus) {
			return focused = !focused;
		}
	}
	
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		return super.handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	@Override public <E extends AbstractConfigListEntry<?> & IChildListEntry> @Nullable E getHeldEntry() {
		//noinspection unchecked
		return (E) heldEntry;
	}
	
	@Override public <E extends AbstractConfigListEntry<?> & IChildListEntry> void setHeldEntry(
	  E entry
	) {
		if (heldEntry != null) {
			children.remove(heldEntry);
			expandedChildren.remove(heldEntry);
		}
		heldEntry = entry;
		heldEntry.setParentEntry(this);
		entry.setChild(true);
		children.add(1, heldEntry);
		expandedChildren.add(1, heldEntry);
		heldEntry.setParent(getParentOrNull());
		heldEntry.setScreen(getConfigScreenOrNull());
	}
	
	@Override public List<INavigableTarget> getNavigableChildren() {
		if (expanded) {
			final List<INavigableTarget> targets =
			  entries.stream().flatMap(e -> e.getNavigableChildren().stream())
				 .collect(Collectors.toList());
			targets.add(0, this);
			return targets;
		}
		return super.getNavigableChildren();
	}
	
	@Internal public static class ToggleAnimator {
		protected float progress;
		protected float target;
		protected float lastProgress;
		protected long lastChange = 0L;
		protected long length;
		
		public ToggleAnimator() {this(250L);}
		
		public ToggleAnimator(long length) {this(0F, length);}
		
		public ToggleAnimator(float progress, long length) {
			this.target = this.lastProgress = this.progress = progress;
			this.length = length;
		}
		
		public void toggle() {
			setTarget(target <= 0.5);
		}
		public void setTarget(boolean onOff) {
			this.setTarget(onOff? 1F : 0F);
		}
		public void setTarget(float target) {
			this.lastProgress = getProgress();
			this.target = target;
			this.lastChange = System.currentTimeMillis();
		}
		
		public void setEaseOutTarget(boolean onOff) {
			this.setEaseOutTarget(onOff? 1F : 0F);
		}
		public void setEaseOutTarget(float target) {
			this.lastProgress = getEaseOut();
			this.target = target;
			this.lastChange = System.currentTimeMillis();
		}
		
		public void setLength(long length) {
			this.length = length;
		}
		
		public boolean isInProgress() {
			return System.currentTimeMillis() - lastChange < length * abs(target - lastProgress);
		}
		
		public float getProgress() {
			long time = System.currentTimeMillis();
			float len = length * abs(target - lastProgress);
			if (time - lastChange < len) {
				final float t = (time - lastChange) / len;
				return progress = lastProgress * (1 - t) + target * t;
			} else return progress = target;
		}
		
		public float getEaseOut() {
			final float t = getProgress();
			return target < t? t*t : -t*t + 2*t;
		}
	}
}
