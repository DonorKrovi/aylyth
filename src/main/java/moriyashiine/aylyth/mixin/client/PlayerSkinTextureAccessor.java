package moriyashiine.aylyth.mixin.client;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;

import java.io.File;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerSkinTexture.class)
public interface PlayerSkinTextureAccessor {

    @Accessor("cacheFile")
    File getCacheFile();

    @Invoker("remapTexture")
    NativeImage invokeRemapTexture(NativeImage nativeImage);
}
