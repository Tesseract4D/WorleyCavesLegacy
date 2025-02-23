package cn.tesseract.worleycaves.world;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkBlockStateStorage;
import net.minecraft.world.gen.carver.CaveCarver;

public class SurfaceCaveCarver extends CaveCarver {
    protected void carve(World worldIn, int dx, int dz, int cx, int cz, ChunkBlockStateStorage chunkStorage) {
        int topY = 128, bottomY = 40;
        int numAttempts = 0;
        if (this.random.nextInt(100) < 7) {
            numAttempts = this.random.nextInt(this.random.nextInt(this.random.nextInt(15) + 1) + 1);
        }

        for (int i = 0; i < numAttempts; ++i) {
            double caveStartX = (dx << 4) + this.random.nextInt(16);
            double caveStartY = this.random.nextInt(topY - bottomY) + bottomY;
            double caveStartZ = (dz << 4) + this.random.nextInt(16);

            int numAddTunnelCalls = 1;


            for (int j = 0; j < numAddTunnelCalls; ++j) {
                float yaw = this.random.nextFloat() * ((float) Math.PI * 2F);
                float pitch = (this.random.nextFloat() - 0.5F) * 2.0F / 8.0F;
                float width = this.random.nextFloat() * 2.0F + this.random.nextFloat();

                this.carveCave(this.random.nextLong(), cx, cz, chunkStorage, caveStartX, caveStartY, caveStartZ, width, yaw, pitch, 0, 0, 1.0D);
            }
        }
    }
}
