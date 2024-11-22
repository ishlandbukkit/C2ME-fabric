package com.ishland.c2me.opts.dfc.common.ast.unary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public class NegMulNode extends AbstractUnaryNode {

    public final double negMul;

    public NegMulNode(AstNode operand, double negMul) {
        super(operand);
        this.negMul = negMul;
    }

    @Override
    protected AstNode newInstance(AstNode operand) {
        return new NegMulNode(operand, this.negMul);
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        double v = this.operand.evalSingle(x, y, z, type);
        return v > 0.0 ? v : v * this.negMul;
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        this.operand.evalMulti(res, x, y, z, type);
        for (int i = 0; i < res.length; i++) {
            double v = res[i];
            res[i] = v > 0.0 ? v : v * this.negMul;
        }
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        super.doBytecodeGenSingle(context, m, localVarConsumer);
        int v = localVarConsumer.createLocalVariable("v", Type.DOUBLE_TYPE.getDescriptor());
        m.store(v, Type.DOUBLE_TYPE);

        Label negMulLabel = new Label();
        Label end = new Label();

        m.load(v, Type.DOUBLE_TYPE);
        m.dconst(0.0);
        m.cmpl(Type.DOUBLE_TYPE);
        m.ifle(negMulLabel); // v <= 0.0
        m.load(v, Type.DOUBLE_TYPE);
        m.goTo(end);
        m.visitLabel(negMulLabel);
        m.load(v, Type.DOUBLE_TYPE);
        m.dconst(this.negMul);
        m.mul(Type.DOUBLE_TYPE);
        m.visitLabel(end);
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        super.doBytecodeGenMulti(context, m, localVarConsumer);
        context.doCountedLoop(m, localVarConsumer, idx -> {
            int v = localVarConsumer.createLocalVariable("v", Type.DOUBLE_TYPE.getDescriptor());
            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);
            m.dup2();
            m.aload(Type.DOUBLE_TYPE);

            m.store(v, Type.DOUBLE_TYPE);

            Label negMulLabel = new Label();
            Label end = new Label();

            m.load(v, Type.DOUBLE_TYPE);
            m.dconst(0.0);
            m.cmpl(Type.DOUBLE_TYPE);
            m.ifle(negMulLabel); // v <= 0.0
            m.load(v, Type.DOUBLE_TYPE);
            m.goTo(end);
            m.visitLabel(negMulLabel);
            m.load(v, Type.DOUBLE_TYPE);
            m.dconst(this.negMul);
            m.mul(Type.DOUBLE_TYPE);
            m.visitLabel(end);

            m.astore(Type.DOUBLE_TYPE);
        });
        m.areturn(Type.VOID_TYPE);
    }
}
