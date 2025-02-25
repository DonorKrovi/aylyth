package moriyashiine.aylyth.common.registry;

import moriyashiine.aylyth.common.util.AylythUtil;
import moriyashiine.aylyth.common.screenhandler.TulpaScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.registry.Registry;

public class ModScreenHandlers {
    public static final ScreenHandlerType<TulpaScreenHandler> TULPA_SCREEN_HANDLER;

    static {
        TULPA_SCREEN_HANDLER = Registry.register(
                Registry.SCREEN_HANDLER,
                AylythUtil.id("tulpa_screen"),
                new ExtendedScreenHandlerType<>(TulpaScreenHandler::new)
        );
    }

    public static void init() {
    }
}
