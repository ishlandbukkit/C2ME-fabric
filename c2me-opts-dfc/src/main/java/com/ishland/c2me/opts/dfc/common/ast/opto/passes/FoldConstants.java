package com.ishland.c2me.opts.dfc.common.ast.opto.passes;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbsNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CubeNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.NegMulNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SquareNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqueezeNode;
import com.ishland.c2me.opts.dfc.common.util.ZeroUtils;
import net.minecraft.util.math.MathHelper;

public class FoldConstants implements AstTransformer {

    public static final FoldConstants INSTANCE = new FoldConstants();

    private FoldConstants() {
    }

    @Override
    public AstNode transform(AstNode astNode) {
        return switch (astNode) {
            case AddNode addNode -> {
                if (addNode.left instanceof ConstantNode c1 && addNode.right instanceof ConstantNode c2) {
                    yield new ConstantNode(c1.getValue() + c2.getValue());
                }

                // TreeNormalization: const left
                if (addNode.left instanceof ConstantNode c && c.getValue() == 0.0 && !ZeroUtils.isPositiveZero(c.getValue())) {
                    yield addNode.right;
                }

                yield addNode;
            }
            case MulNode mulNode -> {
                if (mulNode.left instanceof ConstantNode c1 && mulNode.right instanceof ConstantNode c2) {
                    yield new ConstantNode(c1.getValue() * c2.getValue());
                }

                if (mulNode.left instanceof ConstantNode c && c.getValue() == 0.0) { // special case defined in vanilla
                    yield new ConstantNode(0.0);
                }

                // TreeNormalization: const left
                if (mulNode.left instanceof ConstantNode c && c.getValue() == 1.0) {
                    yield mulNode.right;
                }

                yield mulNode;
            }
            case MaxNode maxNode -> {
                if (maxNode.left instanceof ConstantNode c1 && maxNode.right instanceof ConstantNode c2) {
                    yield new ConstantNode(Math.max(c1.getValue(), c2.getValue()));
                }

                yield maxNode;
            }
            case MaxShortNode maxShortNode -> {
                if (maxShortNode.left instanceof ConstantNode c1 && maxShortNode.right instanceof ConstantNode c2) {
                    yield new ConstantNode(Math.max(c1.getValue(), c2.getValue()));
                }

                if (maxShortNode.left instanceof ConstantNode c1 && c1.getValue() >= maxShortNode.rightMax) {
                    yield c1;
                }

                yield maxShortNode;
            }
            case MinNode minNode -> {
                if (minNode.left instanceof ConstantNode c1 && minNode.right instanceof ConstantNode c2) {
                    yield new ConstantNode(Math.min(c1.getValue(), c2.getValue()));
                }

                yield minNode;
            }
            case MinShortNode minShortNode -> {
                if (minShortNode.left instanceof ConstantNode c1 && minShortNode.right instanceof ConstantNode c2) {
                    yield new ConstantNode(Math.min(c1.getValue(), c2.getValue()));
                }

                if (minShortNode.left instanceof ConstantNode c1 && c1.getValue() <= minShortNode.rightMin) {
                    yield c1;
                }

                yield minShortNode;
            }
            case AbsNode absNode -> {
                if (absNode.operand instanceof ConstantNode c) {
                    yield new ConstantNode(Math.abs(c.getValue()));
                }

                yield absNode;
            }
            case CubeNode cubeNode -> {
                if (cubeNode.operand instanceof ConstantNode c) {
                    yield new ConstantNode(c.getValue() * c.getValue() * c.getValue());
                }

                yield cubeNode;
            }
            case NegMulNode negMulNode -> {
                if (negMulNode.operand instanceof ConstantNode c) {
                    yield new ConstantNode(c.getValue() > 0.0 ? c.getValue() : c.getValue() * negMulNode.negMul);
                }

                yield negMulNode;
            }
            case SquareNode squareNode -> {
                if (squareNode.operand instanceof ConstantNode c) {
                    yield new ConstantNode(c.getValue() * c.getValue());
                }

                yield squareNode;
            }
            case SqueezeNode squeezeNode -> {
                if (squeezeNode.operand instanceof ConstantNode c) {
                    double v = MathHelper.clamp(c.getValue(), -1.0, 1.0);
                    yield new ConstantNode(v / 2.0 - v * v * v / 24.0);
                }

                yield squeezeNode;
            }
            case CacheLikeNode cacheLikeNode -> {
                if (cacheLikeNode.getCacheLike().c2me$isActualCache() && cacheLikeNode.getDelegate() instanceof ConstantNode c) {
                    yield c;
                }

                yield cacheLikeNode;
            }
            case null -> throw new NullPointerException();
            default -> astNode;
        };
    }

}
