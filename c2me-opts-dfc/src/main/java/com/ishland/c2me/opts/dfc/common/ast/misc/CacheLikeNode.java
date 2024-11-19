package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import com.ishland.c2me.opts.dfc.common.gen.IMultiMethod;
import com.ishland.c2me.opts.dfc.common.gen.ISingleMethod;
import com.ishland.c2me.opts.dfc.common.gen.SubCompiledDensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Objects;

public class CacheLikeNode implements AstNode {

    private final IFastCacheLike cacheLike;
    private final AstNode delegate;

    public CacheLikeNode(IFastCacheLike cacheLike, AstNode delegate) {
        this.cacheLike = cacheLike;
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        if (this.cacheLike == null) {
            return this.delegate.evalSingle(x, y, z, type);
        }
        double cached = this.cacheLike.c2me$getCached(x, y, z, type);
        if (Double.doubleToRawLongBits(cached) != IFastCacheLike.CACHE_MISS_NAN_BITS) {
            return cached;
        } else {
            double eval = this.delegate.evalSingle(x, y, z, type);
            this.cacheLike.c2me$cache(x, y, z, type, eval);
            return eval;
        }
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        if (this.cacheLike == null) {
            this.delegate.evalMulti(res, x, y, z, type);
            return;
        }
        boolean cached = this.cacheLike.c2me$getCached(res, x, y, z, type);
        if (!cached) {
            this.delegate.evalMulti(res, x, y, z, type);
            this.cacheLike.c2me$cache(res, x, y, z, type);
        }
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[]{this.delegate};
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode delegate = this.delegate.transform(transformer);
        if (this.delegate == delegate) {
            return transformer.transform(this);
        } else {
            return transformer.transform(new CacheLikeNode(this.cacheLike, delegate));
        }
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD delegateMethod = context.newSingleMethod(this.delegate);
        String cacheLikeField = context.newField(IFastCacheLike.class, this.cacheLike);
        genPostprocessingMethod(context, cacheLikeField);

        int eval = localVarConsumer.createLocalVariable("eval", Type.DOUBLE_TYPE.getDescriptor());

        Label cacheExists = new Label();
        Label cacheMiss = new Label();

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, cacheLikeField, Type.getDescriptor(IFastCacheLike.class));
        m.ifnonnull(cacheExists);
        context.callDelegateSingle(m, delegateMethod);
        m.areturn(Type.DOUBLE_TYPE);

        m.visitLabel(cacheExists);
        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, cacheLikeField, Type.getDescriptor(IFastCacheLike.class));
        m.load(1, Type.INT_TYPE);
        m.load(2, Type.INT_TYPE);
        m.load(3, Type.INT_TYPE);
        m.load(4, InstructionAdapter.OBJECT_TYPE);
        m.invokeinterface(Type.getInternalName(IFastCacheLike.class), "c2me$getCached", Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE, Type.getType(EvalType.class)));
        m.dup2();
        m.invokestatic(Type.getInternalName(Double.class), "doubleToRawLongBits", Type.getMethodDescriptor(Type.LONG_TYPE, Type.DOUBLE_TYPE), false);
        m.lconst(IFastCacheLike.CACHE_MISS_NAN_BITS);
        m.lcmp();
        m.ifeq(cacheMiss); // operand1 == operand2, branched with cache res
        m.areturn(Type.DOUBLE_TYPE);

        m.visitLabel(cacheMiss);
        m.pop2();

        context.callDelegateSingle(m, delegateMethod);
        m.store(eval, Type.DOUBLE_TYPE);
        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, cacheLikeField, Type.getDescriptor(IFastCacheLike.class));
        m.load(1, Type.INT_TYPE);
        m.load(2, Type.INT_TYPE);
        m.load(3, Type.INT_TYPE);
        m.load(4, InstructionAdapter.OBJECT_TYPE);
        m.load(eval, Type.DOUBLE_TYPE);
        m.invokeinterface(Type.getInternalName(IFastCacheLike.class), "c2me$cache", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE, Type.getType(EvalType.class), Type.DOUBLE_TYPE));

        m.load(eval, Type.DOUBLE_TYPE);
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD delegateMethod = context.newMultiMethod(this.delegate);
        String cacheLikeField = context.newField(IFastCacheLike.class, this.cacheLike);

        genPostprocessingMethod(context, cacheLikeField);

        Label cacheExists = new Label();
        Label cacheMiss = new Label();

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, cacheLikeField, Type.getDescriptor(IFastCacheLike.class));
        m.ifnonnull(cacheExists);
        context.callDelegateMulti(m, delegateMethod);
        m.areturn(Type.VOID_TYPE);

        m.visitLabel(cacheExists);
        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, cacheLikeField, Type.getDescriptor(IFastCacheLike.class));
        m.load(1, InstructionAdapter.OBJECT_TYPE);
        m.load(2, InstructionAdapter.OBJECT_TYPE);
        m.load(3, InstructionAdapter.OBJECT_TYPE);
        m.load(4, InstructionAdapter.OBJECT_TYPE);
        m.load(5, InstructionAdapter.OBJECT_TYPE);
        m.invokeinterface(Type.getInternalName(IFastCacheLike.class), "c2me$getCached", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(double[].class), Type.getType(int[].class), Type.getType(int[].class), Type.getType(int[].class), Type.getType(EvalType.class)));
        m.ifeq(cacheMiss);
        m.areturn(Type.VOID_TYPE);

        m.visitLabel(cacheMiss);
        context.callDelegateMulti(m, delegateMethod);
        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, cacheLikeField, Type.getDescriptor(IFastCacheLike.class));
        m.load(1, InstructionAdapter.OBJECT_TYPE);
        m.load(2, InstructionAdapter.OBJECT_TYPE);
        m.load(3, InstructionAdapter.OBJECT_TYPE);
        m.load(4, InstructionAdapter.OBJECT_TYPE);
        m.load(5, InstructionAdapter.OBJECT_TYPE);
        m.invokeinterface(Type.getInternalName(IFastCacheLike.class), "c2me$cache", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(double[].class), Type.getType(int[].class), Type.getType(int[].class), Type.getType(int[].class), Type.getType(EvalType.class)));
        m.areturn(Type.VOID_TYPE);
    }

    private void genPostprocessingMethod(BytecodeGen.Context context, String cacheLikeField) {
        String methodName = String.format("postProcessing_%s", cacheLikeField);
        String delegateSingle = context.newSingleMethodUnoptimized(this.delegate);
        String delegateMulti = context.newMultiMethodUnoptimized(this.delegate);
        context.genPostprocessingMethod(methodName, m -> {
            Label cacheExists = new Label();

            m.load(0, InstructionAdapter.OBJECT_TYPE);

            {
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(context.className, cacheLikeField, Type.getDescriptor(IFastCacheLike.class));
                m.dup();
                m.ifnonnull(cacheExists);
                m.pop();
                m.pop();
                m.areturn(Type.VOID_TYPE);

                m.visitLabel(cacheExists);

                {
                    m.anew(Type.getType(SubCompiledDensityFunction.class));
                    m.dup();

                    m.load(0, InstructionAdapter.OBJECT_TYPE);
                    m.invokedynamic(
                            "evalSingle",
                            Type.getMethodDescriptor(Type.getType(ISingleMethod.class), Type.getType(context.classDesc)),
                            new Handle(
                                    Opcodes.H_INVOKESTATIC,
                                    "java/lang/invoke/LambdaMetafactory",
                                    "metafactory",
                                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                                    false
                            ),
                            new Object[]{
                                    Type.getMethodType(BytecodeGen.Context.SINGLE_DESC),
                                    new Handle(
                                            Opcodes.H_INVOKEVIRTUAL,
                                            context.className,
                                            delegateSingle,
                                            BytecodeGen.Context.SINGLE_DESC,
                                            false
                                    ),
                                    Type.getMethodType(BytecodeGen.Context.SINGLE_DESC)
                            }
                    );

                    m.load(0, InstructionAdapter.OBJECT_TYPE);
                    m.invokedynamic(
                            "evalMulti",
                            Type.getMethodDescriptor(Type.getType(IMultiMethod.class), Type.getType(context.classDesc)),
                            new Handle(
                                    Opcodes.H_INVOKESTATIC,
                                    "java/lang/invoke/LambdaMetafactory",
                                    "metafactory",
                                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                                    false
                            ),
                            new Object[]{
                                    Type.getMethodType(BytecodeGen.Context.MULTI_DESC),
                                    new Handle(
                                            Opcodes.H_INVOKEVIRTUAL,
                                            context.className,
                                            delegateMulti,
                                            BytecodeGen.Context.MULTI_DESC,
                                            false
                                    ),
                                    Type.getMethodType(BytecodeGen.Context.MULTI_DESC)
                            }
                    );

                    m.load(0, InstructionAdapter.OBJECT_TYPE);
                    m.getfield(context.className, cacheLikeField, Type.getDescriptor(IFastCacheLike.class));
                    m.checkcast(Type.getType(DensityFunction.class));

                    m.invokespecial(
                            Type.getInternalName(SubCompiledDensityFunction.class),
                            "<init>",
                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ISingleMethod.class), Type.getType(IMultiMethod.class), Type.getType(DensityFunction.class)),
                            false
                    );

                    m.checkcast(Type.getType(DensityFunction.class));
                }

                m.invokeinterface(
                        Type.getInternalName(IFastCacheLike.class),
                        "c2me$withDelegate",
                        Type.getMethodDescriptor(Type.getType(DensityFunction.class), Type.getType(DensityFunction.class))
                );
            }

            m.putfield(context.className, cacheLikeField, Type.getDescriptor(IFastCacheLike.class));

            m.areturn(Type.VOID_TYPE);
        });
    }

    public IFastCacheLike getCacheLike() {
        return cacheLike;
    }

    public AstNode getDelegate() {
        return delegate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheLikeNode that = (CacheLikeNode) o;
        return equals(cacheLike, that.cacheLike) && Objects.equals(delegate, that.delegate);
    }

    private static boolean equals(IFastCacheLike a, IFastCacheLike b) {
        if ((Object) a instanceof DensityFunctionTypes.Wrapping wrappingA && (Object) b instanceof DensityFunctionTypes.Wrapping wrappingB) {
            return wrappingA.type() == wrappingB.type();
        } else {
            return a.equals(b);
        }
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + hashCode(cacheLike);
        result = 31 * result + delegate.hashCode();

        return result;
    }

    private static int hashCode(IFastCacheLike o) {
        if ((Object) o instanceof DensityFunctionTypes.Wrapping wrapping) {
            return wrapping.type().hashCode();
        } else {
            return o.hashCode();
        }
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheLikeNode that = (CacheLikeNode) o;
        return relaxedEquals(cacheLike, that.cacheLike) && delegate.relaxedEquals(that.delegate);
    }

    private static boolean relaxedEquals(IFastCacheLike a, IFastCacheLike b) {
        if ((Object) a instanceof DensityFunctionTypes.Wrapping wrappingA && (Object) b instanceof DensityFunctionTypes.Wrapping wrappingB) {
            return wrappingA.type() == wrappingB.type();
        } else {
            return a.getClass() == b.getClass();
        }
    }

    @Override
    public int relaxedHashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + relaxedHashCode(this.cacheLike);
        result = 31 * result + delegate.relaxedHashCode();

        return result;
    }

    private static int relaxedHashCode(IFastCacheLike o) {
        if ((Object) o instanceof DensityFunctionTypes.Wrapping wrapping) {
            return wrapping.type().hashCode();
        } else {
            return o.getClass().hashCode();
        }
    }
}
