package com.ishland.c2me.opts.dfc.common.ast.binary;

import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import com.ishland.c2me.opts.dfc.common.util.ArrayCache;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Objects;

public abstract class AbstractBinaryNode implements AstNode {

    public final AstNode left;
    public final AstNode right;

    public AbstractBinaryNode(AstNode left, AstNode right) {
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[]{left, right};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractBinaryNode that = (AbstractBinaryNode) o;
        return Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + left.hashCode();
        result = 31 * result + right.hashCode();

        return result;
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractBinaryNode that = (AbstractBinaryNode) o;
        return left.relaxedEquals(that.left) && right.relaxedEquals(that.right);
    }

    @Override
    public int relaxedHashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + left.relaxedHashCode();
        result = 31 * result + right.relaxedHashCode();

        return result;
    }

    protected abstract AstNode newInstance(AstNode left, AstNode right);

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode left = this.left.transform(transformer);
        AstNode right = this.right.transform(transformer);
        if (left == this.left && right == this.right) {
            return transformer.transform(this);
        } else {
            return transformer.transform(newInstance(left, right));
        }
    }

    public AstNode swapOperands() {
        return newInstance(this.right, this.left);
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD leftMethod = context.newSingleMethod(this.left);
        BytecodeGen.Context.ValuesMethodDefD rightMethod = context.newSingleMethod(this.right);

        context.callDelegateSingle(m, leftMethod);
        context.callDelegateSingle(m, rightMethod);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD leftMethod = context.newMultiMethod(this.left);
        BytecodeGen.Context.ValuesMethodDefD rightMethod = context.newMultiMethod(this.right);

        int res1 = localVarConsumer.createLocalVariable("res1", Type.getDescriptor(double[].class));

        m.load(6, InstructionAdapter.OBJECT_TYPE);
        m.load(1, InstructionAdapter.OBJECT_TYPE);
        m.arraylength();
        m.iconst(0);
        m.invokevirtual(Type.getInternalName(ArrayCache.class), "getDoubleArray", Type.getMethodDescriptor(Type.getType(double[].class), Type.INT_TYPE, Type.BOOLEAN_TYPE), false);
        m.store(res1, InstructionAdapter.OBJECT_TYPE);
        context.callDelegateMulti(m, leftMethod);
        context.callDelegateMulti(m, rightMethod, res1);

        context.doCountedLoop(m, localVarConsumer, idx -> bytecodeGenMultiBody(m, idx, res1));

        m.load(6, InstructionAdapter.OBJECT_TYPE);
        m.load(res1, InstructionAdapter.OBJECT_TYPE);
        m.invokevirtual(Type.getInternalName(ArrayCache.class), "recycle", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(double[].class)), false);

        m.areturn(Type.VOID_TYPE);
    }

    protected abstract void bytecodeGenMultiBody(InstructionAdapter m, int idx, int res1);


}
