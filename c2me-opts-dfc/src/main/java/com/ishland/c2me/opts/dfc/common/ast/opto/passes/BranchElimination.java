package com.ishland.c2me.opts.dfc.common.ast.opto.passes;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;

public class BranchElimination implements AstTransformer {

    public static final BranchElimination INSTANCE = new BranchElimination();

    private BranchElimination() {
    }

    @Override
    public AstNode transform(AstNode astNode) {
        return switch (astNode) {
            case RangeChoiceNode rangeChoiceNode -> {
                if (rangeChoiceNode.input instanceof ConstantNode c) {
                    if (c.getValue() >= rangeChoiceNode.minInclusive && c.getValue() < rangeChoiceNode.maxExclusive) {
                        yield rangeChoiceNode.whenInRange;
                    } else {
                        yield rangeChoiceNode.whenOutOfRange;
                    }
                }

                if (rangeChoiceNode.whenInRange.equals(rangeChoiceNode.whenOutOfRange)) {
                    yield rangeChoiceNode.whenInRange;
                }

                yield rangeChoiceNode;
            }
            default -> astNode;
        };
    }

}
