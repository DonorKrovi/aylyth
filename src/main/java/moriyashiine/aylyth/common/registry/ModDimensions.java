package moriyashiine.aylyth.common.registry;

import moriyashiine.aylyth.common.Aylyth;
import moriyashiine.aylyth.common.world.dimension.AylythBiomeSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;

public class ModDimensions {

	public static void datagenInit() {}

	//refer to json files in data
	public static final RegistryKey<DimensionOptions> AYLYTH_DIMENSION_KEY = RegistryKey.of(Registry.DIMENSION_KEY, new Identifier(Aylyth.MOD_ID, "aylyth"));
	
	public static final RegistryKey<World> AYLYTH = RegistryKey.of(Registry.WORLD_KEY, AYLYTH_DIMENSION_KEY.getValue());

	static {
		Registry.register(Registry.BIOME_SOURCE, new Identifier(Aylyth.MOD_ID, "aylyth_biome_provider"), AylythBiomeSource.CODEC);
	}
}
