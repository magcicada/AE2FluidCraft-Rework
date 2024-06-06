package com.glodblock.github.common.tile;

import com.glodblock.github.common.component.DualityDualInterface;
import com.glodblock.github.common.component.DualityTrioInterface;
import com.glodblock.github.integration.mek.FCGasBlocks;
import com.glodblock.github.inventory.GuiType;
import com.mekeng.github.common.me.duality.IGasInterfaceHost;
import com.mekeng.github.common.me.duality.impl.DualityGasInterface;
import net.minecraft.item.ItemStack;

public class TileTrioInterface extends TileDualInterface implements IGasInterfaceHost {

    public TileTrioInterface() {
        super();
    }

    @SuppressWarnings("rawtypes")
    protected DualityDualInterface createDuality() {
        return new DualityTrioInterface<>(getProxy(), this);
    }

    @Override
    public DualityGasInterface getDualityGasInterface() {
        return ((DualityTrioInterface<?>) this.duality).getGasInterface();
    }

    @Override
    public GuiType getGuiType() {
        return GuiType.TRIO_ITEM_INTERFACE;
    }

    @Override
    public ItemStack getItemStackRepresentation() {
        return new ItemStack(FCGasBlocks.TRIO_INTERFACE);
    }

}
