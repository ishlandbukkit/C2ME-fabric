package com.ishland.c2me.opts.dfc.common.ast;

import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.DelegateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.YClampedGradientNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftANode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftBNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTWeirdScaledSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.ShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineAstNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbsNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CubeNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.NegMulNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SquareNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqueezeNode;
import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import com.ishland.c2me.opts.dfc.common.ducks.IEqualityOverriding;
import com.ishland.c2me.opts.dfc.common.vif.AstVanillaInterface;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

import java.util.Objects;

public class McToAst {

//    private static final ConcurrentHashMap<Class<?>, LongAdder> delegateStatistics = new ConcurrentHashMap<>();

    public static AstNode toAst(DensityFunction df) {
        Objects.requireNonNull(df);
        return switch (df) {
            case AstVanillaInterface f -> f.getAstNode();

            case ChunkNoiseSampler.BlendAlphaDensityFunction f -> new ConstantNode(1.0);
            case ChunkNoiseSampler.BlendOffsetDensityFunction f -> new ConstantNode(0.0);
            case DensityFunctionTypes.BlendAlpha f -> new ConstantNode(1.0);
            case DensityFunctionTypes.BlendOffset f -> new ConstantNode(0.0);
            case DensityFunctionTypes.BinaryOperationLike f -> switch (f.type()) {
                case ADD -> new AddNode(toAst(f.argument1()), toAst(f.argument2()));
                case MUL -> new MulNode(toAst(f.argument1()), toAst(f.argument2()));
                case MIN -> {
                    double rightMin = f.argument2().minValue();
                    if (f.argument1().minValue() < rightMin) {
                        yield new MinShortNode(toAst(f.argument1()), toAst(f.argument2()), rightMin);
                    } else {
                        yield new MinNode(toAst(f.argument1()), toAst(f.argument2()));
                    }
                }
                case MAX -> {
                    double rightMax = f.argument2().maxValue();
                    if (f.argument1().maxValue() > rightMax) {
                        yield new MaxShortNode(toAst(f.argument1()), toAst(f.argument2()), rightMax);
                    } else {
                        yield new MaxNode(toAst(f.argument1()), toAst(f.argument2()));
                    }
                }
            };
            case DensityFunctionTypes.BlendDensity f -> toAst(f.input());
            case DensityFunctionTypes.Clamp f -> new MaxNode(new ConstantNode(f.minValue()), new MinNode(new ConstantNode(f.maxValue()), toAst(f.input())));
            case DensityFunctionTypes.Constant f -> new ConstantNode(f.value());
            case DensityFunctionTypes.RegistryEntryHolder f -> toAst(f.function().value());
            case DensityFunctionTypes.UnaryOperation f -> switch (f.type()) {
                case ABS -> new AbsNode(toAst(f.input()));
                case SQUARE -> new SquareNode(toAst(f.input()));
                case CUBE -> new CubeNode(toAst(f.input()));
                case HALF_NEGATIVE -> new NegMulNode(toAst(f.input()), 0.5);
                case QUARTER_NEGATIVE -> new NegMulNode(toAst(f.input()), 0.25);
                case SQUEEZE -> new SqueezeNode(toAst(f.input()));
            };
            case DensityFunctionTypes.RangeChoice f -> new RangeChoiceNode(toAst(f.input()), f.minInclusive(), f.maxExclusive(), toAst(f.whenInRange()), toAst(f.whenOutOfRange()));
            case IFastCacheLike f -> new CacheLikeNode(f, toAst(f.c2me$getDelegate()));
            case DensityFunctionTypes.Wrapping f -> {
                DensityFunctionTypes.Wrapping wrapping = new DensityFunctionTypes.Wrapping(f.type(), new AstVanillaInterface(toAst(f.wrapped()), null));
                ((IEqualityOverriding) (Object) wrapping).c2me$overrideEquality(wrapping);
                yield new DelegateNode(wrapping);
            }
            case DensityFunctionTypes.ShiftedNoise f -> new ShiftedNoiseNode(toAst(f.shiftX()), toAst(f.shiftY()), toAst(f.shiftZ()), f.xzScale(), f.yScale(), f.noise());
            case DensityFunctionTypes.Noise f -> new DFTNoiseNode(f.noise(), f.xzScale(), f.yScale());
            case DensityFunctionTypes.Shift f -> new DFTShiftNode(f.offsetNoise());
            case DensityFunctionTypes.ShiftA f -> new DFTShiftANode(f.offsetNoise());
            case DensityFunctionTypes.ShiftB f -> new DFTShiftBNode(f.offsetNoise());
            case DensityFunctionTypes.YClampedGradient f -> new YClampedGradientNode(f.fromY(), f.toY(), f.fromValue(), f.toValue());
            case DensityFunctionTypes.WeirdScaledSampler f -> new DFTWeirdScaledSamplerNode(toAst(f.input()), f.noise(), f.rarityValueMapper());
            case DensityFunctionTypes.Spline f -> new SplineAstNode(f.spline());

            default -> {
//                delegateStatistics.computeIfAbsent(df.getClass(), unused -> new LongAdder()).increment();;
                yield new DelegateNode(df);
            }
        };
    }

    public static DensityFunction wrapVanilla(DensityFunction densityFunction) {
        return new AstVanillaInterface(McToAst.toAst(densityFunction), densityFunction);
    }

}
