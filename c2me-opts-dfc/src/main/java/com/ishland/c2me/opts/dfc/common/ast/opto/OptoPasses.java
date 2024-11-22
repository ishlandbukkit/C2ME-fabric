package com.ishland.c2me.opts.dfc.common.ast.opto;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.opto.passes.FoldConstants;

public class OptoPasses {

    private static final AstTransformer[] PASSES = new AstTransformer[] {
            FoldConstants.INSTANCE
    };

    public static AstNode optimize(AstNode astNode) {
        AstNode res = astNode;
        do {
            astNode = res;
            for (AstTransformer pass : PASSES) {
                res = res.transform(pass);
            }
        } while (res != astNode);
        return res;
    }

}
