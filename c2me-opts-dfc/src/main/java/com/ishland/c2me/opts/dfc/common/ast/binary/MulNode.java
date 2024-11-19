package com.ishland.c2me.opts.dfc.common.ast.binary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public class MulNode extends AbstractBinaryNode {

    public MulNode(AstNode left, AstNode right) {
        super(left, right);
    }

    @Override
    protected AstNode newInstance(AstNode left, AstNode right) {
        return new MulNode(left, right);
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        double evaled = this.left.evalSingle(x, y, z, type);
        return evaled == 0.0 ? 0.0 : evaled * this.right.evalSingle(x, y, z, type);
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        this.left.evalMulti(res, x, y, z, type);
        for (int i = 0; i < res.length; i++) {
            res[i] = res[i] == 0.0 ? 0.0 : res[i] * this.right.evalSingle(x[i], y[i], z[i], type);
        }
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD leftMethod = context.newSingleMethod(this.left);
        BytecodeGen.Context.ValuesMethodDefD rightMethod = context.newSingleMethod(this.right);

        Label notZero = new Label();

        context.callDelegateSingle(m, leftMethod);
        m.dup2();
        m.dconst(0.0);
        m.cmpl(Type.DOUBLE_TYPE);
        m.ifne(notZero);
        m.dconst(0.0);
        m.areturn(Type.DOUBLE_TYPE);

        m.visitLabel(notZero);
        context.callDelegateSingle(m, rightMethod);
        m.mul(Type.DOUBLE_TYPE);
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD leftMethod = context.newMultiMethod(this.left);
        BytecodeGen.Context.ValuesMethodDefD rightMethodSingle = context.newSingleMethod(this.right);
        context.callDelegateMulti(m, leftMethod);

        context.doCountedLoop(m, localVarConsumer, idx -> {
            Label minLabel = new Label();
            Label end = new Label();

            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);

            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);
            m.aload(Type.DOUBLE_TYPE);

            m.dup2();
            m.dconst(0.0);
            m.cmpl(Type.DOUBLE_TYPE);
            m.ifne(minLabel);
            m.pop2();
            m.dconst(0.0);
            m.goTo(end);

            m.visitLabel(minLabel);
            context.callDelegateSingleFromMulti(m, rightMethodSingle, idx);
            m.mul(Type.DOUBLE_TYPE);

            m.visitLabel(end);
            m.astore(Type.DOUBLE_TYPE);
        });

        m.areturn(Type.VOID_TYPE);
    }

    @Override
    protected void bytecodeGenMultiBody(InstructionAdapter m, int idx, int res1) {
        throw new UnsupportedOperationException();
    }
}
