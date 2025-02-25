package moriyashiine.aylyth.client.render.item;

import com.mojang.datafixers.util.Unit;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.profiler.Profiler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class BigItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer, IdentifiableResourceReloadListener {
	private final Identifier id;
	private final Identifier itemId;
	private ItemRenderer itemRenderer;
	private BakedModel inventoryModel;
	private BakedModel worldModel;

	public BigItemRenderer(Identifier id) {
		this.id = new Identifier(id + "_renderer");
		this.itemId = id;
	}

	@Override
	public Identifier getFabricId() {
		return this.id;
	}

	@Override
	public CompletableFuture<Void> reload(ResourceReloader.Synchronizer synchronizer, ResourceManager manager, Profiler prepareProfiler, Profiler applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
		return synchronizer.whenPrepared(Unit.INSTANCE).thenRunAsync(() -> {
			applyProfiler.startTick();
			applyProfiler.push("listener");
			final MinecraftClient client = MinecraftClient.getInstance();
			this.itemRenderer = client.getItemRenderer();
			this.inventoryModel = client.getBakedModelManager().getModel(new ModelIdentifier(this.itemId + "_gui", "inventory"));
			this.worldModel = client.getBakedModelManager().getModel(new ModelIdentifier(this.itemId + "_handheld", "inventory"));
			applyProfiler.pop();
			applyProfiler.endTick();
		}, applyExecutor);
	}

	@Override
	public void render(ItemStack stack, ModelTransformation.Mode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		matrices.pop();
		matrices.push();
		if(itemRenderer != null) {
			if (mode != ModelTransformation.Mode.FIRST_PERSON_LEFT_HAND && mode != ModelTransformation.Mode.FIRST_PERSON_RIGHT_HAND && mode != ModelTransformation.Mode.THIRD_PERSON_LEFT_HAND && mode != ModelTransformation.Mode.THIRD_PERSON_RIGHT_HAND && mode != ModelTransformation.Mode.NONE) {
				itemRenderer.renderItem(stack, mode, false, matrices, vertexConsumers, light, overlay, this.inventoryModel);
			} else {
				boolean leftHanded = switch (mode) {
					case FIRST_PERSON_LEFT_HAND, THIRD_PERSON_LEFT_HAND -> true;
					default -> false;
				};
				if (MinecraftClient.getInstance().player.getActiveItem() == stack) { // TODO: Find a more permanent solution if this doesn't work out in the future
					if (mode.isFirstPerson()) {
						matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-45));
						matrices.translate(0.15, -0.25, 0);
					} else {
						matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180));
					}
				}

				itemRenderer.renderItem(stack, mode, leftHanded, matrices, vertexConsumers, light, overlay, this.worldModel);
			}
		}
	}
}