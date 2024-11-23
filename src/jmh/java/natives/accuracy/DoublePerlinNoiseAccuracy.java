package natives.accuracy;

import com.ishland.c2me.opts.natives_math.common.BindingsTemplate;
import com.ishland.c2me.opts.natives_math.common.ISATarget;
import natives.DoublePerlinNoiseBenchmark;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Random;

public class DoublePerlinNoiseAccuracy extends AbstractAccuracy {

    private final Random random = new Random();
    private final DoublePerlinNoiseSampler vanillaSampler;
    private final MemorySegment nativeSampler;
    private final long nativeSamplerPtr;

    protected DoublePerlinNoiseAccuracy() {
        super(Arrays.stream(ISATarget.getInstance().getEnumConstants()).toArray(ISATarget[]::new), BindingsTemplate.c2me_natives_noise_perlin_double_ptr, "c2me_natives_noise_perlin_double");
        final net.minecraft.util.math.random.Random minecraftRandom = net.minecraft.util.math.random.Random.create(0xcafe);
        double[] octaves = new double[16];
        for (int i = 0, octavesLength = octaves.length; i < octavesLength; i++) {
            octaves[i] = minecraftRandom.nextDouble() * 32.0D + 0.01D;
        }
        vanillaSampler = DoublePerlinNoiseSampler.create(minecraftRandom, minecraftRandom.nextInt(128), octaves);
        nativeSampler = DoublePerlinNoiseBenchmark.create(vanillaSampler);
        nativeSamplerPtr = nativeSampler.address();
    }

    private double invokeNative(MethodHandle handle, double x, double y, double z) {
        try {
            return (double) handle.invokeExact(nativeSamplerPtr, x, y, z);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private double invokeVanilla(double x, double y, double z) {
        return vanillaSampler.sample(x, y, z);
    }

    private void loopBody() {
        double x = this.random.nextDouble(-30000000.0D, 30000000.0D);
        double y = this.random.nextDouble(-2048.0D, 2048.0D);
        double z = this.random.nextDouble(-30000000.0D, 30000000.0D);

        double original = invokeVanilla(x, y, z);
        for (int i = 0; i < this.MHs.length; i ++) {
            double actual = invokeNative(this.MHs[i], x, y, z);
            long ulpDiff = ulpDistance(original, actual);
            if (ulpDiff > this.maxUlp[i]) {
                this.maxUlp[i] = ulpDiff;
                System.out.println(String.format("%s: new max error %d ulps at x=%.18g, y=%.18g, z=%.18g (expected %.18g but got %.18g)", this.targets[i], ulpDiff, x, y, z, original, actual));
            }
        }
    }

    public static void main(String[] args) {
        final long printInterval = 10_000_000_000L;
        DoublePerlinNoiseAccuracy instance = new DoublePerlinNoiseAccuracy();
        long lastPrint = System.nanoTime();
        for (long iter = 0; ; iter ++) {
            instance.loopBody();
            if ((iter & (1L << 16L - 1L)) == 0) {
                long nanoTime = System.nanoTime();
                if (nanoTime > (lastPrint + printInterval)) {
                    lastPrint += printInterval;
                    System.out.println("=".repeat(30));
                    System.out.println(String.format("Iterations: %d", iter));
                    instance.printUlps();
                    System.out.println("=".repeat(30));
                }
            }
        }
    }

}
