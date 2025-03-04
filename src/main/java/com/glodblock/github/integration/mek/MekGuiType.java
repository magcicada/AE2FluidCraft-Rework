package com.glodblock.github.integration.mek;

import appeng.fluids.helper.IFluidInterfaceHost;
import appeng.helpers.IInterfaceHost;
import com.glodblock.github.client.GuiFluidTrioInterface;
import com.glodblock.github.client.GuiGasTrioInterface;
import com.glodblock.github.client.GuiItemTrioInterface;
import com.glodblock.github.client.container.ContainerFluidDualInterface;
import com.glodblock.github.client.container.ContainerGasTrioInterface;
import com.glodblock.github.client.container.ContainerItemDualInterface;
import com.glodblock.github.inventory.GuiType;
import com.mekeng.github.common.me.duality.IGasInterfaceHost;
import net.minecraft.entity.player.EntityPlayer;

public class MekGuiType {

    public static GuiType.PartOrTileGuiFactory<IInterfaceHost> TRIO_ITEM_GUI() {
        return new GuiType.PartOrTileGuiFactory<IInterfaceHost>(IInterfaceHost.class) {

            @Override
            protected Object createServerGui(EntityPlayer player, IInterfaceHost inv) {
                return new ContainerItemDualInterface(player.inventory, inv);
            }

            @Override
            protected Object createClientGui(EntityPlayer player, IInterfaceHost inv) {
                return new GuiItemTrioInterface(player.inventory, inv);
            }
        };
    }

    public static GuiType.PartOrTileGuiFactory<IFluidInterfaceHost> TRIO_FLUID_GUI() {
        return new GuiType.PartOrTileGuiFactory<IFluidInterfaceHost>(IFluidInterfaceHost.class) {

            @Override
            protected Object createServerGui(EntityPlayer player, IFluidInterfaceHost inv) {
                return new ContainerFluidDualInterface(player.inventory, inv);
            }

            @Override
            protected Object createClientGui(EntityPlayer player, IFluidInterfaceHost inv) {
                return new GuiFluidTrioInterface(player.inventory, inv);
            }
        };
    }

    public static GuiType.PartOrTileGuiFactory<IGasInterfaceHost> TRIO_GAS_GUI() {
        return new GuiType.PartOrTileGuiFactory<IGasInterfaceHost>(IGasInterfaceHost.class) {

            @Override
            protected Object createServerGui(EntityPlayer player, IGasInterfaceHost inv) {
                return new ContainerGasTrioInterface(player.inventory, inv);
            }

            @Override
            protected Object createClientGui(EntityPlayer player, IGasInterfaceHost inv) {
                return new GuiGasTrioInterface(player.inventory, inv);
            }
        };
    }

}
