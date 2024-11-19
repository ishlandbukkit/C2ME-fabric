package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Objects;

public class RootNode implements AstNode {

    private final AstNode next;

    public RootNode(AstNode next) {
        this.next = Objects.requireNonNull(next);
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        return next.evalSingle(x, y, z, type);
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        next.evalMulti(res, x, y, z, type);
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[]{next};
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode next = this.next.transform(transformer);
        if (next == this.next) {
            return transformer.transform(this);
        } else {
            return transformer.transform(new RootNode(next));
        }
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD nextMethod = context.newSingleMethod(this.next);
        context.callDelegateSingle(m, nextMethod);
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        BytecodeGen.Context.ValuesMethodDefD nextMethod = context.newMultiMethod(this.next);
        context.callDelegateMulti(m, nextMethod);
        m.areturn(Type.VOID_TYPE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RootNode that = (RootNode) o;
        return Objects.equals(next, that.next);
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + next.hashCode();

        return result;
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RootNode that = (RootNode) o;
        return next.relaxedEquals(that.next);
    }

    @Override
    public int relaxedHashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + next.relaxedHashCode();

        return result;
    }
}
