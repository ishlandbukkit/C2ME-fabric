package aquifer;

import com.ishland.c2me.opts.natives_math.common.BindingsTemplate;
import natives.Base_x86_64;
import net.minecraft.util.math.BlockPos;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode({Mode.AverageTime})
@OperationsPerInvocation(AddressingBenchmark.invocation)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AddressingBenchmark extends Base_x86_64 {

    protected static final int invocation = 16 * 16 * (320 - (-64));

    public AddressingBenchmark() {
        super(BindingsTemplate.c2me_natives_aquifer_refreshDistPosIdx_ptr, "c2me_natives_aquifer_refreshDistPosIdx");
    }

    private static int c2me$expNearestSuperiorPow2(int x) { // x > 0
        // https://stackoverflow.com/questions/5242533/fast-way-to-find-exponent-of-nearest-superior-power-of-2
        return 32 - Integer.numberOfLeadingZeros(x - 1);
    }

    private static final Arena ARENA = Arena.ofAuto();

    private final int startX = -1;
    private final int startY = -7;
    private final int startZ = 18;
    private final int sizeX = 3;
    private final int sizeZ = 3;
    private final int sizeY = 35;
    private final int c2me$shiftY = 4;
    private final int c2me$shiftZ = 2;
    private final MemorySegment c2me$aquiferData = ARENA.allocate(5 * 4);
    private final long c2me$aquiferDataAddress = c2me$aquiferData.address();
    {
        MemorySegment.copy(new int[] {startX, startY, startZ, sizeX, sizeZ}, 0, c2me$aquiferData, ValueLayout.JAVA_INT, 0, 5);
    }

    private final long[] blockPositions = new long[sizeX * sizeZ * sizeY];
    private final short[] c2me$packedBlockPositions = new short[sizeX * sizeZ * sizeY];
    private final MemorySegment c2me$packedBlockPositionsSegment = ARENA.allocate(c2me$packedBlockPositions.length * 2);
    private final long c2me$packedBlockPositionsSegmentAddress = c2me$packedBlockPositionsSegment.address();
    private final int[] c2me$blockPos = new int[((1 << c2me$shiftY) * sizeY) << 2];
    private final long[] c2me$blockPosPacked = new long[(1 << c2me$shiftY) * sizeY];
    {
        MemorySegment.copy(c2me$packedBlockPositions, 0, c2me$packedBlockPositionsSegment, ValueLayout.JAVA_SHORT, 0, c2me$packedBlockPositions.length);
    }

//    private final int[] c2me$packedArray = new int[3];
    private final MemorySegment c2me$packedArraySegment = ARENA.allocate(3 * 4);
    private final long c2me$packedArraySegmentAddress = c2me$packedArraySegment.address();

    {
        Random random = new Random(0xcafe);
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
                for (int x = 0; x < this.sizeX; x++) {
                    final int x1 = x + this.startX;
                    final int y1 = y + this.startY;
                    final int z1 = z + this.startZ;
                    int r0 = random.nextInt(10);
                    int r1 = random.nextInt(9);
                    int r2 = random.nextInt(10);
                    int x2 = x1 * 16 + r0;
                    int y2 = y1 * 12 + r1;
                    int z2 = z1 * 16 + r2;
                    int index = this.index(x1, y1, z1);
                    int fastIdx = this.c2me$fastIdx(x1, y1, z1);
                    this.blockPositions[index] = BlockPos.asLong(x2, y2, z2);
                    this.c2me$blockPosPacked[fastIdx] = BlockPos.asLong(x2, y2, z2);
                    int shiftedIdx = fastIdx << 2;
                    this.c2me$blockPos[shiftedIdx + 0] = x2;
                    this.c2me$blockPos[shiftedIdx + 1] = y2;
                    this.c2me$blockPos[shiftedIdx + 2] = z2;
                    this.c2me$packedBlockPositions[index] = (short) ((r0 << 8) | (r1 << 4) | r2);
                }
            }
        }
    }

    private static int c2me$unpackPackedX(int packed) {
        return packed >> 8;
    }

    private static int c2me$unpackPackedY(int packed) {
        return (packed >> 4) & 0b1111;
    }

    private static int c2me$unpackPackedZ(int packed) {
        return packed & 0b1111;
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

    private void vanillaOpt4(int x, int y, int z, Blackhole bh) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int A = Integer.MAX_VALUE;
        int B = Integer.MAX_VALUE;
        int C = Integer.MAX_VALUE;

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


                {
                    int p = (dist_0 << 9) | posIdx0;//Dont know what shift is min
                    int n1 = Math.max(A, p);
                    A = Math.min(A, p);

                    int n2 = Math.max(B, n1);
                    B = Math.min(B, n1);

                    C = Math.min(C, n2);
                }

                {
                    int p1 = (dist_1<<9)|posIdx1;//Dont know what shift is min

                    int n1 = Math.max(A, p1);
                    A = Math.min(A, p1);

                    int n2 = Math.max(B, n1);
                    B = Math.min(B, n1);

                    C = Math.min(C, n2);
                }
            }
        }

        bh.consume(A>>>9);
        bh.consume(B>>>9);
        bh.consume(C>>>9);
        bh.consume(0);
        bh.consume(0);
        bh.consume(0);
        bh.consume(this.blockPositions[A&0x1F]);
        bh.consume(this.blockPositions[B&0x1F]);
        bh.consume(this.blockPositions[C&0x1F]);
    }

    private void vanillaOpt5(int x, int y, int z, Blackhole bh) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int dist1 = Integer.MAX_VALUE;
        int dist2 = Integer.MAX_VALUE;
        int dist3 = Integer.MAX_VALUE;
        int idx1 = 0;
        int idx2 = 0;
        int idx3 = 0;
        long pos1 = 0;
        long pos2 = 0;
        long pos3 = 0;

        for (int offY = -1; offY <= 1; ++offY) {
            for (int offZ = 0; offZ <= 1; ++offZ) {
                int posIdx0 = this.index(gx, gy + offY, gz + offZ);

                int position0 = this.c2me$packedBlockPositions[posIdx0];

                int gymul = (gy + offY) * 12;
                int gzmul = (gz + offZ) * 16;

                int dx0 = gx * 16 + c2me$unpackPackedX(position0) - x;
                int dy0 = gymul + c2me$unpackPackedY(position0) - y;
                int dz0 = gzmul + c2me$unpackPackedZ(position0) - z;
                int dist_0 = dx0 * dx0 + dy0 * dy0 + dz0 * dz0;

                int posIdx1 = posIdx0 + 1;
                int position1 = this.c2me$packedBlockPositions[posIdx1];
                int dx1 = (gx + 1) * 16 + c2me$unpackPackedX(position1) - x;
                int dy1 = gymul + c2me$unpackPackedY(position1) - y;
                int dz1 = gzmul + c2me$unpackPackedZ(position1) - z;
                int dist_1 = dx1 * dx1 + dy1 * dy1 + dz1 * dz1;
                if (dist3 >= dist_0) {
                    idx3 = posIdx0;
                    dist3 = dist_0;
                }
                if (dist2 >= dist_0) {
                    idx3 = idx2;
                    dist3 = dist2;
                    idx2 = posIdx0;
                    dist2 = dist_0;
                }
                if (dist1 >= dist_0) {
                    idx2 = idx1;
                    dist2 = dist1;
                    idx1 = posIdx0;
                    dist1 = dist_0;
                }
                if (dist3 >= dist_1) {
                    idx3 = posIdx1;
                    dist3 = dist_1;
                }
                if (dist2 >= dist_1) {
                    idx3 = idx2;
                    dist3 = dist2;
                    idx2 = posIdx1;
                    dist2 = dist_1;
                }
                if (dist1 >= dist_1) {
                    idx2 = idx1;
                    dist2 = dist1;
                    idx1 = posIdx1;
                    dist1 = dist_1;
                }
            }
        }

        pos1 = this.blockPositions[idx1];
        pos2 = this.blockPositions[idx2];
        pos3 = this.blockPositions[idx3];

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

    private void vanillaOpt6(int x, int y, int z, Blackhole bh) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int A = Integer.MAX_VALUE;
        int B = Integer.MAX_VALUE;
        int C = Integer.MAX_VALUE;

        int index = 12; // 12 max
        for (int offY = -1; offY <= 1; ++offY) {
            int gymul = (gy + offY) * 12;
            for (int offZ = 0; offZ <= 1; ++offZ) {
                int gzmul = (gz + offZ) << 4;

                int index0 = index - 1;
                int idxX = gx - this.startX;
                int idxY = gy + offY - this.startY;
                int idxZ = gz + offZ - this.startZ;
                int posIdx0 = (idxY * this.sizeZ + idxZ) * this.sizeX + idxX;
                int position0 = this.c2me$packedBlockPositions[posIdx0];
                int dx0 = (gx << 4) + (position0 >> 8) - x;
                int dy0 = gymul + ((position0 >> 4) & 0b1111) - y;
                int dz0 = gzmul + (position0 & 0b1111) - z;
                int dist_0 = dx0 * dx0 + dy0 * dy0 + dz0 * dz0;

                int index1 = index - 2;
                int posIdx1 = posIdx0 + 1;
                int position1 = this.c2me$packedBlockPositions[posIdx1];
                int dx1 = ((gx + 1) << 4) + (position1 >> 8) - x;
                int dy1 = gymul + ((position1 >> 4) & 0b1111) - y;
                int dz1 = gzmul + (position1 & 0b1111) - z;
                int dist_1 = dx1 * dx1 + dy1 * dy1 + dz1 * dz1;

                int p0 = (dist_0 << 20) | (index0 << 16) | posIdx0;
                if (p0 <= C) {
                    int n01 = Math.max(A, p0);
                    A = Math.min(A, p0);

                    int n02 = Math.max(B, n01);
                    B = Math.min(B, n01);

                    C = Math.min(C, n02);
                }

                int p1 = (dist_1 << 20) | (index1 << 16) | posIdx1;
                if (p1 <= C) {
                    int n11 = Math.max(A, p1);
                    A = Math.min(A, p1);

                    int n12 = Math.max(B, n11);
                    B = Math.min(B, n11);

                    C = Math.min(C, n12);
                }

                index -= 2;
            }
        }

        int dist1 = A >> 20;
        int dist2 = B >> 20;
        int dist3 = C >> 20;
        long pos1 = this.blockPositions[A&0xffff];
        long pos2 = this.blockPositions[B&0xffff];
        long pos3 = this.blockPositions[C&0xffff];

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

    @Benchmark
    public void benchVanillaOpt4(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    vanillaOpt4(x, y, z, bh);
                }
            }
        }
    }

    @Benchmark
    public void benchVanillaOpt5(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    vanillaOpt5(x, y, z, bh);
                }
            }
        }
    }

    @Benchmark
    public void benchVanillaOpt6(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    vanillaOpt6(x, y, z, bh);
                }
            }
        }
    }

    @Override
    protected void doInvocation(MethodHandle handle, Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    invokeNative0(handle, bh, x, y, z);
                }
            }
        }
    }

    private void invokeNative0(MethodHandle handle, Blackhole bh, int x, int y, int z) {
        try {
            handle.invokeExact(c2me$packedBlockPositionsSegmentAddress, c2me$packedArraySegmentAddress, c2me$aquiferDataAddress, x, y, z);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        int A = c2me$packedArraySegment.get(ValueLayout.JAVA_INT, 0);
        int B = c2me$packedArraySegment.get(ValueLayout.JAVA_INT, 4);
        int C = c2me$packedArraySegment.get(ValueLayout.JAVA_INT, 8);

        int dist1 = A >> 20;
        int dist2 = B >> 20;
        int dist3 = C >> 20;
        long pos1 = this.blockPositions[A&0xffff];
        long pos2 = this.blockPositions[B&0xffff];
        long pos3 = this.blockPositions[C&0xffff];

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
    @Override
    public void spinning(Blackhole bh) {
        for (int x = 0; x < 16; x ++) {
            for (int z = 304; z < 320; z ++) {
                for (int y = -64; y < 320; y ++) {
                    bh.consume(x);
                    bh.consume(y);
                    bh.consume(z);
                    bh.consume(x);
                    bh.consume(y);
                    bh.consume(z);
                    bh.consume(0L);
                    bh.consume(0L);
                    bh.consume(0L);
                }
            }
        }
    }

    @Override
    public void vanilla(Blackhole bh) {
        // unused
    }
}
