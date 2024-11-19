package com.ishland.c2me.opts.dfc.common.ast.noise;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import com.ishland.c2me.opts.dfc.common.util.ArrayCache;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.gen.densityfunction.DensityFunction.Noise;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Objects;

public class ShiftedNoiseNode implements AstNode {

    private final AstNode shiftX;
    private final AstNode shiftY;
    private final AstNode shiftZ;
    private final double xzScale;
    private final double yScale;
    private final Noise noise;

    public ShiftedNoiseNode(AstNode shiftX, AstNode shiftY, AstNode shiftZ, double xzScale, double yScale, Noise noise) {
        this.shiftX = Objects.requireNonNull(shiftX);
        this.shiftY = Objects.requireNonNull(shiftY);
        this.shiftZ = Objects.requireNonNull(shiftZ);
        this.xzScale = xzScale;
        this.yScale = yScale;
        this.noise = Objects.requireNonNull(noise);
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        double d = x * this.xzScale + this.shiftX.evalSingle(x, y, z, type);
        double e = y * this.yScale + this.shiftY.evalSingle(x, y, z, type);
        double f = z * this.xzScale + this.shiftZ.evalSingle(x, y, z, type);
        return this.noise.sample(d, e, f);
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        double[] res1 = new double[res.length];
        double[] res2 = new double[res.length];
        this.shiftX.evalMulti(res, x, y, z, type);
        this.shiftY.evalMulti(res1, x, y, z, type);
        this.shiftZ.evalMulti(res2, x, y, z, type);

        for (int i = 0; i < res.length; i++) {
            res[i] = this.noise.sample(
                    x[i] * this.xzScale + res[i],
                    y[i] * this.yScale + res1[i],
                    z[i] * xzScale + res2[i]
            );
        }
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[]{this.shiftX, this.shiftY, this.shiftZ};
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode shiftX = this.shiftX.transform(transformer);
        AstNode shiftY = this.shiftY.transform(transformer);
        AstNode shiftZ = this.shiftZ.transform(transformer);
        if (shiftX == this.shiftX && shiftY == this.shiftY && shiftZ == this.shiftZ) {
            return transformer.transform(this);
        } else {
            return transformer.transform(new ShiftedNoiseNode(shiftX, shiftY, shiftZ, xzScale, yScale, noise));
        }
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String noiseField = context.newField(Noise.class, this.noise);

        BytecodeGen.Context.ValuesMethodDefD shiftXMethod = context.newSingleMethod(this.shiftX);
        BytecodeGen.Context.ValuesMethodDefD shiftYMethod = context.newSingleMethod(this.shiftY);
        BytecodeGen.Context.ValuesMethodDefD shiftZMethod = context.newSingleMethod(this.shiftZ);

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, noiseField, Type.getDescriptor(Noise.class));

        m.load(1, Type.INT_TYPE);
        m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
        m.dconst(this.xzScale);
        m.mul(Type.DOUBLE_TYPE);
        context.callDelegateSingle(m, shiftXMethod);
        m.add(Type.DOUBLE_TYPE);

        m.load(2, Type.INT_TYPE);
        m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
        m.dconst(this.yScale);
        m.mul(Type.DOUBLE_TYPE);
        context.callDelegateSingle(m, shiftYMethod);
        m.add(Type.DOUBLE_TYPE);

        m.load(3, Type.INT_TYPE);
        m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
        m.dconst(this.xzScale);
        m.mul(Type.DOUBLE_TYPE);
        context.callDelegateSingle(m, shiftZMethod);
        m.add(Type.DOUBLE_TYPE);

        m.invokevirtual(
                Type.getInternalName(Noise.class),
                FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_6910$class_7270", "method_42356", "(DDD)D"),
                "(DDD)D",
                false
        );
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String noiseField = context.newField(Noise.class, this.noise);

        BytecodeGen.Context.ValuesMethodDefD shiftXMethod = context.newMultiMethod(this.shiftX);
        BytecodeGen.Context.ValuesMethodDefD shiftYMethod = context.newMultiMethod(this.shiftY);
        BytecodeGen.Context.ValuesMethodDefD shiftZMethod = context.newMultiMethod(this.shiftZ);

        int res1 = localVarConsumer.createLocalVariable("res1", Type.getDescriptor(double[].class));
        int res2 = localVarConsumer.createLocalVariable("res2", Type.getDescriptor(double[].class));

        m.load(6, InstructionAdapter.OBJECT_TYPE);
        m.load(1, InstructionAdapter.OBJECT_TYPE);
        m.arraylength();
        m.iconst(0);
        m.invokevirtual(Type.getInternalName(ArrayCache.class), "getDoubleArray", Type.getMethodDescriptor(Type.getType(double[].class), Type.INT_TYPE, Type.BOOLEAN_TYPE), false);
        m.store(res1, InstructionAdapter.OBJECT_TYPE);

        m.load(6, InstructionAdapter.OBJECT_TYPE);
        m.load(1, InstructionAdapter.OBJECT_TYPE);
        m.arraylength();
        m.iconst(0);
        m.invokevirtual(Type.getInternalName(ArrayCache.class), "getDoubleArray", Type.getMethodDescriptor(Type.getType(double[].class), Type.INT_TYPE, Type.BOOLEAN_TYPE), false);
        m.store(res2, InstructionAdapter.OBJECT_TYPE);

        context.callDelegateMulti(m, shiftXMethod);
        context.callDelegateMulti(m, shiftYMethod, res1);
        context.callDelegateMulti(m, shiftZMethod, res2);

        context.doCountedLoop(m, localVarConsumer, idx -> {
            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);

            {
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(context.className, noiseField, Type.getDescriptor(Noise.class));

                m.load(2, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
                m.dconst(this.xzScale);
                m.mul(Type.DOUBLE_TYPE);
                m.load(1, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.DOUBLE_TYPE);
                m.add(Type.DOUBLE_TYPE);

                m.load(3, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
                m.dconst(this.yScale);
                m.mul(Type.DOUBLE_TYPE);
                m.load(res1, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.DOUBLE_TYPE);
                m.add(Type.DOUBLE_TYPE);

                m.load(4, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
                m.dconst(this.xzScale);
                m.mul(Type.DOUBLE_TYPE);
                m.load(res2, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.DOUBLE_TYPE);
                m.add(Type.DOUBLE_TYPE);

                m.invokevirtual(
                        Type.getInternalName(Noise.class),
                        FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_6910$class_7270", "method_42356", "(DDD)D"),
                        "(DDD)D",
                        false
                );
            }

            m.astore(Type.DOUBLE_TYPE);

        });

        m.load(6, InstructionAdapter.OBJECT_TYPE);
        m.load(res1, InstructionAdapter.OBJECT_TYPE);
        m.invokevirtual(Type.getInternalName(ArrayCache.class), "recycle", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(double[].class)), false);

        m.load(6, InstructionAdapter.OBJECT_TYPE);
        m.load(res2, InstructionAdapter.OBJECT_TYPE);
        m.invokevirtual(Type.getInternalName(ArrayCache.class), "recycle", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(double[].class)), false);

        m.areturn(Type.VOID_TYPE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShiftedNoiseNode that = (ShiftedNoiseNode) o;
        return Double.compare(xzScale, that.xzScale) == 0 && Double.compare(yScale, that.yScale) == 0 && Objects.equals(shiftX, that.shiftX) && Objects.equals(shiftY, that.shiftY) && Objects.equals(shiftZ, that.shiftZ) && Objects.equals(noise, that.noise);
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + shiftX.hashCode();
        result = 31 * result + shiftY.hashCode();
        result = 31 * result + shiftZ.hashCode();
        result = 31 * result + Double.hashCode(xzScale);
        result = 31 * result + Double.hashCode(yScale);
        result = 31 * result + noise.hashCode();

        return result;
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShiftedNoiseNode that = (ShiftedNoiseNode) o;
        return Double.compare(xzScale, that.xzScale) == 0 && Double.compare(yScale, that.yScale) == 0 && shiftX.relaxedEquals(that.shiftX) && shiftY.relaxedEquals(that.shiftY) && shiftZ.relaxedEquals(that.shiftZ);
    }

    @Override
    public int relaxedHashCode() {
        int result = 1;

        result = 31 * result + shiftX.relaxedHashCode();
        result = 31 * result + shiftY.relaxedHashCode();
        result = 31 * result + shiftZ.relaxedHashCode();
        result = 31 * result + Double.hashCode(xzScale);
        result = 31 * result + Double.hashCode(yScale);

        return result;
    }
}
