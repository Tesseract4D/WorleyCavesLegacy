package cn.tesseract.worleycaves.mixin;

import cn.tesseract.worleycaves.world.WorleyCavesGenerator;
import net.minecraft.world.World;
import net.minecraft.world.chunk.SurfaceChunkGenerator;
import net.minecraft.world.gen.carver.Carver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SurfaceChunkGenerator.class)
public class SurfaceChunkGeneratorMixin {
    @Shadow
    private Carver caveCarver;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(World world, long l, boolean bl, String string, CallbackInfo info) {
        caveCarver = new WorleyCavesGenerator(l);
    }
}
