package cn.tesseract.worleycaves.world;

import cn.tesseract.worleycaves.util.FastNoise;
import cn.tesseract.worleycaves.util.WorleyUtil;
import net.minecraft.block.AbstractFluidBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SandBlock;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkBlockStateStorage;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.gen.carver.Carver;
import net.minecraft.world.gen.carver.CaveCarver;

import java.util.Random;

public class WorleyCavesGenerator extends Carver {
    private static final BlockState lava = Blocks.LAVA.getDefaultState();
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState SAND = Blocks.SAND.getDefaultState();
    private static final BlockState SANDSTONE = Blocks.SANDSTONE.getDefaultState();
    private static final BlockState RED_SAND = Blocks.SAND.getDefaultState().with(SandBlock.sandType, SandBlock.SandType.RED_SAND);
    private static final BlockState RED_SANDSTONE = Blocks.RED_SANDSTONE.getDefaultState();

    private CaveCarver surfaceCaves = new SurfaceCaveCarver();
    private WorleyUtil worleyF1divF3;
    private FastNoise displacementNoisePerlin;
    private static int maxCaveHeight;
    private static int minCaveHeight;
    private static float noiseCutoff;
    private static float warpAmplifier;
    private static float easeInDepth;
    private static float yCompression;
    private static float xzCompression;
    private static int lavaDepth;
    private static int HAS_CAVES_FLAG = 129;

    public WorleyCavesGenerator(long seed) {
        int seed2 = new Random(seed).nextInt();
        worleyF1divF3 = new WorleyUtil(seed2);
        worleyF1divF3.SetFrequency(0.016f);

        displacementNoisePerlin = new FastNoise(seed2);
        displacementNoisePerlin.SetNoiseType(FastNoise.NoiseType.Perlin);
        displacementNoisePerlin.SetFrequency(0.05f);

        maxCaveHeight = 128;
        minCaveHeight = 1;
        noiseCutoff = -0.16f;
        warpAmplifier = 8.0f;
        easeInDepth = 15;
        yCompression = 2.0f;
        xzCompression = 1.0f;
        lavaDepth = 6;
    }

    @Override
    public void carveRegion(ChunkProvider chunkProvider, World world, int x, int z, ChunkBlockStateStorage chunkStorage) {
        int currentDim = world.dimension.getType();
        this.world = world;

        if (currentDim != 0) {
            return;
        }

        this.generateWorleyCaves(world, x, z, chunkStorage);
        this.surfaceCaves.carveRegion(chunkProvider, world, x, z, chunkStorage);
    }

    protected void generateWorleyCaves(World worldIn, int chunkX, int chunkZ, ChunkBlockStateStorage chunkPrimerIn) {
        int chunkMaxHeight = getMaxSurfaceHeight(chunkPrimerIn);
        int seaLevel = worldIn.getSeaLevel();
        float[][][] samples = sampleNoise(chunkX, chunkZ, chunkMaxHeight + 1);
        float oneQuarter = 0.25F;
        float oneHalf = 0.5F;
        Biome currentBiome;
        BlockPos realPos;

        for (int x = 0; x < 4; x++) {
            //each chunk divided into 4 subchunks along Z axis
            for (int z = 0; z < 4; z++) {
                int depth = 0;

                //don't bother checking all the other logic if there's nothing to dig in this column
                if (samples[x][HAS_CAVES_FLAG][z] == 0 && samples[x + 1][HAS_CAVES_FLAG][z] == 0 && samples[x][HAS_CAVES_FLAG][z + 1] == 0 && samples[x + 1][HAS_CAVES_FLAG][z + 1] == 0)
                    continue;

                //each chunk divided into 128 subchunks along Y axis. Need lots of y sample points to not break things
                for (int y = (maxCaveHeight / 2) - 1; y >= 0; y--) {
                    //grab the 8 sample points needed from the noise values
                    float x0y0z0 = samples[x][y][z];
                    float x0y0z1 = samples[x][y][z + 1];
                    float x1y0z0 = samples[x + 1][y][z];
                    float x1y0z1 = samples[x + 1][y][z + 1];
                    float x0y1z0 = samples[x][y + 1][z];
                    float x0y1z1 = samples[x][y + 1][z + 1];
                    float x1y1z0 = samples[x + 1][y + 1][z];
                    float x1y1z1 = samples[x + 1][y + 1][z + 1];

                    //how much to increment noise along y value
                    //linear interpolation from start y and end y
                    float noiseStepY00 = (x0y1z0 - x0y0z0) * -oneHalf;
                    float noiseStepY01 = (x0y1z1 - x0y0z1) * -oneHalf;
                    float noiseStepY10 = (x1y1z0 - x1y0z0) * -oneHalf;
                    float noiseStepY11 = (x1y1z1 - x1y0z1) * -oneHalf;

                    //noise values of 4 corners at y=0
                    float noiseStartX0 = x0y0z0;
                    float noiseStartX1 = x0y0z1;
                    float noiseEndX0 = x1y0z0;
                    float noiseEndX1 = x1y0z1;

                    // loop through 2 blocks of the Y subchunk
                    for (int suby = 1; suby >= 0; suby--) {
                        int localY = suby + y * 2;
                        float noiseStartZ = noiseStartX0;
                        float noiseEndZ = noiseStartX1;

                        //how much to increment X values, linear interpolation
                        float noiseStepX0 = (noiseEndX0 - noiseStartX0) * oneQuarter;
                        float noiseStepX1 = (noiseEndX1 - noiseStartX1) * oneQuarter;

                        // loop through 4 blocks of the X subchunk
                        for (int subx = 0; subx < 4; subx++) {
                            int localX = subx + x * 4;
                            int realX = localX + chunkX * 16;

                            //how much to increment Z values, linear interpolation
                            float noiseStepZ = (noiseEndZ - noiseStartZ) * oneQuarter;

                            //Y and X already interpolated, just need to interpolate final 4 Z block to get final noise value
                            float noiseVal = noiseStartZ;

                            // loop through 4 blocks of the Z subchunk
                            for (int subz = 0; subz < 4; subz++) {
                                int localZ = subz + z * 4;
                                int realZ = localZ + chunkZ * 16;
                                realPos = new BlockPos(realX, localY, realZ);
                                currentBiome = null;

                                if (depth == 0) {
                                    //only checks depth once per 4x4 subchunk
                                    if (subx == 0 && subz == 0) {
                                        BlockState currentBlock = chunkPrimerIn.get(localX, localY, localZ);
                                        currentBiome = world.dimension.getBiomeSource().getBiomeAt(realPos, Biome.PLAINS);//world.getBiome(realPos);

                                        //use isDigable to skip leaves/wood getting counted as surface
                                        if (canReplaceBlock(currentBlock, AIR) || isBiomeBlock(chunkPrimerIn, realX, realZ, currentBlock, currentBiome)) {
                                            depth++;
                                        }
                                    } else {
                                        continue;
                                    }
                                } else if (subx == 0 && subz == 0) {
                                    depth++;
                                }

                                float adjustedNoiseCutoff = noiseCutoff;
                                if (depth < easeInDepth) {
                                    adjustedNoiseCutoff = 1;
                                }

                                if (localY < (minCaveHeight + 5)) {
                                    adjustedNoiseCutoff += ((minCaveHeight + 5) - localY) * 0.05;
                                }

                                if (noiseVal > adjustedNoiseCutoff) {
                                    BlockState aboveBlock = chunkPrimerIn.get(localX, localY + 1, localZ);
                                    if (!isFluidBlock(aboveBlock) || localY <= lavaDepth) {
                                        //if we are in the easeInDepth range or near sea level or subH2O is installed, do some extra checks for water before digging
                                        if ((depth < easeInDepth || localY > (seaLevel - 8)) && localY > lavaDepth) {
                                            if (localX < 15)
                                                if (isFluidBlock(chunkPrimerIn.get(localX + 1, localY, localZ)))
                                                    continue;
                                            if (localX > 0)
                                                if (isFluidBlock(chunkPrimerIn.get(localX - 1, localY, localZ)))
                                                    continue;
                                            if (localZ < 15)
                                                if (isFluidBlock(chunkPrimerIn.get(localX, localY, localZ + 1)))
                                                    continue;
                                            if (localZ > 0)
                                                if (isFluidBlock(chunkPrimerIn.get(localX, localY, localZ - 1)))
                                                    continue;
                                        }
                                        BlockState currentBlock = chunkPrimerIn.get(localX, localY, localZ);
                                        if (currentBiome == null)
                                            currentBiome = world.dimension.getBiomeSource().getBiomeAt(realPos, Biome.PLAINS);//world.getBiome(realPos);

                                        boolean foundTopBlock = isTopBlock(currentBlock, currentBiome);
                                        digBlock(chunkPrimerIn, localX, localY, localZ, chunkX, chunkZ, foundTopBlock, currentBlock, aboveBlock, currentBiome);
                                    }
                                }

                                noiseVal += noiseStepZ;
                            }

                            noiseStartZ += noiseStepX0;
                            noiseEndZ += noiseStepX1;
                        }

                        noiseStartX0 += noiseStepY00;
                        noiseStartX1 += noiseStepY01;
                        noiseEndX0 += noiseStepY10;
                        noiseEndX1 += noiseStepY11;
                    }
                }
            }
        }
    }

    public float[][][] sampleNoise(int chunkX, int chunkZ, int maxSurfaceHeight) {
        int originalMaxHeight = 128;
        float[][][] noiseSamples = new float[5][130][5];
        float noise;
        for (int x = 0; x < 5; x++) {
            int realX = x * 4 + chunkX * 16;
            for (int z = 0; z < 5; z++) {
                int realZ = z * 4 + chunkZ * 16;

                int columnHasCaveFlag = 0;

                //loop from top down for y values so we can adjust noise above current y later on
                for (int y = 128; y >= 0; y--) {
                    float realY = y * 2;
                    if (realY > maxSurfaceHeight || realY > maxCaveHeight || realY < minCaveHeight) {
                        //if outside of valid cave range set noise value below normal minimum of -1.0
                        noiseSamples[x][y][z] = -1.1F;
                    } else {
                        //Experiment making the cave system more chaotic the more you descend 
                        ///TODO might be too dramatic down at lava level
                        float dispAmp = (float) (warpAmplifier * ((originalMaxHeight - y) / (originalMaxHeight * 0.85)));

                        float xDisp = displacementNoisePerlin.GetNoise(realX, realZ) * dispAmp;
                        float yDisp = displacementNoisePerlin.GetNoise(realX, realZ + 67.0f) * dispAmp;
                        float zDisp = displacementNoisePerlin.GetNoise(realX, realZ + 149.0f) * dispAmp;

                        //doubling the y frequency to get some more caves
                        noise = worleyF1divF3.SingleCellular3Edge(realX * xzCompression + xDisp, realY * yCompression + yDisp, realZ * xzCompression + zDisp);
                        noiseSamples[x][y][z] = noise;

                        if (noise > noiseCutoff) {
                            columnHasCaveFlag = 1;
                            //if noise is below cutoff, adjust values of neighbors
                            //helps prevent caves fracturing during interpolation

                            if (x > 0)
                                noiseSamples[x - 1][y][z] = (noise * 0.2f) + (noiseSamples[x - 1][y][z] * 0.8f);
                            if (z > 0)
                                noiseSamples[x][y][z - 1] = (noise * 0.2f) + (noiseSamples[x][y][z - 1] * 0.8f);

                            //more heavily adjust y above 'air block' noise values to give players more headroom
                            if (y < 128) {
                                float noiseAbove = noiseSamples[x][y + 1][z];
                                if (noise > noiseAbove)
                                    noiseSamples[x][y + 1][z] = (noise * 0.8F) + (noiseAbove * 0.2F);
                                if (y < 127) {
                                    float noiseTwoAbove = noiseSamples[x][y + 2][z];
                                    if (noise > noiseTwoAbove)
                                        noiseSamples[x][y + 2][z] = (noise * 0.35F) + (noiseTwoAbove * 0.65F);
                                }
                            }

                        }
                    }
                }
                noiseSamples[x][HAS_CAVES_FLAG][z] = columnHasCaveFlag; //used to skip cave digging logic when we know there is nothing to dig out
            }
        }
        return noiseSamples;
    }

    private int getSurfaceHeight(ChunkBlockStateStorage chunkPrimerIn, int localX, int localZ) {
        //Using a recursive binary search to find the surface
        return recursiveBinarySurfaceSearch(chunkPrimerIn, localX, localZ, 255, 0);
    }

    //Recursive binary search, this search always converges on the surface in 8 in cycles for the range 255 >= y >= 0
    private int recursiveBinarySurfaceSearch(ChunkBlockStateStorage chunkPrimer, int localX, int localZ, int searchTop, int searchBottom) {
        int top = searchTop;
        if (searchTop > searchBottom) {
            int searchMid = (searchBottom + searchTop) / 2;
            if (canReplaceBlock(chunkPrimer.get(localX, searchMid, localZ), AIR)) {
                top = recursiveBinarySurfaceSearch(chunkPrimer, localX, localZ, searchTop, searchMid + 1);
            } else {
                top = recursiveBinarySurfaceSearch(chunkPrimer, localX, localZ, searchMid, searchBottom);
            }
        }
        return top;
    }

    //tests 6 points in hexagon pattern get max height of chunk
    private int getMaxSurfaceHeight(ChunkBlockStateStorage primer) {
        int y = 0;
        int[] cords = {2, 6, 3, 11, 7, 2, 9, 13, 12, 4, 13, 9};

        for (int i = 0; i < cords.length; i += 2) {
            int test = recursiveBinarySurfaceSearch(primer, cords[i], cords[i + 1], 255, 0);
            if (test > y) {
                y = test;
                if (y > maxCaveHeight)
                    return y;
            }
        }
        return y;
    }

    //returns true if block matches the top or filler block of the location biome
    private boolean isBiomeBlock(ChunkBlockStateStorage primer, int realX, int realZ, BlockState state, Biome biome) {
        return state == biome.topBlock || state == biome.baseBlock;
    }

    //returns true if block is fluid, trying to play nice with modded liquid
    private boolean isFluidBlock(BlockState state) {
        return state.getBlock() instanceof AbstractFluidBlock;
    }

    private boolean isTopBlock(BlockState state, Biome biome) {
        return (isExceptionBiome(biome) ? state.getBlock() == Blocks.GRASS : state == biome.topBlock);
    }

    private boolean isExceptionBiome(net.minecraft.world.biome.Biome biome) {
        return biome == Biome.BEACH || biome == Biome.DESERT;
    }

    protected boolean canReplaceBlock(BlockState state, BlockState stateUp) {
        return state.getBlock().getMaterial() == Material.STONE;
    }

    protected void digBlock(ChunkBlockStateStorage data, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop, BlockState state, BlockState up, Biome biome) {
        BlockState top = biome.topBlock;
        BlockState filler = biome.baseBlock;


        if (this.canReplaceBlock(state, up) || state.getBlock() == top.getBlock() || state.getBlock() == filler.getBlock()) {
            if (y <= lavaDepth) {
                data.set(x, y, z, lava);
            } else {
                data.set(x, y, z, AIR);

                if (foundTop && data.get(x, y - 1, z).getBlock() == filler.getBlock()) {
                    data.set(x, y - 1, z, top);
                }

                if (up == SAND) {
                    data.set(x, y + 1, z, SAND);
                } else if (up == RED_SAND) {
                    data.set(x, y + 1, z, RED_SANDSTONE);
                }
            }
        }
    }
}
