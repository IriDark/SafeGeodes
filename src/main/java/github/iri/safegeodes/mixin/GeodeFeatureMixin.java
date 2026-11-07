package github.iri.safegeodes.mixin;

import github.iri.safegeodes.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import java.util.*;

@Mixin(GeodeFeature.class)
public class GeodeFeatureMixin{

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    public void place(FeaturePlaceContext<GeodeConfiguration> ctx, CallbackInfoReturnable<Boolean> cir){
        try{
            GeodeConfiguration geodeconfiguration = ctx.config();
            BlockPos origin = ctx.origin();
            int minOffset = geodeconfiguration.minGenOffset;
            int maxOffset = geodeconfiguration.maxGenOffset;

            BoundingBox geodeBox = new BoundingBox(origin.getX() + minOffset, origin.getY() + minOffset, origin.getZ() + minOffset, origin.getX() + maxOffset, origin.getY() + maxOffset, origin.getZ() + maxOffset);
            int minChunkX = (origin.getX() + minOffset) >> 4;
            int maxChunkX = (origin.getX() + maxOffset) >> 4;
            int minChunkZ = (origin.getZ() + minOffset) >> 4;
            int maxChunkZ = (origin.getZ() + maxOffset) >> 4;
            for(int cx = minChunkX; cx <= maxChunkX; cx++){
                for(int cz = minChunkZ; cz <= maxChunkZ; cz++){
                    if (!ctx.level().hasChunk(cx, cz)) continue;

                    ChunkAccess chunk = ctx.level().getChunk(cx, cz);
                    for (StructureStart start : chunk.getAllStarts().values()) {
                        if(!start.isValid() || !start.getBoundingBox().intersects(geodeBox)) continue;
                        for(StructurePiece piece : start.getPieces()){
                            if(piece.getBoundingBox().intersects(geodeBox)){
                                var key = BuiltInRegistries.STRUCTURE_PIECE.getKey(piece.getType());
                                SafeGeodes.LOGGER.debug("Geode Feature Placement is invalid, intersected a structure {} at pos {}", key != null ? key.getPath() : "unknown", geodeBox.getCenter().toShortString());
                                cir.setReturnValue(false);
                                return;
                            }
                        }
                    }
                }
            }
        }catch(Exception e){
            SafeGeodes.LOGGER.warn("Failed to check structure collision for geode at {}: {}", ctx.origin(), e.getMessage());
        }
    }
}