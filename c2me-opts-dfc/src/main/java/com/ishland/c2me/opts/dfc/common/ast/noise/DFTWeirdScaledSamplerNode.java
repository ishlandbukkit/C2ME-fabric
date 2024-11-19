package com.ishland.c2me.opts.dfc.common.ast.noise;

import com.ishland.c2me.base.mixin.access.IDensityFunctionTypesWeirdScaledSamplerRarityValueMapper;
import com.ishland.c2me.base.mixin.access.IDensityFunctionsCaveScaler;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Objects;

public class DFTWeirdScaledSamplerNode implements AstNode {

    private final AstNode input;
    private final DensityFunction.Noise noise;
    private final DensityFunctionTypes.WeirdScaledSampler.RarityValueMapper mapper;

    public DFTWeirdScaledSamplerNode(AstNode input, DensityFunction.Noise noise, DensityFunctionTypes.WeirdScaledSampler.RarityValueMapper mapper) {
        this.input = Objects.requireNonNull(input);
        this.noise = Objects.requireNonNull(noise);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        double v = this.input.evalSingle(x, y, z, type);
        double d = ((IDensityFunctionTypesWeirdScaledSamplerRarityValueMapper) (Object) this.mapper).getScaleFunction().get(v);
        return d * Math.abs(this.noise.sample((double) x / d, (double) y / d, (double) z / d));
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        this.input.evalMulti(res, x, y, z, type);
        for (int i = 0; i < res.length; i++) {
            double d = ((IDensityFunctionTypesWeirdScaledSamplerRarityValueMapper) (Object) this.mapper).getScaleFunction().get(res[i]);
            res[i] = d * Math.abs(this.noise.sample((double) x[i] / d, (double) y[i] / d, (double) z[i] / d));
        }
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[]{this.input};
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode input = this.input.transform(transformer);
        if (input == this.input) {
            return transformer.transform(this);
        } else {
            return transformer.transform(new DFTWeirdScaledSamplerNode(input, this.noise, this.mapper));
        }
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD inputMethod = context.newSingleMethod(this.input);
        String noiseField = context.newField(DensityFunction.Noise.class, this.noise);
        int scale = localVarConsumer.createLocalVariable("scale", Type.DOUBLE_TYPE.getDescriptor());

        context.callDelegateSingle(m, inputMethod);

        switch (this.mapper) {
            case TYPE1 -> m.invokestatic(
                    Type.getInternalName(IDensityFunctionsCaveScaler.class),
                    "invokeScaleTunnels",
                    Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE),
                    true
            );
            case TYPE2 -> m.invokestatic(
                    Type.getInternalName(IDensityFunctionsCaveScaler.class),
                    "invokeScaleCaves",
                    Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE),
                    true
            );
            default -> throw new UnsupportedOperationException(String.format("Unknown mapper %s", this.mapper));
        }

        m.store(scale, Type.DOUBLE_TYPE);

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, noiseField, Type.getDescriptor(DensityFunction.Noise.class));

        m.load(1, Type.INT_TYPE);
        m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
        m.load(scale, Type.DOUBLE_TYPE);
        m.div(Type.DOUBLE_TYPE);

        m.load(2, Type.INT_TYPE);
        m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
        m.load(scale, Type.DOUBLE_TYPE);
        m.div(Type.DOUBLE_TYPE);

        m.load(3, Type.INT_TYPE);
        m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
        m.load(scale, Type.DOUBLE_TYPE);
        m.div(Type.DOUBLE_TYPE);

        m.invokevirtual(
                Type.getInternalName(DensityFunction.Noise.class),
                FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_6910$class_7270", "method_42356", "(DDD)D"),
                "(DDD)D",
                false
        );
        m.invokestatic(
                Type.getInternalName(Math.class),
                "abs",
                "(D)D",
                false
        );
        m.load(scale, Type.DOUBLE_TYPE);
        m.mul(Type.DOUBLE_TYPE);
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD inputMethod = context.newMultiMethod(this.input);
        String noiseField = context.newField(DensityFunction.Noise.class, this.noise);

        context.callDelegateMulti(m, inputMethod);
        context.doCountedLoop(m, localVarConsumer, idx -> {
            int scale = localVarConsumer.createLocalVariable("scale", Type.DOUBLE_TYPE.getDescriptor());

            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);

            {
                m.load(1, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.DOUBLE_TYPE);

                switch (this.mapper) {
                    case TYPE1 -> m.invokestatic(
                            Type.getInternalName(IDensityFunctionsCaveScaler.class),
                            "invokeScaleTunnels",
                            Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE),
                            true
                    );
                    case TYPE2 -> m.invokestatic(
                            Type.getInternalName(IDensityFunctionsCaveScaler.class),
                            "invokeScaleCaves",
                            Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE),
                            true
                    );
                    default -> throw new UnsupportedOperationException(String.format("Unknown mapper %s", this.mapper));
                }

                m.store(scale, Type.DOUBLE_TYPE);

                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(context.className, noiseField, Type.getDescriptor(DensityFunction.Noise.class));

                m.load(2, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
                m.load(scale, Type.DOUBLE_TYPE);
                m.div(Type.DOUBLE_TYPE);

                m.load(3, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
                m.load(scale, Type.DOUBLE_TYPE);
                m.div(Type.DOUBLE_TYPE);

                m.load(4, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
                m.load(scale, Type.DOUBLE_TYPE);
                m.div(Type.DOUBLE_TYPE);

                m.invokevirtual(
                        Type.getInternalName(DensityFunction.Noise.class),
                        FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_6910$class_7270", "method_42356", "(DDD)D"),
                        "(DDD)D",
                        false
                );
                m.invokestatic(
                        Type.getInternalName(Math.class),
                        "abs",
                        "(D)D",
                        false
                );

                m.load(scale, Type.DOUBLE_TYPE);
                m.mul(Type.DOUBLE_TYPE);
            }

            m.astore(Type.DOUBLE_TYPE);
        });

        m.areturn(Type.VOID_TYPE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DFTWeirdScaledSamplerNode that = (DFTWeirdScaledSamplerNode) o;
        return Objects.equals(input, that.input) && Objects.equals(noise, that.noise) && mapper == that.mapper;
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + input.hashCode();
        result = 31 * result + noise.hashCode();
        result = 31 * result + mapper.hashCode();

        return result;
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DFTWeirdScaledSamplerNode that = (DFTWeirdScaledSamplerNode) o;
        return input.relaxedEquals(that.input) && mapper == that.mapper;
    }

    @Override
    public int relaxedHashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + input.relaxedHashCode();
        result = 31 * result + mapper.hashCode();

        return result;
    }
}
