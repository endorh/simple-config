package endorh.simpleconfig.ui.gui.widget.treeview;

import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable.IDelegatedPositionableRenderable;
import endorh.simpleconfig.ui.gui.widget.treeview.DragBroadcastableControl.DragBroadcastableAction.WidgetDragBroadcastableAction;
import net.minecraft.client.gui.widget.Widget;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class DragBroadcastableControl<W extends IPositionableRenderable> implements IDelegatedPositionableRenderable {
	private final Supplier<IDragBroadcastableControlContainer> treeSupplier;
	private final DragBroadcastableAction<W> action;
	private W control;
	
	public DragBroadcastableControl(
	  Supplier<IDragBroadcastableControlContainer> treeSupplier, DragBroadcastableAction<W> action, W control
	) {
		this.treeSupplier = treeSupplier;
		this.action = action;
		this.control = control;
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean clicked = IDelegatedPositionableRenderable.super.mouseClicked(mouseX, mouseY, button);
		if (clicked) getTree().startDragBroadcastableAction(getAction(), this);
		return clicked;
	}
	
	@Override public @NotNull IPositionableRenderable getDelegate() {
		return control;
	}
	
	public IDragBroadcastableControlContainer getTree() {
		return treeSupplier.get();
	}
	
	public DragBroadcastableAction<W> getAction() {
		return action;
	}
	
	public W getControl() {
		return control;
	}
	
	public static class DragBroadcastableWidget<W extends Widget> extends DragBroadcastableControl<WidgetPositionableWrapper<W>> {
		public DragBroadcastableWidget(
		  Supplier<IDragBroadcastableControlContainer> tree, WidgetDragBroadcastableAction<W> action, W widget
		) {
			super(tree, action, new WidgetPositionableWrapper<>(widget));
		}
		
		public W getWidget() {
			return getControl().getWidget();
		}
	}
	
	public interface DragBroadcastableAction<W extends IPositionableRenderable> {
		void apply(W widget, W source);
		default void start(W source) {}
		default void end(W source) {}
		
		interface WidgetDragBroadcastableAction<W extends Widget>
		  extends DragBroadcastableAction<WidgetPositionableWrapper<W>> {
			@Override default void apply(
			  WidgetPositionableWrapper<W> widget, WidgetPositionableWrapper<W> source
			) {
				apply(widget.getWidget(), source.getWidget());
			}
			@Override default void start(WidgetPositionableWrapper<W> source) {
				start(source.getWidget());
			}
			@Override default void end(WidgetPositionableWrapper<W> source) {
				end(source.getWidget());
			}
			
			void apply(W widget, W source);
			default void start(W source) {}
			default void end(W source) {}
		}
	}
}
