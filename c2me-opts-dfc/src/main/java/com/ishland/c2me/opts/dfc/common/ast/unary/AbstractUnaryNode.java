package com.ishland.c2me.opts.dfc.common.ast.unary;

import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Objects;

public abstract class AbstractUnaryNode implements AstNode {

    protected final AstNode operand;

    public AbstractUnaryNode(AstNode operand) {
        this.operand = Objects.requireNonNull(operand);
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[]{operand};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractUnaryNode that = (AbstractUnaryNode) o;
        return Objects.equals(operand, that.operand);
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + operand.hashCode();

        return result;
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractUnaryNode that = (AbstractUnaryNode) o;
        return operand.relaxedEquals(that.operand);
    }

    @Override
    public int relaxedHashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + operand.relaxedHashCode();

        return result;
    }

    protected abstract AstNode newInstance(AstNode operand);

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode operand = this.operand.transform(transformer);
        if (this.operand == operand) {
            return transformer.transform(this);
        } else {
            return transformer.transform(newInstance(operand));
        }
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD operandMethod = context.newSingleMethod(this.operand);
        context.callDelegateSingle(m, operandMethod);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD operandMethod = context.newMultiMethod(this.operand);
        context.callDelegateMulti(m, operandMethod);
    }
}
