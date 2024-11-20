package com.ishland.c2me.opts.dfc.common.ast.spline;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.ast.McToAst;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import com.ishland.c2me.opts.dfc.common.vif.NoisePosVanillaInterface;
import com.ishland.flowsched.util.Assertions;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Spline;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplineAstNode implements AstNode {

    public static final String SPLINE_METHOD_DESC = Type.getMethodDescriptor(Type.getType(float.class), Type.getType(int.class), Type.getType(int.class), Type.getType(int.class), Type.getType(EvalType.class));
    private final Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> spline;

    public SplineAstNode(Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> spline) {
        this.spline = spline;
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        return spline.apply(new DensityFunctionTypes.Spline.SplinePos(new NoisePosVanillaInterface(x, y, z, type)));
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        for (int i = 0; i < res.length; i++) {
            res[i] = this.evalSingle(x[i], y[i], z[i], type);
        }
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[0];
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        return transformer.transform(this);
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        ValuesMethodDef splineMethod = doBytecodeGenSpline(context, this.spline);
        callSplineSingle(context, m, splineMethod);
        m.cast(Type.FLOAT_TYPE, Type.DOUBLE_TYPE);
        m.areturn(Type.DOUBLE_TYPE);
    }

    private static ValuesMethodDef doBytecodeGenSpline(BytecodeGen.Context context, Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> spline) {
        {
            String cachedSplineMethod = context.getCachedSplineMethod(spline);
            if (cachedSplineMethod != null) {
                return new ValuesMethodDef(false, cachedSplineMethod, 0.0F);
            }
        }
        if (spline instanceof Spline.FixedFloatFunction<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> spline1) {
            return new ValuesMethodDef(true, null, spline1.value());
        }
        String name = context.nextMethodName("Spline");
        InstructionAdapter m = new InstructionAdapter(
                new AnalyzerAdapter(
                        context.className,
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                        name,
                        SPLINE_METHOD_DESC,
                        context.classWriter.visitMethod(
                                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                                name,
                                SPLINE_METHOD_DESC,
                                null,
                                null
                        )
                )
        );
        List<IntObjectPair<Pair<String, String>>> extraLocals = new ArrayList<>();
        Label start = new Label();
        Label end = new Label();
        m.visitLabel(start);
        BytecodeGen.Context.LocalVarConsumer localVarConsumer = (localName, localDesc) -> {
            int ordinal = extraLocals.size() + 5;
            extraLocals.add(IntObjectPair.of(ordinal, Pair.of(localName, localDesc)));
            return ordinal;
        };

        if (spline instanceof Spline.Implementation<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> impl) {
            ValuesMethodDef[] valuesMethods = impl.values().stream()
                    .map(spline1 -> doBytecodeGenSpline(context, spline1))
                    .toArray(ValuesMethodDef[]::new);

            String locations = context.newField(float[].class, impl.locations());
            String derivatives = context.newField(float[].class, impl.derivatives());

            int point = localVarConsumer.createLocalVariable("point", Type.FLOAT_TYPE.getDescriptor());
            int rangeForLocation = localVarConsumer.createLocalVariable("rangeForLocation", Type.INT_TYPE.getDescriptor());

            int lastConst = impl.locations().length - 1;

            String locationFunction = context.newSingleMethod(McToAst.toAst(impl.locationFunction().function().value()));
            context.callDelegateSingle(m, locationFunction);
            m.cast(Type.DOUBLE_TYPE, Type.FLOAT_TYPE);
            m.store(point, Type.FLOAT_TYPE);

            if (valuesMethods.length == 1) {
                m.load(point, Type.FLOAT_TYPE);
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(
                        context.className,
                        locations,
                        Type.getDescriptor(float[].class)
                );
                callSplineSingle(context, m, valuesMethods[0]);
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(
                        context.className,
                        derivatives,
                        Type.getDescriptor(float[].class)
                );
                m.iconst(0);
                m.invokestatic(
                        Type.getInternalName(SplineSupport.class),
                        "sampleOutsideRange",
                        Type.getMethodDescriptor(Type.FLOAT_TYPE, Type.FLOAT_TYPE, Type.getType(float[].class), Type.FLOAT_TYPE, Type.getType(float[].class), Type.INT_TYPE),
                        false
                );
                m.areturn(Type.FLOAT_TYPE);
            } else {
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(
                        context.className,
                        locations,
                        Type.getDescriptor(float[].class)
                );
                m.load(point, Type.FLOAT_TYPE);
                m.invokestatic(
                        Type.getInternalName(SplineSupport.class),
                        "findRangeForLocation",
                        Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(float[].class), Type.FLOAT_TYPE),
                        false
                );
                m.store(rangeForLocation, Type.INT_TYPE);

                Label label1 = new Label();
                Label label2 = new Label();

                m.load(rangeForLocation, Type.INT_TYPE);
                m.ifge(label1);
                // rangeForLocation < 0
                m.load(point, Type.FLOAT_TYPE);
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(
                        context.className,
                        locations,
                        Type.getDescriptor(float[].class)
                );
                callSplineSingle(context, m, valuesMethods[0]);
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(
                        context.className,
                        derivatives,
                        Type.getDescriptor(float[].class)
                );
                m.iconst(0);
                m.invokestatic(
                        Type.getInternalName(SplineSupport.class),
                        "sampleOutsideRange",
                        Type.getMethodDescriptor(Type.FLOAT_TYPE, Type.FLOAT_TYPE, Type.getType(float[].class), Type.FLOAT_TYPE, Type.getType(float[].class), Type.INT_TYPE),
                        false
                );
                m.areturn(Type.FLOAT_TYPE);

                m.visitLabel(label1);
                m.load(rangeForLocation, Type.INT_TYPE);
                m.iconst(lastConst);
                m.ificmpne(label2);
                // rangeForLocation == last
                m.load(point, Type.FLOAT_TYPE);
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(
                        context.className,
                        locations,
                        Type.getDescriptor(float[].class)
                );
                callSplineSingle(context, m, valuesMethods[lastConst]);
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(
                        context.className,
                        derivatives,
                        Type.getDescriptor(float[].class)
                );
                m.iconst(lastConst);
                m.invokestatic(
                        Type.getInternalName(SplineSupport.class),
                        "sampleOutsideRange",
                        Type.getMethodDescriptor(Type.FLOAT_TYPE, Type.FLOAT_TYPE, Type.getType(float[].class), Type.FLOAT_TYPE, Type.getType(float[].class), Type.INT_TYPE),
                        false
                );
                m.areturn(Type.FLOAT_TYPE);

                m.visitLabel(label2);

                int loc0 = localVarConsumer.createLocalVariable("loc0", Type.FLOAT_TYPE.getDescriptor());
                int loc1 = localVarConsumer.createLocalVariable("loc1", Type.FLOAT_TYPE.getDescriptor());
                int locDist = localVarConsumer.createLocalVariable("locDist", Type.FLOAT_TYPE.getDescriptor());
                int k = localVarConsumer.createLocalVariable("k", Type.FLOAT_TYPE.getDescriptor());
                int n = localVarConsumer.createLocalVariable("n", Type.FLOAT_TYPE.getDescriptor());
                int o = localVarConsumer.createLocalVariable("o", Type.FLOAT_TYPE.getDescriptor());
                int onDist = localVarConsumer.createLocalVariable("onDist", Type.FLOAT_TYPE.getDescriptor());
                int p = localVarConsumer.createLocalVariable("p", Type.FLOAT_TYPE.getDescriptor());
                int q = localVarConsumer.createLocalVariable("q", Type.FLOAT_TYPE.getDescriptor());

                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(
                        context.className,
                        locations,
                        Type.getDescriptor(float[].class)
                );
                m.load(rangeForLocation, Type.INT_TYPE);
                m.aload(Type.FLOAT_TYPE);
                m.store(loc0, Type.FLOAT_TYPE);

                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(
                        context.className,
                        locations,
                        Type.getDescriptor(float[].class)
                );
                m.load(rangeForLocation, Type.INT_TYPE);
                m.iconst(1);
                m.add(Type.INT_TYPE);
                m.aload(Type.FLOAT_TYPE);
                m.store(loc1, Type.FLOAT_TYPE);

                m.load(loc1, Type.FLOAT_TYPE);
                m.load(loc0, Type.FLOAT_TYPE);
                m.sub(Type.FLOAT_TYPE);
                m.store(locDist, Type.FLOAT_TYPE);

                m.load(point, Type.FLOAT_TYPE);
                m.load(loc0, Type.FLOAT_TYPE);
                m.sub(Type.FLOAT_TYPE);
                m.load(locDist, Type.FLOAT_TYPE);
                m.div(Type.FLOAT_TYPE);
                m.store(k, Type.FLOAT_TYPE);

                Label[] jumpLabels = new Label[valuesMethods.length - 1];
                boolean[] jumpGenerated = new boolean[valuesMethods.length - 1];
                for (int i = 0; i < valuesMethods.length - 1; i++) {
                    jumpLabels[i] = new Label();
                }
                Label defaultLabel = new Label();
                Label label3 = new Label();

                m.load(rangeForLocation, Type.INT_TYPE);
                m.tableswitch(
                        0,
                        valuesMethods.length - 2,
                        defaultLabel,
                        jumpLabels
                );

                for (int i = 0; i < valuesMethods.length - 1; i++) {
                    if (jumpGenerated[i]) continue;
                    m.visitLabel(jumpLabels[i]);
                    jumpGenerated[i] = true;
                    for (int j = i + 1; j < valuesMethods.length - 1; j++) { // deduplication
                        if (valuesMethods[i].equals(valuesMethods[j]) && valuesMethods[i + 1].equals(valuesMethods[j + 1])) {
                            m.visitLabel(jumpLabels[j]);
                            jumpGenerated[j] = true;
                        }
                    }
                    callSplineSingle(context, m, valuesMethods[i]);
                    if (valuesMethods[i].equals(valuesMethods[i + 1])) { // splines are pure
                        m.dup();
                        m.store(n, Type.FLOAT_TYPE);
                        m.store(o, Type.FLOAT_TYPE);
                    } else {
                        m.store(n, Type.FLOAT_TYPE);
                        callSplineSingle(context, m, valuesMethods[i + 1]);
                        m.store(o, Type.FLOAT_TYPE);
                    }
                    m.goTo(label3);
                }

                m.visitLabel(defaultLabel);
                m.iconst(0);
                m.aconst("boom");
                m.invokestatic(
                        Type.getInternalName(Assertions.class),
                        "assertTrue",
                        Type.getMethodDescriptor(Type.VOID_TYPE, Type.BOOLEAN_TYPE, Type.getType(String.class)),
                        false
                );
                m.fconst(Float.NaN); // unreachable code
                m.areturn(Type.FLOAT_TYPE);

                m.visitLabel(label3);

                m.load(o, Type.FLOAT_TYPE);
                m.load(n, Type.FLOAT_TYPE);
                m.sub(Type.FLOAT_TYPE);
                m.store(onDist, Type.FLOAT_TYPE);

                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(
                        context.className,
                        derivatives,
                        Type.getDescriptor(float[].class)
                );
                m.load(rangeForLocation, Type.INT_TYPE);
                m.aload(Type.FLOAT_TYPE);
                m.load(locDist, Type.FLOAT_TYPE);
                m.mul(Type.FLOAT_TYPE);
                m.load(onDist, Type.FLOAT_TYPE);
                m.sub(Type.FLOAT_TYPE);
                m.store(p, Type.FLOAT_TYPE);

                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(
                        context.className,
                        derivatives,
                        Type.getDescriptor(float[].class)
                );
                m.load(rangeForLocation, Type.INT_TYPE);
                m.iconst(1);
                m.add(Type.INT_TYPE);
                m.aload(Type.FLOAT_TYPE);
                m.neg(Type.FLOAT_TYPE);
                m.load(locDist, Type.FLOAT_TYPE);
                m.mul(Type.FLOAT_TYPE);
                m.load(onDist, Type.FLOAT_TYPE);
                m.add(Type.FLOAT_TYPE);
                m.store(q, Type.FLOAT_TYPE);

                m.load(k, Type.FLOAT_TYPE);
                m.load(n, Type.FLOAT_TYPE);
                m.load(o, Type.FLOAT_TYPE);
                m.invokestatic(
                        Type.getInternalName(MathHelper.class),
                        FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_3532", "method_16439", "(FFF)F"),
                        "(FFF)F",
                        false
                );
                m.load(k, Type.FLOAT_TYPE);
                m.fconst(1.0F);
                m.load(k, Type.FLOAT_TYPE);
                m.sub(Type.FLOAT_TYPE);
                m.mul(Type.FLOAT_TYPE);
                m.load(k, Type.FLOAT_TYPE);
                m.load(p, Type.FLOAT_TYPE);
                m.load(q, Type.FLOAT_TYPE);
                m.invokestatic(
                        Type.getInternalName(MathHelper.class),
                        FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_3532", "method_16439", "(FFF)F"),
                        "(FFF)F",
                        false
                );
                m.mul(Type.FLOAT_TYPE);
                m.add(Type.FLOAT_TYPE);
                m.areturn(Type.FLOAT_TYPE);
            }

        } else if (spline instanceof Spline.FixedFloatFunction<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> floatFunction) {
            m.fconst(floatFunction.value());
            m.areturn(Type.FLOAT_TYPE);
        } else {
            throw new UnsupportedOperationException(String.format("Unsupported spline implementation: %s", spline.getClass().getName()));
        }

        m.visitLabel(end);
        m.visitLocalVariable("this", context.classDesc, null, start, end, 0);
        m.visitLocalVariable("x", Type.INT_TYPE.getDescriptor(), null, start, end, 1);
        m.visitLocalVariable("y", Type.INT_TYPE.getDescriptor(), null, start, end, 2);
        m.visitLocalVariable("z", Type.INT_TYPE.getDescriptor(), null, start, end, 3);
        m.visitLocalVariable("evalType", Type.getType(EvalType.class).getDescriptor(), null, start, end, 4);
        for (IntObjectPair<Pair<String, String>> local : extraLocals) {
            m.visitLocalVariable(local.right().left(), local.right().right(), null, start, end, local.leftInt());
        }
        m.visitMaxs(0, 0);

        context.cacheSplineMethod(spline, name);

        return new ValuesMethodDef(false, name, 0.0F);
    }

    private static void callSplineSingle(BytecodeGen.Context context, InstructionAdapter m, ValuesMethodDef target) {
        if (target.isConst()) {
            m.fconst(target.constValue());
        } else {
            m.load(0, InstructionAdapter.OBJECT_TYPE);
            m.load(1, Type.INT_TYPE);
            m.load(2, Type.INT_TYPE);
            m.load(3, Type.INT_TYPE);
            m.load(4, InstructionAdapter.OBJECT_TYPE);
            m.invokevirtual(context.className, target.generatedMethod(), SPLINE_METHOD_DESC, false);
        }
    }

    private record ValuesMethodDef(boolean isConst, String generatedMethod, float constValue) {
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        context.delegateToSingle(m, localVarConsumer, this);
        m.areturn(Type.VOID_TYPE);
    }

    private static boolean deepEquals(Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a,
                                      Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> b) {
        if (a instanceof Spline.FixedFloatFunction<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a1 &&
                b instanceof Spline.FixedFloatFunction<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> b1) {
            return a1.value() == b1.value();
        } else if (a instanceof Spline.Implementation<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a1 &&
                b instanceof Spline.Implementation<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> b1) {
            boolean equals1 = Arrays.equals(a1.derivatives(), b1.derivatives()) &&
                    Arrays.equals(a1.locations(), b1.locations()) &&
                    a1.values().size() == b1.values().size() &&
                    McToAst.toAst(a1.locationFunction().function().value()).equals(McToAst.toAst(b1.locationFunction().function().value()));
            if (!equals1) return false;
            int size = a1.values().size();
            for (int i = 0; i < size; i++) {
                if (!deepEquals(a1.values().get(i), b1.values().get(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private static boolean deepRelaxedEquals(Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a,
                                      Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> b) {
        if (a instanceof Spline.FixedFloatFunction<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a1 &&
                b instanceof Spline.FixedFloatFunction<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> b1) {
            return a1.value() == b1.value();
        } else if (a instanceof Spline.Implementation<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a1 &&
                b instanceof Spline.Implementation<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> b1) {
            boolean equals1 = a1.values().size() == b1.values().size() &&
                    McToAst.toAst(a1.locationFunction().function().value()).relaxedEquals(McToAst.toAst(b1.locationFunction().function().value()));
            if (!equals1) return false;
            int size = a1.values().size();
            for (int i = 0; i < size; i++) {
                if (!deepRelaxedEquals(a1.values().get(i), b1.values().get(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private static int deepHashcode(Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a) {
        if (a instanceof Spline.FixedFloatFunction<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a1) {
            return Float.hashCode(a1.value());
        } else if (a instanceof Spline.Implementation<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a1) {
            int result = 1;

            result = 31 * result + Arrays.hashCode(a1.derivatives());
            result = 31 * result + Arrays.hashCode(a1.locations());
            for (Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> spline : a1.values()) {
                result = 31 * result + deepHashcode(spline);
            }
            result = 31 * result + McToAst.toAst(a1.locationFunction().function().value()).hashCode();

            return result;
        } else {
            return a.hashCode();
        }
    }

    private static int deepRelaxedHashcode(Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a) {
        if (a instanceof Spline.FixedFloatFunction<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a1) {
            return Float.hashCode(a1.value());
        } else if (a instanceof Spline.Implementation<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> a1) {
            int result = 1;

            for (Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> spline : a1.values()) {
                result = 31 * result + deepRelaxedHashcode(spline);
            }
            result = 31 * result + McToAst.toAst(a1.locationFunction().function().value()).relaxedHashCode();

            return result;
        } else {
            return a.hashCode();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SplineAstNode that = (SplineAstNode) o;
        return deepEquals(this.spline, that.spline);
    }

    @Override
    public int hashCode() {
        return deepHashcode(this.spline);
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SplineAstNode that = (SplineAstNode) o;
        return deepRelaxedEquals(this.spline, that.spline);
    }

    @Override
    public int relaxedHashCode() {
        return deepRelaxedHashcode(this.spline);
    }
}
