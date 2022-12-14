package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.gui.widget.ScrollingContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Math.*;

public abstract class AbstractButtonDialog extends AbstractDialog {
	protected List<AbstractWidget> buttons = Lists.newArrayList();
	protected ScrollingContainerWidget scroller;
	protected Consumer<String> linkActionHandler = s -> {};
	protected List<GuiEventListener> bodyListeners = Lists.newArrayList();
	
	public AbstractButtonDialog(Component title) {
		super(title);
		scroller = new DialogScrollingContainerWidget(getX(), getY(), (int) (getWidth() * 0.9), (int) (
		  getHeight() * 0.5), this);
		listeners.add(scroller);
	}
	
	public void addButton(AbstractWidget button) {
		buttons.add(button);
		listeners.add(button);
	}
	
	public void addButton(int i, AbstractWidget button) {
		if (i == buttons.size()) {
			addButton(button);
		} else {
			if (!buttons.isEmpty()) {
				listeners.add(listeners.indexOf(buttons.get(i)) + 1, button);
			} else listeners.add(button);
			buttons.add(i, button);
		}
	}
	
	public void removeButton(AbstractWidget button) {
		buttons.remove(button);
		listeners.remove(button);
	}
	
	public void removeButton(int i) {
		listeners.remove(buttons.remove(i));
	}
	
	@Override protected void layout() {
		super.layout();
		int x = getX(), y = getY(), w = getWidth(), h = getHeight();
		scroller.area.setBounds(x + 4, y + 28, w - 8, h - 60);
		scroller.setHideScrollBar(areaAnimator.isInProgress());
		final int count = buttons.size();
		if (count > 0) {
			final int bw = min(150, (w - 4 - count * 4) / count);
			final int by = y + h - 24;
			int bx = max(x + 4, x + w / 2 - ((bw + 4) * count - 4) / 2);
			for (AbstractWidget button: buttons) {
				button.x = bx;
				button.y = by;
				button.setWidth(bw);
				bx += bw + 4;
			}
		}
	}
	
	@Override public void renderBody(
	  PoseStack mStack, int mouseX, int mouseY, float delta
	) {
		int x = getX(), y = getY(), w = getWidth(), h = getHeight();
		fill(mStack, x + 1, y + h - 27, x + w - 1, y + h - 1, backgroundOverlayColor);
		fill(mStack, x + 1, y + h - 28, x + w - 1, y + h - 27, subBorderColor);
		scroller.render(mStack, mouseX, mouseY, delta);
		for (AbstractWidget button : buttons) button.render(mStack, mouseX, mouseY, delta);
	}
	
	public abstract void renderInner(
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta);
	
	public @Nullable Style getInnerTextAt(int x, int y, int w, int h, double mX, double mY) {
		return null;
	}
	
	protected boolean handleComponentClicked(
	  @NotNull Style style, double mouseX, double mouseY, int button
	) {
		ClickEvent event = style.getClickEvent();
		if (event != null && event.getAction() == ClickEvent.Action.OPEN_URL) {
			String value = event.getValue();
			if (value.startsWith("action:")) {
				handleTextAction(
				  value.substring("action:".length()),
				  mouseX, mouseY, button, style);
				return true;
			}
		}
		return getScreen().handleComponentClicked(style);
	}
	
	protected void handleTextAction(
	  String action, double mouseX, double mouseY, int button, @NotNull Style style
	) {
		if (linkActionHandler != null) linkActionHandler.accept(action);
	}
	
	public Consumer<String> getLinkActionHandler() {
		return linkActionHandler;
	}
	
	public void setLinkActionHandler(Consumer<String> linkActionHandler) {
		this.linkActionHandler = linkActionHandler;
	}
	
	public abstract int getInnerHeight();
	
	public static class DialogScrollingContainerWidget extends ScrollingContainerWidget {
		private static final Method Screen$renderComponentHoverEffect =
		  ObfuscationReflectionHelper.findMethod(
			 Screen.class, "m_96570_",
			 PoseStack.class, Style.class, int.class, int.class);
		private static final Logger LOGGER = LogManager.getLogger();
		private static boolean loggedReflectionError = false;
		
		protected final AbstractButtonDialog dialog;
		
		public DialogScrollingContainerWidget(
		  int x, int y, int w, int h, AbstractButtonDialog dialog
		) {
			super(x, y, w, h);
			this.dialog = dialog;
		}
		
		@Override public void render(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
			super.render(mStack, mouseX, mouseY, delta);
			Rectangle area = getArea();
			final Style style = dialog.getInnerTextAt(
			  area.x, (int) round(area.y - scrollAmount),
			  area.width - 8, area.height, mouseX, mouseY);
			if (style != null) {
				try {
					Screen$renderComponentHoverEffect.invoke(
					  dialog.getScreen(), mStack, style, mouseX, mouseY);
				} catch (IllegalAccessException | InvocationTargetException e) {
					if (!loggedReflectionError) {
						loggedReflectionError = true;
						LOGGER.error("Reflective invocation error: ", e);
					}
				}
			}
		}
		
		@Override public void renderInner(
		  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
		) {
			dialog.renderInner(mStack, x, y, w, h, mouseX, mouseY, delta);
		}
		
		@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
			Rectangle area = getArea();
			if (!area.contains(mouseX, mouseY)) return false;
			final Style style = dialog.getInnerTextAt(
			  area.x, (int) round(area.y - scrollAmount),
			  area.width - 8, area.height, mouseX, mouseY);
			if (style != null && dialog.handleComponentClicked(style, mouseX, mouseY, button)) return true;
			return super.mouseClicked(mouseX, mouseY, button);
		}
		
		@Override public int getInnerHeight() {
			return dialog.getInnerHeight();
		}
		
		@Override public @NotNull List<? extends GuiEventListener> children() {
			return dialog.bodyListeners;
		}
	}
}
