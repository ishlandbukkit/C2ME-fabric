package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Objects;

public class RangeChoiceNode implements AstNode {

    private final AstNode input;
    private final double minInclusive;
    private final double maxExclusive;
    private final AstNode whenInRange;
    private final AstNode whenOutOfRange;

    public RangeChoiceNode(AstNode input, double minInclusive, double maxExclusive, AstNode whenInRange, AstNode whenOutOfRange) {
        this.input = Objects.requireNonNull(input);
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
        this.whenInRange = Objects.requireNonNull(whenInRange);
        this.whenOutOfRange = Objects.requireNonNull(whenOutOfRange);
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        double v = this.input.evalSingle(x, y, z, type);
        return v >= this.minInclusive && v < this.maxExclusive ? this.whenInRange.evalSingle(x, y, z, type) : this.whenOutOfRange.evalSingle(x, y, z, type);
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        this.input.evalMulti(res, x, y, z, type);

//        for (int i = 0; i < res.length; i ++) {
//            double v = res[i];
//            if (v >= this.minInclusive && v < this.maxExclusive) {
//                res[i] = this.whenInRange.evalSingle(x[i], y[i], z[i], type);
//            } else {
//                res[i] = this.whenOutOfRange.evalSingle(x[i], y[i], z[i], type);
//            }
//        }

        int numInRange = 0;
        for (int i = 0; i < x.length; i++) {
            double v = res[i];
            if (v >= this.minInclusive && v < this.maxExclusive) {
                numInRange++;
            }
        }
        int numOutOfRange = res.length - numInRange;

        if (numInRange == 0) {
            evalChildMulti(this.whenOutOfRange, res, x, y, z, type);
        } else if (numInRange == res.length) {
            evalChildMulti(this.whenInRange, res, x, y, z, type);
        } else {
            int idx1 = 0;
            int[] i1 = new int[numInRange];
            double[] res1 = new double[numInRange];
            int[] x1 = new int[numInRange];
            int[] y1 = new int[numInRange];
            int[] z1 = new int[numInRange];
            int idx2 = 0;
            int[] i2 = new int[numOutOfRange];
            double[] res2 = new double[numOutOfRange];
            int[] x2 = new int[numOutOfRange];
            int[] y2 = new int[numOutOfRange];
            int[] z2 = new int[numOutOfRange];
            for (int i = 0; i < res.length; i++) {
                double v = res[i];
                if (v >= this.minInclusive && v < this.maxExclusive) {
                    int index = idx1++;
                    i1[index] = i;
                    x1[index] = x[i];
                    y1[index] = y[i];
                    z1[index] = z[i];
                } else {
                    int index = idx2++;
                    i2[index] = i;
                    x2[index] = x[i];
                    y2[index] = y[i];
                    z2[index] = z[i];
                }
            }
            evalChildMulti(this.whenInRange, res1, x1, y1, z1, type);
            evalChildMulti(this.whenOutOfRange, res2, x2, y2, z2, type);
            for (int i = 0; i < numInRange; i++) {
                res[i1[i]] = res1[i];
            }
            for (int i = 0; i < numOutOfRange; i++) {
                res[i2[i]] = res2[i];
            }
        }
    }

    // called by generated code
    public static void evalMultiStatic(double[] res, int[] x, int[] y, int[] z, EvalType type, double minInclusive, double maxExclusive,
                                       BytecodeGen.EvalSingleInterface whenInRangeSingle, BytecodeGen.EvalSingleInterface whenOutOfRangeSingle,
                                       BytecodeGen.EvalMultiInterface inputMulti, BytecodeGen.EvalMultiInterface whenInRangeMulti, BytecodeGen.EvalMultiInterface whenOutOfRangeMulti) {
        inputMulti.evalMulti(res, x, y, z, type);

//        for (int i = 0; i < res.length; i ++) {
//            double v = res[i];
//            if (v >= this.minInclusive && v < this.maxExclusive) {
//                res[i] = this.whenInRange.evalSingle(x[i], y[i], z[i], type);
//            } else {
//                res[i] = this.whenOutOfRange.evalSingle(x[i], y[i], z[i], type);
//            }
//        }

        int numInRange = 0;
        for (int i = 0; i < x.length; i++) {
            double v = res[i];
            if (v >= minInclusive && v < maxExclusive) {
                numInRange++;
            }
        }
        int numOutOfRange = res.length - numInRange;

        if (numInRange == 0) {
            evalChildMulti(whenOutOfRangeSingle, whenOutOfRangeMulti, res, x, y, z, type);
        } else if (numInRange == res.length) {
            evalChildMulti(whenInRangeSingle, whenInRangeMulti, res, x, y, z, type);
        } else {
            int idx1 = 0;
            int[] i1 = new int[numInRange];
            double[] res1 = new double[numInRange];
            int[] x1 = new int[numInRange];
            int[] y1 = new int[numInRange];
            int[] z1 = new int[numInRange];
            int idx2 = 0;
            int[] i2 = new int[numOutOfRange];
            double[] res2 = new double[numOutOfRange];
            int[] x2 = new int[numOutOfRange];
            int[] y2 = new int[numOutOfRange];
            int[] z2 = new int[numOutOfRange];
            for (int i = 0; i < res.length; i++) {
                double v = res[i];
                if (v >= minInclusive && v < maxExclusive) {
                    int index = idx1++;
                    i1[index] = i;
                    x1[index] = x[i];
                    y1[index] = y[i];
                    z1[index] = z[i];
                } else {
                    int index = idx2++;
                    i2[index] = i;
                    x2[index] = x[i];
                    y2[index] = y[i];
                    z2[index] = z[i];
                }
            }
            evalChildMulti(whenInRangeSingle, whenInRangeMulti, res1, x1, y1, z1, type);
            evalChildMulti(whenOutOfRangeSingle, whenOutOfRangeMulti, res2, x2, y2, z2, type);
            for (int i = 0; i < numInRange; i++) {
                res[i1[i]] = res1[i];
            }
            for (int i = 0; i < numOutOfRange; i++) {
                res[i2[i]] = res2[i];
            }
        }
    }

    private static void evalChildMulti(BytecodeGen.EvalSingleInterface single, BytecodeGen.EvalMultiInterface multi, double[] res, int[] x, int[] y, int[] z, EvalType type) {
        if (res.length == 1) {
            res[0] = single.evalSingle(x[0], y[0], z[0], type);
        } else {
            multi.evalMulti(res, x, y, z, type);
        }
    }

    private void evalChildMulti(AstNode child, double[] res, int[] x, int[] y, int[] z, EvalType type) {
        if (res.length == 1) {
            res[0] = child.evalSingle(x[0], y[0], z[0], type);
        } else {
            child.evalMulti(res, x, y, z, type);
        }
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[]{this.input, this.whenInRange, this.whenOutOfRange};
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode input = this.input.transform(transformer);
        AstNode whenInRange = this.whenInRange.transform(transformer);
        AstNode whenOutOfRange = this.whenOutOfRange.transform(transformer);
        if (this.input == input && this.whenInRange == whenInRange && this.whenOutOfRange == whenOutOfRange) {
            return transformer.transform(this);
        } else {
            return transformer.transform(new RangeChoiceNode(input, minInclusive, maxExclusive, whenInRange, whenOutOfRange));
        }
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD inputMethod = context.newSingleMethod(this.input);
        BytecodeGen.Context.ValuesMethodDefD whenInRangeMethod = context.newSingleMethod(this.whenInRange);
        BytecodeGen.Context.ValuesMethodDefD whenOutOfRangeMethod = context.newSingleMethod(this.whenOutOfRange);

        int inputValue = localVarConsumer.createLocalVariable("inputValue", Type.DOUBLE_TYPE.getDescriptor());
        context.callDelegateSingle(m, inputMethod);
        m.store(inputValue, Type.DOUBLE_TYPE);

        Label whenOutOfRangeLabel = new Label();
        Label end = new Label();

        m.load(inputValue, Type.DOUBLE_TYPE);
        m.dconst(this.minInclusive);
        m.cmpl(Type.DOUBLE_TYPE);
        m.iflt(whenOutOfRangeLabel); // inputValue < minInclusive
        m.load(inputValue, Type.DOUBLE_TYPE);
        m.dconst(this.maxExclusive);
        m.cmpg(Type.DOUBLE_TYPE);
        m.ifge(whenOutOfRangeLabel); // inputValue >= maxExclusive

        if (whenInRangeMethod.equals(inputMethod)) {
            m.load(inputValue, Type.DOUBLE_TYPE);
        } else {
            context.callDelegateSingle(m, whenInRangeMethod);
        }
        m.goTo(end);

        m.visitLabel(whenOutOfRangeLabel);
        if (whenOutOfRangeMethod.equals(inputMethod)) {
            m.load(inputValue, Type.DOUBLE_TYPE);
        } else {
            context.callDelegateSingle(m, whenOutOfRangeMethod);
        }

        m.visitLabel(end);
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD inputSingle = context.newSingleMethod(this.input);
        BytecodeGen.Context.ValuesMethodDefD whenInRangeSingle = context.newSingleMethod(this.whenInRange);
        BytecodeGen.Context.ValuesMethodDefD whenOutOfRangeSingle = context.newSingleMethod(this.whenOutOfRange);
        BytecodeGen.Context.ValuesMethodDefD inputMulti = context.newMultiMethod(this.input);
//        String whenInRangeMulti = context.newMultiMethod(this.whenInRange);
//        String whenOutOfRangeMulti = context.newMultiMethod(this.whenOutOfRange);

        context.callDelegateMulti(m, inputMulti);

        context.doCountedLoop(m, localVarConsumer, idx -> {
            Label whenOutOfRangeLabel = new Label();
            Label end = new Label();

            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);

//            m.load(inputValue, Type.DOUBLE_TYPE);
            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);
            m.aload(Type.DOUBLE_TYPE);
            m.dconst(this.minInclusive);
            m.cmpl(Type.DOUBLE_TYPE);
            m.iflt(whenOutOfRangeLabel); // inputValue < minInclusive
//            m.load(inputValue, Type.DOUBLE_TYPE);
            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);
            m.aload(Type.DOUBLE_TYPE);
            m.dconst(this.maxExclusive);
            m.cmpg(Type.DOUBLE_TYPE);
            m.ifge(whenOutOfRangeLabel); // inputValue >= maxExclusive

//            context.callDelegateSingle(m, whenInRangeSingle);
            if (whenInRangeSingle.equals(inputSingle)) {
                m.load(1, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.DOUBLE_TYPE);
            } else {
                context.callDelegateSingleFromMulti(m, whenInRangeSingle, idx);
            }
            m.goTo(end);

            m.visitLabel(whenOutOfRangeLabel);
            if (whenOutOfRangeSingle.equals(inputSingle)) {
                m.load(1, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.DOUBLE_TYPE);
            } else {
                context.callDelegateSingleFromMulti(m, whenOutOfRangeSingle, idx);
            }

            m.visitLabel(end);
            m.astore(Type.DOUBLE_TYPE);
        });

        m.areturn(Type.VOID_TYPE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RangeChoiceNode that = (RangeChoiceNode) o;
        return Double.compare(minInclusive, that.minInclusive) == 0 && Double.compare(maxExclusive, that.maxExclusive) == 0 && Objects.equals(input, that.input) && Objects.equals(whenInRange, that.whenInRange) && Objects.equals(whenOutOfRange, that.whenOutOfRange);
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + this.input.hashCode();
        result = 31 * result + Double.hashCode(this.minInclusive);
        result = 31 * result + Double.hashCode(this.maxExclusive);
        result = 31 * result + this.whenInRange.hashCode();
        result = 31 * result + this.whenOutOfRange.hashCode();

        return result;
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RangeChoiceNode that = (RangeChoiceNode) o;
        return Double.compare(minInclusive, that.minInclusive) == 0 && Double.compare(maxExclusive, that.maxExclusive) == 0 && input.relaxedEquals(that.input) && whenInRange.relaxedEquals(that.whenInRange) && whenOutOfRange.relaxedEquals(that.whenOutOfRange);
    }

    @Override
    public int relaxedHashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + this.input.relaxedHashCode();
        result = 31 * result + Double.hashCode(this.minInclusive);
        result = 31 * result + Double.hashCode(this.maxExclusive);
        result = 31 * result + this.whenInRange.relaxedHashCode();
        result = 31 * result + this.whenOutOfRange.relaxedHashCode();

        return result;
    }
}
