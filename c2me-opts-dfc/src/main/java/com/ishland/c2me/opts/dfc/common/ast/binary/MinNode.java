package com.ishland.c2me.opts.dfc.common.ast.binary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public class MinNode extends AbstractBinaryNode {

    public MinNode(AstNode left, AstNode right) {
        super(left, right);
    }

    @Override
    protected AstNode newInstance(AstNode left, AstNode right) {
        return new MinNode(left, right);
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        return Math.min(this.left.evalSingle(x, y, z, type), this.right.evalSingle(x, y, z, type));
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        double[] res1 = new double[res.length];
        this.left.evalMulti(res, x, y, z, type);
        this.right.evalMulti(res1, x, y, z, type);
        for (int i = 0; i < res1.length; i++) {
            res[i] = Math.min(res[i], res1[i]);
        }
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        super.doBytecodeGenSingle(context, m, localVarConsumer);
        m.invokestatic(
                Type.getInternalName(Math.class),
                "min",
                Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE),
                false
        );
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    protected void bytecodeGenMultiBody(InstructionAdapter m, int idx, int res1) {
        m.load(1, InstructionAdapter.OBJECT_TYPE);
        m.load(idx, Type.INT_TYPE);
        m.dup2();
        m.aload(Type.DOUBLE_TYPE);
        m.load(res1, InstructionAdapter.OBJECT_TYPE);
        m.load(idx, Type.INT_TYPE);
        m.aload(Type.DOUBLE_TYPE);
        m.invokestatic(
                Type.getInternalName(Math.class),
                "min",
                Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE),
                false
        );
        m.astore(Type.DOUBLE_TYPE);
    }
}
