package natives.accuracy;

import com.ishland.c2me.opts.natives_math.common.BindingsTemplate;
import com.ishland.c2me.opts.natives_math.common.ISATarget;
import natives.support.InterpolatedNoiseSamplerCopy;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Random;

public class InterpolatedNoiseAccuracy extends AbstractAccuracy {

    private final Random random = new Random();
    private final InterpolatedNoiseSamplerCopy vanillaSampler;
    private final MemorySegment nativeSampler;
    private final long nativeSamplerPtr;

    protected InterpolatedNoiseAccuracy() {
        super(Arrays.stream(ISATarget.getInstance().getEnumConstants()).toArray(ISATarget[]::new), BindingsTemplate.c2me_natives_noise_interpolated_ptr, "c2me_natives_noise_interpolated");
        vanillaSampler = InterpolatedNoiseSamplerCopy.createBase3dNoiseFunction(0.25, 0.125, 80.0, 160.0, 8.0);
        nativeSampler = InterpolatedNoiseSamplerCopy.interpolated_noise_sampler$create(vanillaSampler);
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
        InterpolatedNoiseAccuracy instance = new InterpolatedNoiseAccuracy();
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
