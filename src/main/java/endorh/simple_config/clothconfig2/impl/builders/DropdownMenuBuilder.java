package endorh.simple_config.clothconfig2.impl.builders;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.entries.DropdownBoxEntry;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class DropdownMenuBuilder<T>
  extends FieldBuilder<T, DropdownBoxEntry<T>> {
	protected DropdownBoxEntry.SelectionTopCellElement<T> topCellElement;
	protected DropdownBoxEntry.SelectionCellCreator<T> cellCreator;
	protected Function<T, Optional<ITextComponent[]>> tooltipSupplier = str -> Optional.empty();
	protected Consumer<T> saveConsumer = null;
	protected Iterable<T> selections = Collections.emptyList();
	protected boolean suggestionMode = true;
	
	public DropdownMenuBuilder(
	  ITextComponent resetButtonKey, ITextComponent fieldNameKey,
	  DropdownBoxEntry.SelectionTopCellElement<T> topCellElement,
	  DropdownBoxEntry.SelectionCellCreator<T> cellCreator
	) {
		super(resetButtonKey, fieldNameKey);
		this.topCellElement = Objects.requireNonNull(topCellElement);
		this.cellCreator = Objects.requireNonNull(cellCreator);
	}
	
	public DropdownMenuBuilder<T> setSelections(Iterable<T> selections) {
		this.selections = selections;
		return this;
	}
	
	public DropdownMenuBuilder<T> setDefaultValue(Supplier<T> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public DropdownMenuBuilder<T> setDefaultValue(T defaultValue) {
		this.defaultValue = () -> Objects.requireNonNull(defaultValue);
		return this;
	}
	
	public DropdownMenuBuilder<T> setSaveConsumer(Consumer<T> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public DropdownMenuBuilder<T> setTooltipSupplier(
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = str -> tooltipSupplier.get();
		return this;
	}
	
	public DropdownMenuBuilder<T> setTooltipSupplier(
	  Function<T, Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public DropdownMenuBuilder<T> setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = str -> tooltip;
		return this;
	}
	
	public DropdownMenuBuilder<T> setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = str -> Optional.ofNullable(tooltip);
		return this;
	}
	
	public DropdownMenuBuilder<T> requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public DropdownMenuBuilder<T> setErrorSupplier(
	  Function<T, Optional<ITextComponent>> errorSupplier
	) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public DropdownMenuBuilder<T> setSuggestionMode(boolean suggestionMode) {
		this.suggestionMode = suggestionMode;
		return this;
	}
	
	public boolean isSuggestionMode() {
		return this.suggestionMode;
	}
	
	@Override
	@NotNull
	public DropdownBoxEntry<T> build() {
		DropdownBoxEntry<T> entry =
        new DropdownBoxEntry<>(this.getFieldNameKey(), this.getResetButtonKey(), null,
                               this.isRequireRestart(), this.defaultValue, this.saveConsumer,
                               this.selections, this.topCellElement, this.cellCreator);
		entry.setTooltipSupplier(() -> this.tooltipSupplier.apply(entry.getValue()));
		if (this.errorSupplier != null) {
			entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue()));
		}
		entry.setSuggestionMode(this.suggestionMode);
		return entry;
	}
	
	public static class CellCreatorBuilder {
		public static <T> DropdownBoxEntry.SelectionCellCreator<T> of() {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<>();
		}
		
		public static <T> DropdownBoxEntry.SelectionCellCreator<T> of(
		  Function<T, ITextComponent> toTextFunction
		) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<>(toTextFunction);
		}
		
		public static <T> DropdownBoxEntry.SelectionCellCreator<T> ofWidth(final int cellWidth) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<T>() {
				
				@Override
				public int getCellWidth() {
					return cellWidth;
				}
			};
		}
		
		public static <T> DropdownBoxEntry.SelectionCellCreator<T> ofWidth(
		  final int cellWidth, Function<T, ITextComponent> toTextFunction
		) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<T>(toTextFunction) {
				
				@Override
				public int getCellWidth() {
					return cellWidth;
				}
			};
		}
		
		public static <T> DropdownBoxEntry.SelectionCellCreator<T> ofCellCount(final int maxItems) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<T>() {
				
				@Override
				public int getDropBoxMaxHeight() {
					return this.getCellHeight() * maxItems;
				}
			};
		}
		
		public static <T> DropdownBoxEntry.SelectionCellCreator<T> ofCellCount(
		  final int maxItems, Function<T, ITextComponent> toTextFunction
		) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<T>(toTextFunction) {
				
				@Override
				public int getDropBoxMaxHeight() {
					return this.getCellHeight() * maxItems;
				}
			};
		}
		
		public static <T> DropdownBoxEntry.SelectionCellCreator<T> of(
		  final int cellWidth, final int maxItems
		) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<T>() {
				
				@Override
				public int getCellWidth() {
					return cellWidth;
				}
				
				@Override
				public int getDropBoxMaxHeight() {
					return this.getCellHeight() * maxItems;
				}
			};
		}
		
		public static <T> DropdownBoxEntry.SelectionCellCreator<T> of(
		  final int cellWidth, final int maxItems, Function<T, ITextComponent> toTextFunction
		) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<T>(toTextFunction) {
				
				@Override
				public int getCellWidth() {
					return cellWidth;
				}
				
				@Override
				public int getDropBoxMaxHeight() {
					return this.getCellHeight() * maxItems;
				}
			};
		}
		
		public static <T> DropdownBoxEntry.SelectionCellCreator<T> of(
		  final int cellHeight, final int cellWidth, final int maxItems
		) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<T>() {
				
				@Override
				public int getCellHeight() {
					return cellHeight;
				}
				
				@Override
				public int getCellWidth() {
					return cellWidth;
				}
				
				@Override
				public int getDropBoxMaxHeight() {
					return this.getCellHeight() * maxItems;
				}
			};
		}
		
		public static <T> DropdownBoxEntry.SelectionCellCreator<T> of(
		  final int cellHeight, final int cellWidth, final int maxItems,
		  Function<T, ITextComponent> toTextFunction
		) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<T>(toTextFunction) {
				
				@Override
				public int getCellHeight() {
					return cellHeight;
				}
				
				@Override
				public int getCellWidth() {
					return cellWidth;
				}
				
				@Override
				public int getDropBoxMaxHeight() {
					return this.getCellHeight() * maxItems;
				}
			};
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<ResourceLocation> ofItemIdentifier() {
			return CellCreatorBuilder.ofItemIdentifier(20, 146, 7);
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<ResourceLocation> ofItemIdentifier(
		  int maxItems
		) {
			return CellCreatorBuilder.ofItemIdentifier(20, 146, maxItems);
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<ResourceLocation> ofItemIdentifier(
		  final int cellHeight, final int cellWidth, final int maxItems
		) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<ResourceLocation>() {
				
				@Override
				public DropdownBoxEntry.SelectionCellElement<ResourceLocation> create(
				  ResourceLocation selection
				) {
					final ItemStack s = new ItemStack(Registry.ITEM.getOrDefault(selection));
					return new DropdownBoxEntry.DefaultSelectionCellElement<ResourceLocation>(
					  selection, this.toTextFunction) {
						
						@Override
						public void render(
						  MatrixStack matrices, int mouseX, int mouseY, int x, int y, int width,
						  int height, float delta
						) {
							boolean b;
							this.rendering = true;
							this.x = x;
							this.y = y;
							this.width = width;
							this.height = height;
							boolean bl = b =
							  mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
							if (b) fill(matrices, x + 1, y + 1, x + width - 1, y + height - 1, -15132391);
							Minecraft.getInstance().fontRenderer.func_238407_a_(
							  matrices, this.toTextFunction.apply(this.r)
								 .func_241878_f(), (float) (x + 6 + 18), (float) (y + 6),
							  b ? 0xFFFFFF : 0x888888);
							ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
							itemRenderer.renderItemIntoGUI(s, x + 4, y + 2);
						}
					};
				}
				
				@Override
				public int getCellHeight() {
					return cellHeight;
				}
				
				@Override
				public int getCellWidth() {
					return cellWidth;
				}
				
				@Override
				public int getDropBoxMaxHeight() {
					return this.getCellHeight() * maxItems;
				}
			};
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<ResourceLocation> ofBlockIdentifier() {
			return CellCreatorBuilder.ofBlockIdentifier(20, 146, 7);
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<ResourceLocation> ofBlockIdentifier(
		  int maxItems
		) {
			return CellCreatorBuilder.ofBlockIdentifier(20, 146, maxItems);
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<ResourceLocation> ofBlockIdentifier(
		  final int cellHeight, final int cellWidth, final int maxItems
		) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<ResourceLocation>() {
				
				@Override
				public DropdownBoxEntry.SelectionCellElement<ResourceLocation> create(
				  ResourceLocation selection
				) {
					final ItemStack s = new ItemStack(Registry.BLOCK.getOrDefault(selection));
					return new DropdownBoxEntry.DefaultSelectionCellElement<ResourceLocation>(
					  selection, this.toTextFunction) {
						
						@Override
						public void render(
						  MatrixStack matrices, int mouseX, int mouseY, int x, int y, int width,
						  int height, float delta
						) {
							boolean b;
							this.rendering = true;
							this.x = x;
							this.y = y;
							this.width = width;
							this.height = height;
							boolean bl = b =
							  mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
							if (b) fill(matrices, x + 1, y + 1, x + width - 1, y + height - 1, -15132391);
							Minecraft.getInstance().fontRenderer.func_238407_a_(
							  matrices, this.toTextFunction.apply(this.r)
								 .func_241878_f(), (float) (x + 6 + 18), (float) (y + 6),
							  b ? 0xFFFFFF : 0x888888);
							ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
							itemRenderer.renderItemIntoGUI(s, x + 4, y + 2);
						}
					};
				}
				
				@Override
				public int getCellHeight() {
					return cellHeight;
				}
				
				@Override
				public int getCellWidth() {
					return cellWidth;
				}
				
				@Override
				public int getDropBoxMaxHeight() {
					return this.getCellHeight() * maxItems;
				}
			};
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<Item> ofItemObject() {
			return CellCreatorBuilder.ofItemObject(20, 146, 7);
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<Item> ofItemObject(int maxItems) {
			return CellCreatorBuilder.ofItemObject(20, 146, maxItems);
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<Item> ofItemObject(
		  final int cellHeight, final int cellWidth, final int maxItems
		) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<Item>(
			  i -> new StringTextComponent(Registry.ITEM.getKey(i).toString())) {
				
				@Override
				public DropdownBoxEntry.SelectionCellElement<Item> create(Item selection) {
					final ItemStack s = new ItemStack(selection);
					return new DropdownBoxEntry.DefaultSelectionCellElement<Item>(
					  selection, this.toTextFunction) {
						
						@Override
						public void render(
						  MatrixStack matrices, int mouseX, int mouseY, int x, int y, int width,
						  int height, float delta
						) {
							boolean b;
							this.rendering = true;
							this.x = x;
							this.y = y;
							this.width = width;
							this.height = height;
							boolean bl = b =
							  mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
							if (b) fill(matrices, x + 1, y + 1, x + width - 1, y + height - 1, -15132391);
							Minecraft.getInstance().fontRenderer.func_238407_a_(
							  matrices, this.toTextFunction.apply(this.r)
								 .func_241878_f(), (float) (x + 6 + 18), (float) (y + 6),
							  b ? 0xFFFFFF : 0x888888);
							ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
							itemRenderer.renderItemIntoGUI(s, x + 4, y + 2);
						}
					};
				}
				
				@Override
				public int getCellHeight() {
					return cellHeight;
				}
				
				@Override
				public int getCellWidth() {
					return cellWidth;
				}
				
				@Override
				public int getDropBoxMaxHeight() {
					return this.getCellHeight() * maxItems;
				}
			};
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<Block> ofBlockObject() {
			return CellCreatorBuilder.ofBlockObject(20, 146, 7);
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<Block> ofBlockObject(int maxItems) {
			return CellCreatorBuilder.ofBlockObject(20, 146, maxItems);
		}
		
		public static DropdownBoxEntry.SelectionCellCreator<Block> ofBlockObject(
		  final int cellHeight, final int cellWidth, final int maxItems
		) {
			return new DropdownBoxEntry.DefaultSelectionCellCreator<Block>(
			  i -> new StringTextComponent(Registry.BLOCK.getKey(i).toString())) {
				
				@Override
				public DropdownBoxEntry.SelectionCellElement<Block> create(Block selection) {
					final ItemStack s = new ItemStack(selection);
					return new DropdownBoxEntry.DefaultSelectionCellElement<Block>(
					  selection, this.toTextFunction) {
						
						@Override
						public void render(
						  MatrixStack matrices, int mouseX, int mouseY, int x, int y, int width,
						  int height, float delta
						) {
							boolean b;
							this.rendering = true;
							this.x = x;
							this.y = y;
							this.width = width;
							this.height = height;
							boolean bl = b =
							  mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
							if (b) fill(matrices, x + 1, y + 1, x + width - 1, y + height - 1, -15132391);
							Minecraft.getInstance().fontRenderer.func_238407_a_(
							  matrices, this.toTextFunction.apply(this.r)
								 .func_241878_f(), (float) (x + 6 + 18), (float) (y + 6),
							  b ? 0xFFFFFF : 0x888888);
							ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
							itemRenderer.renderItemIntoGUI(s, x + 4, y + 2);
						}
					};
				}
				
				@Override
				public int getCellHeight() {
					return cellHeight;
				}
				
				@Override
				public int getCellWidth() {
					return cellWidth;
				}
				
				@Override
				public int getDropBoxMaxHeight() {
					return this.getCellHeight() * maxItems;
				}
			};
		}
	}
	
	public static class TopCellElementBuilder {
		public static final Function<String, ResourceLocation> IDENTIFIER_FUNCTION = str -> {
			try {
				return new ResourceLocation(str);
			} catch (NumberFormatException e) {
				return null;
			}
		};
		public static final Function<String, ResourceLocation> ITEM_IDENTIFIER_FUNCTION = str -> {
			try {
				ResourceLocation identifier = new ResourceLocation(str);
				if (Registry.ITEM.getOptional(identifier).isPresent()) {
					return identifier;
				}
			} catch (Exception exception) {
				// empty catch block
			}
			return null;
		};
		public static final Function<String, ResourceLocation> BLOCK_IDENTIFIER_FUNCTION = str -> {
			try {
				ResourceLocation identifier = new ResourceLocation(str);
				if (Registry.BLOCK.getOptional(identifier).isPresent()) {
					return identifier;
				}
			} catch (Exception exception) {
				// empty catch block
			}
			return null;
		};
		public static final Function<String, Item> ITEM_FUNCTION = str -> {
			try {
				return Registry.ITEM.getOptional(new ResourceLocation(str)).orElse(null);
			} catch (Exception exception) {
				return null;
			}
		};
		public static final Function<String, Block> BLOCK_FUNCTION = str -> {
			try {
				return Registry.BLOCK.getOptional(new ResourceLocation(str)).orElse(null);
			} catch (Exception exception) {
				return null;
			}
		};
		private static final ItemStack BARRIER = new ItemStack(Items.BARRIER);
		
		public static <T> DropdownBoxEntry.SelectionTopCellElement<T> of(
		  T value, Function<String, T> toObjectFunction
		) {
			return TopCellElementBuilder.of(
			  value, toObjectFunction, t -> new StringTextComponent(t.toString()));
		}
		
		public static <T> DropdownBoxEntry.SelectionTopCellElement<T> of(
		  T value, Function<String, T> toObjectFunction, Function<T, ITextComponent> toTextFunction
		) {
			return new DropdownBoxEntry.DefaultSelectionTopCellElement<>(
           value, toObjectFunction, toTextFunction);
		}
		
		public static DropdownBoxEntry.SelectionTopCellElement<ResourceLocation> ofItemIdentifier(
		  Item item
		) {
			return new DropdownBoxEntry.DefaultSelectionTopCellElement<ResourceLocation>(
			  Registry.ITEM.getKey(
				 item), ITEM_IDENTIFIER_FUNCTION,
			  identifier -> new StringTextComponent(identifier.toString())) {
				
				@Override
				public void render(
				  MatrixStack matrices, int mouseX, int mouseY, int x, int y, int width, int height,
				  float delta
				) {
					this.textFieldWidget.x = x + 4;
					this.textFieldWidget.y = y + 6;
					this.textFieldWidget.setWidth(width - 4 - 20);
					this.textFieldWidget.setEnabled(this.getParent().isEditable());
					this.textFieldWidget.setTextColor(this.getPreferredTextColor());
					this.textFieldWidget.render(matrices, mouseX, mouseY, delta);
					ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
					ItemStack stack = this.hasConfigError() ? BARRIER : new ItemStack(
					  Registry.ITEM.getOrDefault(this.getValue()));
					itemRenderer.renderItemIntoGUI(stack, x + width - 18, y + 2);
				}
			};
		}
		
		public static DropdownBoxEntry.SelectionTopCellElement<ResourceLocation> ofBlockIdentifier(
		  Block block
		) {
			return new DropdownBoxEntry.DefaultSelectionTopCellElement<ResourceLocation>(
			  Registry.BLOCK.getKey(
				 block), BLOCK_IDENTIFIER_FUNCTION,
			  identifier -> new StringTextComponent(identifier.toString())) {
				
				@Override
				public void render(
				  MatrixStack matrices, int mouseX, int mouseY, int x, int y, int width, int height,
				  float delta
				) {
					this.textFieldWidget.x = x + 4;
					this.textFieldWidget.y = y + 6;
					this.textFieldWidget.setWidth(width - 4 - 20);
					this.textFieldWidget.setEnabled(this.getParent().isEditable());
					this.textFieldWidget.setTextColor(this.getPreferredTextColor());
					this.textFieldWidget.render(matrices, mouseX, mouseY, delta);
					ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
					ItemStack stack = this.hasConfigError() ? BARRIER : new ItemStack(
					  Registry.BLOCK.getOrDefault(this.getValue()));
					itemRenderer.renderItemIntoGUI(stack, x + width - 18, y + 2);
				}
			};
		}
		
		public static DropdownBoxEntry.SelectionTopCellElement<Item> ofItemObject(Item item) {
			return new DropdownBoxEntry.DefaultSelectionTopCellElement<Item>(
			  item, ITEM_FUNCTION, i -> new StringTextComponent(Registry.ITEM.getKey(i).toString())) {
				
				@Override
				public void render(
				  MatrixStack matrices, int mouseX, int mouseY, int x, int y, int width, int height,
				  float delta
				) {
					this.textFieldWidget.x = x + 4;
					this.textFieldWidget.y = y + 6;
					this.textFieldWidget.setWidth(width - 4 - 20);
					this.textFieldWidget.setEnabled(this.getParent().isEditable());
					this.textFieldWidget.setTextColor(this.getPreferredTextColor());
					this.textFieldWidget.render(matrices, mouseX, mouseY, delta);
					ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
					ItemStack stack = this.hasConfigError() ? BARRIER : new ItemStack(
					  this.getValue());
					itemRenderer.renderItemIntoGUI(stack, x + width - 18, y + 2);
				}
			};
		}
		
		public static DropdownBoxEntry.SelectionTopCellElement<Block> ofBlockObject(Block block) {
			return new DropdownBoxEntry.DefaultSelectionTopCellElement<Block>(
			  block, BLOCK_FUNCTION, i -> new StringTextComponent(
			  Registry.BLOCK.getKey(i).toString())) {
				
				@Override
				public void render(
				  MatrixStack matrices, int mouseX, int mouseY, int x, int y, int width, int height,
				  float delta
				) {
					this.textFieldWidget.x = x + 4;
					this.textFieldWidget.y = y + 6;
					this.textFieldWidget.setWidth(width - 4 - 20);
					this.textFieldWidget.setEnabled(this.getParent().isEditable());
					this.textFieldWidget.setTextColor(this.getPreferredTextColor());
					this.textFieldWidget.render(matrices, mouseX, mouseY, delta);
					ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
					ItemStack stack = this.hasConfigError() ? BARRIER : new ItemStack(
					  this.getValue());
					itemRenderer.renderItemIntoGUI(stack, x + width - 18, y + 2);
				}
			};
		}
	}
}

