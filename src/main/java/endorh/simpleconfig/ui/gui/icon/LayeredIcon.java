package endorh.simpleconfig.ui.gui.icon;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.gui.icon.LayeredIcon.IIconLayer;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Icon composed by multiple layers, each of them an icon.<br>
 * All rendering logic is deferred to the layers.<br>
 * Layers can be turned invisible.<br>
 */
public abstract class LayeredIcon<L extends IIconLayer> extends Icon {
	public static SimpleLayeredIcon of(Icon... layers) {
		return of(Arrays.asList(layers));
	}
	public static SimpleLayeredIcon of(List<Icon> layers) {
		return new SimpleLayeredIcon(layers);
	}
	
	protected LayeredIcon(
	  int w, int h, int tint
	) {
		super(new ResourceLocation("dummy"), 0, 0, w, h, 0, 0, 0, 0, false, tint);
	}
	
	@Override public abstract Icon withTint(int tint);
	
	public abstract List<L> getRenderedLayers();
	
	@Override public int translateLevel(int level) {
		return level;
	}
	
	@Override public void renderCentered(MatrixStack mStack, int x, int y, int w, int h, int level) {
		beforeRender(level);
		for (L layer : getRenderedLayers()) if (layer.isVisible()) {
			beforeRenderLayer(layer);
			layer.getIcon().renderCentered(mStack, x, y, w, h, translateLevel(level));
			afterRenderLayer(layer);
		}
		afterRender(level);
	}
	
	@Override public void renderStretch(MatrixStack mStack, int x, int y, int w, int h, int level) {
		beforeRender(level);
		for (L layer : getRenderedLayers()) if (layer.isVisible()) {
			beforeRenderLayer(layer);
			layer.getIcon().renderStretch(mStack, x, y, w, h, translateLevel(level));
			afterRenderLayer(layer);
		}
		afterRender(level);
	}
	
	@Override public void renderFill(MatrixStack mStack, int x, int y, int w, int h, int level) {
		beforeRender(level);
		for (L layer : getRenderedLayers()) if (layer.isVisible()) {
			beforeRenderLayer(layer);
			layer.getIcon().renderFill(mStack, x, y, w, h, translateLevel(level));
			afterRenderLayer(layer);
		}
		afterRender(level);
	}
	
	@Override public void bindTexture() {}
	@Override protected void beforeRender(int level) {}
	@Override protected void afterRender(int level) {}
	
	public void beforeRenderLayer(L layer) {
		// If layers are tinted, their tints will have preference
		if (tint != 0) setShaderColorMask(tint);
	}
	
	public void afterRenderLayer(L layer) {
		removeShaderColorMask();
	}
	
	public interface IIconLayer {
		Icon getIcon();
		boolean isVisible();
	}
	
	public static class SimpleIconLayer implements IIconLayer {
		private Icon icon;
		private boolean visible = true;
		
		public SimpleIconLayer(Icon icon) {
			this.icon = icon;
		}
		
		@Override public Icon getIcon() {
			return icon;
		}
		
		public void setIcon(Icon icon) {
			this.icon = icon;
		}
		
		@Override public boolean isVisible() {
			return visible;
		}
		
		public void setVisible(boolean visible) {
			this.visible = visible;
		}
	}
	
	public static class SimpleLayeredIcon extends LayeredIcon<SimpleIconLayer> {
		private final List<SimpleIconLayer> layers;
		
		public SimpleLayeredIcon(List<Icon> layers) {
			this(layers, layers.stream().mapToInt(i -> i.w).max().orElse(0),
			     layers.stream().mapToInt(i -> i.h).max().orElse(0), 0);
		}
		
		public SimpleLayeredIcon(List<Icon> layers, int w, int h, int tint) {
			super(w, h, tint);
			this.layers = layers.stream().map(SimpleIconLayer::new).collect(Collectors.toList());
		}
		
		@Override public Icon withTint(int tint) {
			return new SimpleLayeredIcon(
			  layers.stream().map(SimpleIconLayer::getIcon).collect(Collectors.toList()),
			  w, h, tint);
		}
		
		@Override public List<SimpleIconLayer> getRenderedLayers() {
			return layers;
		}
		
		public int getLayerCount() {
			return layers.size();
		}
		
		public boolean isLayerVisible(int index) {
			return layers.get(index).isVisible();
		}
		
		public void setLayerVisible(int index) {
			layers.get(index).setVisible(true);
		}
		
		public void setLayerIcon(int index, Icon icon) {
			layers.get(index).setIcon(icon);
		}
		
		public void addLayer(int index, Icon icon) {
			layers.add(index, new SimpleIconLayer(icon));
		}
		
		public void removeLayer(int index) {
			layers.remove(index);
		}
	}
}
