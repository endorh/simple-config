package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.widget.ScrollingContainerWidget;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static java.lang.Math.*;

public abstract class AbstractButtonDialog extends AbstractDialog {
	
	protected List<Widget> buttons = Lists.newArrayList();
	protected ScrollingContainerWidget scroller;
	protected List<IGuiEventListener> bodyListeners = Lists.newArrayList();
	
	public AbstractButtonDialog(IOverlayCapableScreen screen, ITextComponent title) {
		super(screen, title);
		scroller = new DialogScrollingContainerWidget(x, y, (int) (w * 0.9), (int) (h * 0.5), this);
		listeners.add(scroller);
	}
	
	public void addButton(Widget button) {
		buttons.add(button);
		listeners.add(button);
	}
	
	public void addButton(int i, Widget button) {
		if (!buttons.isEmpty()) {
			listeners.add(i > 0 ? listeners.indexOf(buttons.get(i - 1)) + 1
			                    : listeners.indexOf(buttons.get(i + 1)), button);
		} else listeners.add(button);
		buttons.add(i, button);
	}
	
	@Override protected void position() {
		super.position();
		scroller.area.width = w - 8;
		scroller.area.height = h - 60;
		scroller.area.x = x + 4;
		scroller.area.y = y + 28;
		final int count = buttons.size();
		final int bw = min(150, (w - 4 - count * 4) / count);
		final int by = y + h - 24;
		int bx = max(x + 4, x + w / 2 - ((bw + 4) * count - 4) / 2);
		for (Widget button : buttons) {
			button.x = bx;
			button.y = by;
			button.setWidth(bw);
			bx += bw + 4;
		}
	}
	
	@Override public void renderBody(
	  MatrixStack mStack, int mouseX, int mouseY, float delta
	) {
		fill(mStack, x + 1, y + h - 27, x + w - 1, y + h - 1, backgroundOverlayColor);
		fill(mStack, x + 1, y + h - 28, x + w - 1, y + h - 27, subBorderColor);
		scroller.render(mStack, mouseX, mouseY, delta);
		for (Widget button : buttons) button.render(mStack, mouseX, mouseY, delta);
	}
	
	public abstract void renderInner(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta);
	
	public @Nullable Style getInnerTextAt(int x, int y, int w, int h, double mX, double mY) {
		return null;
	}
	
	public abstract int getInnerHeight();
	
	public static class DialogScrollingContainerWidget extends ScrollingContainerWidget {
		private static final Method Screen$renderComponentHoverEffect =
		  ObfuscationReflectionHelper.findMethod(
			 Screen.class, "func_238653_a_",
			 MatrixStack.class, Style.class, int.class, int.class);
		private static final Logger LOGGER = LogManager.getLogger();
		private static boolean loggedReflectionError = false;
		
		protected final AbstractButtonDialog dialog;
		
		public DialogScrollingContainerWidget(
		  int x, int y, int w, int h, AbstractButtonDialog dialog
		) {
			super(x, y, w, h);
			this.dialog = dialog;
		}
		
		@Override public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
			super.render(mStack, mouseX, mouseY, delta);
			final Style style = dialog.getInnerTextAt(
			  area.x, (int) round(area.y - scrollAmount),
			  area.width - 8, area.height, mouseX, mouseY);
			if (style != null) {
				try {
					Screen$renderComponentHoverEffect.invoke(
					  dialog.screen, mStack, style, mouseX, mouseY);
				} catch (IllegalAccessException | InvocationTargetException e) {
					if (!loggedReflectionError) {
						loggedReflectionError = true;
						LOGGER.error("Reflective invocation error: ", e);
					}
				}
			}
		}
		
		@Override public void renderInner(
		  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
		) {
			dialog.renderInner(mStack, x, y, w, h, mouseX, mouseY, delta);
		}
		
		@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
			final Style style = dialog.getInnerTextAt(
			  area.x, (int) round(area.y - scrollAmount),
			  area.width - 8, area.height, mouseX, mouseY);
			if (style != null && dialog.screen.handleComponentClicked(style)) return true;
			return super.mouseClicked(mouseX, mouseY, button);
		}
		
		@Override public int getInnerHeight() {
			return dialog.getInnerHeight();
		}
		
		@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
			return dialog.bodyListeners;
		}
	}
}
