package net.taunahi.ezskyblockscripts.mixin.block;

import com.google.common.collect.Sets;
import net.taunahi.ezskyblockscripts.config.TaunahiConfig;
import net.taunahi.ezskyblockscripts.handler.MacroHandler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(BlockRendererDispatcher.class)
public class MixinBlockRendererDispatcher {

    @Unique
    private static final Set<Block> cropBlocks = Sets.newHashSet(
            Blocks.cocoa,
            Blocks.reeds,
            Blocks.nether_wart,
            Blocks.wheat,
            Blocks.carrots,
            Blocks.potatoes,
            Blocks.pumpkin,
            Blocks.pumpkin_stem,
            Blocks.melon_block,
            Blocks.melon_stem,
            Blocks.brown_mushroom,
            Blocks.red_mushroom,
            Blocks.cactus
    );

    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void renderBlock(IBlockState state, BlockPos pos, IBlockAccess blockAccess, WorldRenderer worldRendererIn, CallbackInfoReturnable<Boolean> cir) {
        if (TaunahiConfig.performanceMode && (MacroHandler.getInstance().isMacroToggled()) && cropBlocks.contains(state.getBlock())) {
            cir.setReturnValue(false);
        }
    }
}
