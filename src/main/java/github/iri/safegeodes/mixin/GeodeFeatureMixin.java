package github.iri.safegeodes.mixin;

import github.iri.safegeodes.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.structure.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(GeodeFeature.class)
public class GeodeFeatureMixin{

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    public void place(FeaturePlaceContext<GeodeConfiguration> ctx, CallbackInfoReturnable<Boolean> cir){
        try{
            GeodeConfiguration geodeconfiguration = ctx.config();
            BlockPos origin = ctx.origin();
            int minOffset = geodeconfiguration.minGenOffset;
            int maxOffset = geodeconfiguration.maxGenOffset;
            ServerLevel serverLevel = ctx.level() instanceof WorldGenRegion region ? region.getLevel() : (ctx.level() instanceof ServerLevel sl ? sl : null);
            
            BoundingBox geodeBox = new BoundingBox(origin.getX() + minOffset, origin.getY() + minOffset, origin.getZ() + minOffset, origin.getX() + maxOffset, origin.getY() + maxOffset, origin.getZ() + maxOffset);
            int minChunkX = (origin.getX() + minOffset) >> 4;
            int maxChunkX = (origin.getX() + maxOffset) >> 4;
            int minChunkZ = (origin.getZ() + minOffset) >> 4;
            int maxChunkZ = (origin.getZ() + maxOffset) >> 4;

            for(int cx = minChunkX; cx <= maxChunkX; cx++){
                for(int cz = minChunkZ; cz <= maxChunkZ; cz++){
                    if(!ctx.level().hasChunk(cx, cz)) continue;
                    ChunkAccess chunk = ctx.level().getChunk(cx, cz);
                    safeGeodes$checkDirectStarts(chunk, geodeBox, cir);
                    if(cir.isCancelled()) return;

                    safeGeodes$checkIndirectStarts(ctx, cir, chunk, serverLevel, geodeBox);
                    if(cir.isCancelled()) return;
                }
            }
        }catch(Exception e){
            SafeGeodes.LOGGER.warn("Failed to check structure collision for geode at {}: {}", ctx.origin(), e.getMessage());
        }
    }

    @Unique
    private void safeGeodes$checkIndirectStarts(FeaturePlaceContext<GeodeConfiguration> ctx, CallbackInfoReturnable<Boolean> cir, ChunkAccess chunk, ServerLevel serverLevel, BoundingBox geodeBox){
        for(var entry : chunk.getAllReferences().entrySet()){
            Structure structure = entry.getKey();
            for(long refPos : entry.getValue()){
                int startX = ChunkPos.getX(refPos);
                int startZ = ChunkPos.getZ(refPos);

                ChunkAccess startChunk = null;
                if(ctx.level().hasChunk(startX, startZ)){
                    startChunk = ctx.level().getChunk(startX, startZ);
                }else if(serverLevel != null){
                    startChunk = serverLevel.getChunk(startX, startZ, ChunkStatus.STRUCTURE_STARTS, false);
                }

                if(startChunk == null) continue;
                StructureStart start = startChunk.getStartForStructure(structure);
                if(start != null && start.isValid() && start.getBoundingBox().intersects(geodeBox)){
                    for(StructurePiece piece : start.getPieces()){
                        if(piece.getBoundingBox().intersects(geodeBox)){
                            var key = BuiltInRegistries.STRUCTURE_PIECE.getKey(piece.getType());
                            SafeGeodes.LOGGER.debug("(indirect) Geode intersected structure piece {} from reference at {}", key != null ? key.getPath() : "unknown", geodeBox.getCenter().toShortString());
                            cir.setReturnValue(false);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Unique
    private void safeGeodes$checkDirectStarts(ChunkAccess chunk, BoundingBox geodeBox, CallbackInfoReturnable<Boolean> cir){
        for(StructureStart start : chunk.getAllStarts().values()){
            if(!start.isValid() || !start.getBoundingBox().intersects(geodeBox)) continue;
            for(StructurePiece piece : start.getPieces()){
                if(piece.getBoundingBox().intersects(geodeBox)){
                    var key = BuiltInRegistries.STRUCTURE_PIECE.getKey(piece.getType());
                    SafeGeodes.LOGGER.debug("Geode Feature Placement is invalid, intersected a structure {} at pos {}", key != null ? key.getPath() : "unknown", geodeBox.getCenter().toShortString());
                    cir.setReturnValue(false);
                }
            }
        }
    }
}