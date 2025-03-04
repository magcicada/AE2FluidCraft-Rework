package com.glodblock.github.common.block;

import appeng.util.Platform;
import com.glodblock.github.common.tile.TileTrioInterface;
import com.glodblock.github.inventory.GuiType;
import com.glodblock.github.inventory.InventoryHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockTrioInterface extends BlockDualInterface {

    public BlockTrioInterface() {
        super();
        this.setTileEntity(TileTrioInterface.class);
    }

    @Override
    public boolean onActivated(final World w, final BlockPos pos, final EntityPlayer p,
                               final EnumHand hand, final @Nullable ItemStack heldItem, final EnumFacing side,
                               final float hitX, final float hitY, final float hitZ) {
        if (p.isSneaking()) {
            return false;
        }
        final TileTrioInterface tg = this.getTileEntity(w, pos);
        if (tg != null) {
            if (Platform.isServer()) {
                InventoryHandler.openGui(p, w, pos, side, GuiType.TRIO_ITEM_INTERFACE);
            }
            return true;
        }
        return false;
    }

}
