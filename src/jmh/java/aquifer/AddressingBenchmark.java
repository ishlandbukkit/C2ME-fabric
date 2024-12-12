package aquifer;

import net.minecraft.util.math.BlockPos;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode({Mode.AverageTime})
@OperationsPerInvocation(AddressingBenchmark.invocation)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AddressingBenchmark {

    protected static final int invocation = 16 * 16 * (320 - (-64));

    private static int c2me$expNearestSuperiorPow2(int x) { // x > 0
        // https://stackoverflow.com/questions/5242533/fast-way-to-find-exponent-of-nearest-superior-power-of-2
        return 32 - Integer.numberOfLeadingZeros(x - 1);
    }

    private final int startX = -1;
    private final int startY = -7;
    private final int startZ = 18;
    private final int sizeX = 3;
    private final int sizeZ = 3;
    private final int sizeY = 35;
    private final int c2me$shiftY = 4;
    private final int c2me$shiftZ = 2;

    private final long[] blockPositions = new long[sizeX * sizeZ * sizeY];
    private final int[] c2me$blockPos = new int[((1 << c2me$shiftY) * sizeY) << 2];
    private final long[] c2me$blockPosPacked = new long[(1 << c2me$shiftY) * sizeY];

    {
        Random random = new Random(0xcafe);
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
                for (int x = 0; x < this.sizeX; x++) {
                    final int x1 = x + this.startX;
                    final int y1 = y + this.startY;
                    final int z1 = z + this.startZ;
                    int x2 = x1 * 16 + random.nextInt(10);
                    int y2 = y1 * 12 + random.nextInt(9);
                    int z2 = z1 * 16 + random.nextInt(10);
                    int index = this.index(x1, y1, z1);
                    int fastIdx = this.c2me$fastIdx(x1, y1, z1);
                    this.blockPositions[index] = BlockPos.asLong(x2, y2, z2);
                    this.c2me$blockPosPacked[fastIdx] = BlockPos.asLong(x2, y2, z2);
                    int shiftedIdx = fastIdx << 2;
                    this.c2me$blockPos[shiftedIdx + 0] = x2;
                    this.c2me$blockPos[shiftedIdx + 1] = y2;
                    this.c2me$blockPos[shiftedIdx + 2] = z2;
                }
            }
        }
    }

    private int c2me$fastIdx(int x, int y, int z) {
        int offX = x - this.startX;
        int offY = y - this.startY;
        int offZ = z - this.startZ;
        return (offY << this.c2me$shiftY) | (offZ << this.c2me$shiftZ) | offX;
    }

    private int c2me$fastIdxOff(int offX, int offY, int offZ) {
        return (offY << this.c2me$shiftY) | (offZ << this.c2me$shiftZ) | offX;
    }

    private int index(int x, int y, int z) {
        int i = x - this.startX;
        int j = y - this.startY;
        int k = z - this.startZ;
        return (j * this.sizeZ + k) * this.sizeX + i;
    }

    private void solution1(int x, int y, int z, Blackhole bh) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int dist1 = Integer.MAX_VALUE;
        int dist2 = Integer.MAX_VALUE;
        int dist3 = Integer.MAX_VALUE;
        int posIdx1 = 0;
        int posIdx2 = 0;
        int posIdx3 = 0;

        for (int offY = -1; offY <= 1; ++offY) {
            for (int offZ = 0; offZ <= 1; ++offZ) {
                for (int offX = 0; offX <= 1; ++offX) {
                    int posIdx = this.c2me$fastIdx(gx + offX, gy + offY, gz + offZ);

                    int shiftedIdx = posIdx << 2;
                    int dx = this.c2me$blockPos[shiftedIdx + 0] - x;
                    int dy = this.c2me$blockPos[shiftedIdx + 1] - y;
                    int dz = this.c2me$blockPos[shiftedIdx + 2] - z;
                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist1 >= dist) {
                        posIdx3 = posIdx2;
                        dist3 = dist2;
                        posIdx2 = posIdx1;
                        dist2 = dist1;
                        posIdx1 = posIdx;
                        dist1 = dist;
                    } else if (dist2 >= dist) {
                        posIdx3 = posIdx2;
                        dist3 = dist2;
                        posIdx2 = posIdx;
                        dist2 = dist;
                    } else if (dist3 >= dist) {
                        posIdx3 = posIdx;
                        dist3 = dist;
                    }
                }
            }
        }

        bh.consume(dist1);
        bh.consume(dist2);
        bh.consume(dist3);
        bh.consume(posIdx1);
        bh.consume(posIdx2);
        bh.consume(posIdx3);
        bh.consume(0L);
        bh.consume(0L);
        bh.consume(0L);
    }

    private void solution1Off(int x, int y, int z, Blackhole bh) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int gOffX = gx - this.startX;
        int gOffY = gy - this.startY;
        int gOffZ = gz - this.startZ;
        int dist1 = Integer.MAX_VALUE;
        int dist2 = Integer.MAX_VALUE;
        int dist3 = Integer.MAX_VALUE;
        int posIdx1 = 0;
        int posIdx2 = 0;
        int posIdx3 = 0;

        for (int offY = -1; offY <= 1; ++offY) {
            for (int offZ = 0; offZ <= 1; ++offZ) {
                for (int offX = 0; offX <= 1; ++offX) {
                    int posIdx = this.c2me$fastIdxOff(gOffX + offX, gOffY + offY, gOffZ + offZ);

                    int shiftedIdx = posIdx << 2;
                    int dx = this.c2me$blockPos[shiftedIdx + 0] - x;
                    int dy = this.c2me$blockPos[shiftedIdx + 1] - y;
                    int dz = this.c2me$blockPos[shiftedIdx + 2] - z;
                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist1 >= dist) {
                        posIdx3 = posIdx2;
                        dist3 = dist2;
                        posIdx2 = posIdx1;
                        dist2 = dist1;
                        posIdx1 = posIdx;
                        dist1 = dist;
                    } else if (dist2 >= dist) {
                        posIdx3 = posIdx2;
                        dist3 = dist2;
                        posIdx2 = posIdx;
                        dist2 = dist;
                    } else if (dist3 >= dist) {
                        posIdx3 = posIdx;
                        dist3 = dist;
                    }
                }
            }
        }

        bh.consume(dist1);
        bh.consume(dist2);
        bh.consume(dist3);
        bh.consume(posIdx1);
        bh.consume(posIdx2);
        bh.consume(posIdx3);
        bh.consume(0L);
        bh.consume(0L);
        bh.consume(0L);
    }

    private void solution2(int x, int y, int z, Blackhole bh) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int dist1 = Integer.MAX_VALUE;
        int dist2 = Integer.MAX_VALUE;
        int dist3 = Integer.MAX_VALUE;
        long pos1 = 0;
        long pos2 = 0;
        long pos3 = 0;

        for (int offY = -1; offY <= 1; ++offY) {
            for (int offZ = 0; offZ <= 1; ++offZ) {
                for (int offX = 0; offX <= 1; ++offX) {
                    int posIdx = this.c2me$fastIdx(gx + offX, gy + offY, gz + offZ);

                    long position = this.c2me$blockPosPacked[posIdx];

                    int dx = BlockPos.unpackLongX(position) - x;
                    int dy = BlockPos.unpackLongY(position) - y;
                    int dz = BlockPos.unpackLongZ(position) - z;
                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist1 >= dist) {
                        pos3 = pos2;
                        dist3 = dist2;
                        pos2 = pos1;
                        dist2 = dist1;
                        pos1 = position;
                        dist1 = dist;
                    } else if (dist2 >= dist) {
                        pos3 = pos2;
                        dist3 = dist2;
                        pos2 = position;
                        dist2 = dist;
                    } else if (dist3 >= dist) {
                        pos3 = position;
                        dist3 = dist;
                    }
                }
            }
        }

        bh.consume(dist1);
        bh.consume(dist2);
        bh.consume(dist3);
        bh.consume(0);
        bh.consume(0);
        bh.consume(0);
        bh.consume(pos1);
        bh.consume(pos2);
        bh.consume(pos3);
    }

    private void vanilla(int x, int y, int z, Blackhole bh) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int dist1 = Integer.MAX_VALUE;
        int dist2 = Integer.MAX_VALUE;
        int dist3 = Integer.MAX_VALUE;
        long pos1 = 0;
        long pos2 = 0;
        long pos3 = 0;

        for (int offY = -1; offY <= 1; ++offY) {
            for (int offZ = 0; offZ <= 1; ++offZ) {
                for (int offX = 0; offX <= 1; ++offX) {
                    int posIdx = this.index(gx + offX, gy + offY, gz + offZ);

                    long position = this.blockPositions[posIdx];

                    int dx = BlockPos.unpackLongX(position) - x;
                    int dy = BlockPos.unpackLongY(position) - y;
                    int dz = BlockPos.unpackLongZ(position) - z;
                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist1 >= dist) {
                        pos3 = pos2;
                        dist3 = dist2;
                        pos2 = pos1;
                        dist2 = dist1;
                        pos1 = position;
                        dist1 = dist;
                    } else if (dist2 >= dist) {
                        pos3 = pos2;
                        dist3 = dist2;
                        pos2 = position;
                        dist2 = dist;
                    } else if (dist3 >= dist) {
                        pos3 = position;
                        dist3 = dist;
                    }
                }
            }
        }

        bh.consume(dist1);
        bh.consume(dist2);
        bh.consume(dist3);
        bh.consume(0);
        bh.consume(0);
        bh.consume(0);
        bh.consume(pos1);
        bh.consume(pos2);
        bh.consume(pos3);
    }

    private void vanillaOpt1(int x, int y, int z, Blackhole bh) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int dist1 = Integer.MAX_VALUE;
        int dist2 = Integer.MAX_VALUE;
        int dist3 = Integer.MAX_VALUE;
        long pos1 = 0;
        long pos2 = 0;
        long pos3 = 0;

        for (int offY = -1; offY <= 1; ++offY) {
            for (int offZ = 0; offZ <= 1; ++offZ) {
                for (int offX = 0; offX <= 1; ++offX) {
                    int posIdx = this.index(gx + offX, gy + offY, gz + offZ);

                    long position = this.blockPositions[posIdx];

                    int dx = BlockPos.unpackLongX(position) - x;
                    int dy = BlockPos.unpackLongY(position) - y;
                    int dz = BlockPos.unpackLongZ(position) - z;
                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist3 >= dist) {
                        pos3 = position;
                        dist3 = dist;
                    }
                    if (dist2 >= dist) {
                        pos3 = pos2;
                        dist3 = dist2;
                        pos2 = position;
                        dist2 = dist;
                    }
                    if (dist1 >= dist) {
                        pos2 = pos1;
                        dist2 = dist1;
                        pos1 = position;
                        dist1 = dist;
                    }
                }
            }
        }

        bh.consume(dist1);
        bh.consume(dist2);
        bh.consume(dist3);
        bh.consume(0);
        bh.consume(0);
        bh.consume(0);
        bh.consume(pos1);
        bh.consume(pos2);
        bh.consume(pos3);
    }

    private void vanillaOpt2(int x, int y, int z, Blackhole bh) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int dist1 = Integer.MAX_VALUE;
        int dist2 = Integer.MAX_VALUE;
        int dist3 = Integer.MAX_VALUE;
        long pos1 = 0;
        long pos2 = 0;
        long pos3 = 0;

        for (int offY = -1; offY <= 1; ++offY) {
            for (int offZ = 0; offZ <= 1; ++offZ) {
                int posIdx0 = this.index(gx, gy + offY, gz + offZ);
                int posIdx1 = posIdx0 + 1;

                long position0 = this.blockPositions[posIdx0];
                long position1 = this.blockPositions[posIdx1];

                int dx0 = BlockPos.unpackLongX(position0) - x;
                int dx1 = BlockPos.unpackLongX(position1) - x;
                int dy0 = BlockPos.unpackLongY(position0) - y;
                int dy1 = BlockPos.unpackLongY(position1) - y;
                int dz0 = BlockPos.unpackLongZ(position0) - z;
                int dz1 = BlockPos.unpackLongZ(position1) - z;
                int dist_0 = dx0 * dx0 + dy0 * dy0 + dz0 * dz0;
                int dist_1 = dx1 * dx1 + dy1 * dy1 + dz1 * dz1;
                if (dist3 >= dist_0) {
                    pos3 = position0;
                    dist3 = dist_0;
                }
                if (dist2 >= dist_0) {
                    pos3 = pos2;
                    dist3 = dist2;
                    pos2 = position0;
                    dist2 = dist_0;
                }
                if (dist1 >= dist_0) {
                    pos2 = pos1;
                    dist2 = dist1;
                    pos1 = position0;
                    dist1 = dist_0;
                }
                if (dist3 >= dist_1) {
                    pos3 = position1;
                    dist3 = dist_1;
                }
                if (dist2 >= dist_1) {
                    pos3 = pos2;
                    dist3 = dist2;
                    pos2 = position1;
                    dist2 = dist_1;
                }
                if (dist1 >= dist_1) {
                    pos2 = pos1;
                    dist2 = dist1;
                    pos1 = position1;
                    dist1 = dist_1;
                }
            }
        }

        bh.consume(dist1);
        bh.consume(dist2);
        bh.consume(dist3);
        bh.consume(0);
        bh.consume(0);
        bh.consume(0);
        bh.consume(pos1);
        bh.consume(pos2);
        bh.consume(pos3);
    }

    private static final long[] posCache = new long[12];
    private static final int[] distCache = new int[12];

    private void vanillaOptFailed1(int x, int y, int z, Blackhole bh) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int dist1 = Integer.MAX_VALUE;
        int dist2 = Integer.MAX_VALUE;
        int dist3 = Integer.MAX_VALUE;
        long pos1 = 0;
        long pos2 = 0;
        long pos3 = 0;

        int index = 0;
        for (int offY = -1; offY <= 1; ++offY) {
            for (int offZ = 0; offZ <= 1; ++offZ) {
                int posIdx0 = this.index(gx, gy + offY, gz + offZ);
                int posIdx1 = posIdx0 + 1;

                long position0 = this.blockPositions[posIdx0];
                long position1 = this.blockPositions[posIdx1];

                int dx0 = BlockPos.unpackLongX(position0) - x;
                int dx1 = BlockPos.unpackLongX(position1) - x;
                int dy0 = BlockPos.unpackLongY(position0) - y;
                int dy1 = BlockPos.unpackLongY(position1) - y;
                int dz0 = BlockPos.unpackLongZ(position0) - z;
                int dz1 = BlockPos.unpackLongZ(position1) - z;
                int dist_0 = dx0 * dx0 + dy0 * dy0 + dz0 * dz0;
                int dist_1 = dx1 * dx1 + dy1 * dy1 + dz1 * dz1;

                posCache[index] = position0;
                distCache[index] = dist_0;
                posCache[index + 1] = position1;
                distCache[index + 1] = dist_1;
                index += 2;
            }
        }

        for (int i = 0; i < 12; i ++) {
            int dist = distCache[i];
            long position = posCache[i];
            if (dist3 >= dist) {
                pos3 = position;
                dist3 = dist;
            }
            if (dist2 >= dist) {
                pos3 = pos2;
                dist3 = dist2;
                pos2 = position;
                dist2 = dist;
            }
            if (dist1 >= dist) {
                pos2 = pos1;
                dist2 = dist1;
                pos1 = position;
                dist1 = dist;
            }
        }

        bh.consume(dist1);
        bh.consume(dist2);
        bh.consume(dist3);
        bh.consume(0);
        bh.consume(0);
        bh.consume(0);
        bh.consume(pos1);
        bh.consume(pos2);
        bh.consume(pos3);
    }

    private void vanillaOpt3(int x, int y, int z, Blackhole bh) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int dist1 = Integer.MAX_VALUE;
        int dist2 = Integer.MAX_VALUE;
        int dist3 = Integer.MAX_VALUE;
        long pos1 = 0;
        long pos2 = 0;
        long pos3 = 0;

        for (int offY = -1; offY <= 1; ++offY) {
            for (int offZ = 0; offZ <= 1; ++offZ) {
                int posIdx0 = this.index(gx, gy + offY, gz + offZ);

                long position0 = this.blockPositions[posIdx0];

                int dx0 = BlockPos.unpackLongX(position0) - x;
                int dz0 = BlockPos.unpackLongZ(position0) - z;
                int dy0 = BlockPos.unpackLongY(position0) - y;
                int dist_0 = dx0 * dx0 + dy0 * dy0 + dz0 * dz0;

                int posIdx1 = posIdx0 + 1;
                long position1 = this.blockPositions[posIdx1];
                int dx1 = BlockPos.unpackLongX(position1) - x;
                int dy1 = BlockPos.unpackLongY(position1) - y;
                int dz1 = BlockPos.unpackLongZ(position1) - z;
                int dist_1 = dx1 * dx1 + dy1 * dy1 + dz1 * dz1;
                if (dist3 >= dist_0) {
                    pos3 = position0;
                    dist3 = dist_0;
                }
                if (dist2 >= dist_0) {
                    pos3 = pos2;
                    dist3 = dist2;
                    pos2 = position0;
                    dist2 = dist_0;
                }
                if (dist1 >= dist_0) {
                    pos2 = pos1;
                    dist2 = dist1;
                    pos1 = position0;
                    dist1 = dist_0;
                }
                if (dist3 >= dist_1) {
                    pos3 = position1;
                    dist3 = dist_1;
                }
                if (dist2 >= dist_1) {
                    pos3 = pos2;
                    dist3 = dist2;
                    pos2 = position1;
                    dist2 = dist_1;
                }
                if (dist1 >= dist_1) {
                    pos2 = pos1;
                    dist2 = dist1;
                    pos1 = position1;
                    dist1 = dist_1;
                }
            }
        }

        bh.consume(dist1);
        bh.consume(dist2);
        bh.consume(dist3);
        bh.consume(0);
        bh.consume(0);
        bh.consume(0);
        bh.consume(pos1);
        bh.consume(pos2);
        bh.consume(pos3);
    }

    @Benchmark
    public void benchSolution1(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    solution1(x, y, z, bh);
                }
            }
        }
    }

    @Benchmark
    public void benchSolution1Off(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    solution1Off(x, y, z, bh);
                }
            }
        }
    }

    @Benchmark
    public void benchSolution2(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    solution2(x, y, z, bh);
                }
            }
        }
    }

    @Benchmark
    public void benchVanilla(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    vanilla(x, y, z, bh);
                }
            }
        }
    }

    @Benchmark
    public void benchVanillaOpt1(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    vanillaOpt1(x, y, z, bh);
                }
            }
        }
    }

    @Benchmark
    public void benchVanillaOpt2(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    vanillaOpt2(x, y, z, bh);
                }
            }
        }
    }

    @Benchmark
    public void benchVanillaOptFailed1(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    vanillaOptFailed1(x, y, z, bh);
                }
            }
        }
    }

    @Benchmark
    public void benchVanillaOpt3(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    vanillaOpt3(x, y, z, bh);
                }
            }
        }
    }

}
