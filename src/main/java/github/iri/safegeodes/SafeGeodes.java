package github.iri.safegeodes;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.*;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SafeGeodes.MODID)
public class SafeGeodes{
    public static final String MODID = "safegeodes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SafeGeodes(IEventBus modEventBus, ModContainer modContainer){

    }
}
