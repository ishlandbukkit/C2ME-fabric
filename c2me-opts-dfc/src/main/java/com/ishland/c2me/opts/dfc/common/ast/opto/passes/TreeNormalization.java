package com.ishland.c2me.opts.dfc.common.ast.opto.passes;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.binary.AbstractBinaryNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;

public class TreeNormalization implements AstTransformer {

    public static final TreeNormalization INSTANCE = new TreeNormalization();

    private TreeNormalization() {
    }

    @Override
    public AstNode transform(AstNode astNode) {
        if (astNode instanceof AbstractBinaryNode binaryNode && binaryNode.canSwapOperandsSafely()) {
            if (binaryNode.right instanceof ConstantNode && !(binaryNode.left instanceof ConstantNode)) {
                // fp add, mul, max, min are commutative
                return binaryNode.swapOperands();
            }
        }

        return astNode;
    }
}
