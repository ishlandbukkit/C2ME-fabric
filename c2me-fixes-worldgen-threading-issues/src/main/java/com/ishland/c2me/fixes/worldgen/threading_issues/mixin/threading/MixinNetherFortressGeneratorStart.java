package com.ishland.c2me.fixes.worldgen.threading_issues.mixin.threading;

import com.ishland.c2me.fixes.worldgen.threading_issues.asm.MakeVolatile;
import com.ishland.c2me.fixes.worldgen.threading_issues.common.XPieceDataExtension;
import net.minecraft.structure.NetherFortressGenerator;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(NetherFortressGenerator.Start.class)
public class MixinNetherFortressGeneratorStart {

    @Shadow public List<NetherFortressGenerator.PieceData> bridgePieces;
    @Shadow public List<NetherFortressGenerator.PieceData> corridorPieces;

    @MakeVolatile
    @Shadow public NetherFortressGenerator.PieceData lastPiece;

    @Redirect(method = "<init>(Lnet/minecraft/util/math/random/Random;II)V", at = @At(value = "FIELD", target = "Lnet/minecraft/structure/NetherFortressGenerator$PieceData;generatedCount:I", opcode = Opcodes.PUTFIELD), require = 2)
    private void redirectSetPieceDataGeneratedCount(NetherFortressGenerator.PieceData pieceData, int value) {
        if (value == 0) {
            ((XPieceDataExtension) pieceData).c2me$getGeneratedCountThreadLocal().remove();
        } else {
            ((XPieceDataExtension) pieceData).c2me$getGeneratedCountThreadLocal().set(value);
        }
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.bridgePieces = Collections.synchronizedList(this.bridgePieces);
        this.corridorPieces = Collections.synchronizedList(this.corridorPieces);
    }
}
