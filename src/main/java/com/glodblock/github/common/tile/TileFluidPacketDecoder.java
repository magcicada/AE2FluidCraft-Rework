package com.glodblock.github.common.tile;

import appeng.api.AEApi;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.fluids.util.AEFluidStack;
import appeng.helpers.Reflected;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.tile.grid.AENetworkTile;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.glodblock.github.integration.mek.FCGasItems;
import com.glodblock.github.integration.mek.FakeGases;
import com.glodblock.github.loader.FCItems;
import com.glodblock.github.util.ModAndClassUtil;
import com.glodblock.github.util.Util;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import mekanism.api.gas.GasStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileFluidPacketDecoder extends AENetworkTile implements IGridTickable, IAEAppEngInventory {

    private final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 1);
    private final IActionSource ownActionSource = new MachineSource(this);

    @Reflected
    public TileFluidPacketDecoder() {
        getProxy().setIdlePowerUsage(1D);
        getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    public IItemHandlerModifiable getInventory() {
        return inventory;
    }

    @Override
    public boolean canBeRotated() {
        return false;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T)inventory;
        } else {
            return null;
        }
    }

    @Override
    @Nonnull
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(5, 120, false, true);
    }

    @Override
    @Nonnull
    @SuppressWarnings({"rawtypes", "unchecked"})
    public TickRateModulation tickingRequest(@Nonnull IGridNode node, int ticksSinceLastCall) {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.getItem() == FCItems.FLUID_PACKET) {
            FluidStack fluid = FakeItemRegister.getStack(stack);
            if (fluid == null || fluid.amount <= 0) {
                inventory.setStackInSlot(0, ItemStack.EMPTY);
                return TickRateModulation.SLEEP;
            }
            IAEFluidStack aeFluid = AEFluidStack.fromFluidStack(fluid);
            IEnergyGrid energyGrid = node.getGrid().getCache(IEnergyGrid.class);
            IMEMonitor<IAEFluidStack> fluidGrid = node.getGrid().<IStorageGrid>getCache(IStorageGrid.class)
                    .getInventory(Util.getFluidChannel());
            IAEFluidStack remaining = Platform.poweredInsert(energyGrid, fluidGrid, aeFluid, ownActionSource);
            if (remaining != null) {
                if (remaining.getStackSize() == aeFluid.getStackSize()) {
                    return TickRateModulation.SLOWER;
                }
                inventory.setStackInSlot(0, FakeFluids.packFluid2Packet(remaining.getFluidStack()));
                return TickRateModulation.FASTER;
            } else {
                inventory.setStackInSlot(0, ItemStack.EMPTY);
                return TickRateModulation.SLEEP;
            }
        } else if (ModAndClassUtil.GAS && stack.getItem() == FCGasItems.GAS_PACKET) {
            GasStack gas = FakeItemRegister.getStack(stack);
            if (gas == null || gas.getGas() == null || gas.amount <= 0) {
                inventory.setStackInSlot(0, ItemStack.EMPTY);
                return TickRateModulation.SLEEP;
            }
            IAEStack aeGas = AEGasStack.of(gas);
            IEnergyGrid energyGrid = node.getGrid().getCache(IEnergyGrid.class);
            IMEMonitor gasGrid = node.getGrid().<IStorageGrid>getCache(IStorageGrid.class)
                    .getInventory(AEApi.instance().storage().getStorageChannel(IGasStorageChannel.class));
            IAEStack remaining = Platform.poweredInsert(energyGrid, gasGrid, aeGas, ownActionSource);
            if (remaining != null) {
                if (remaining.getStackSize() == aeGas.getStackSize()) {
                    return TickRateModulation.SLOWER;
                }
                inventory.setStackInSlot(0, FakeGases.packGas2Packet(((IAEGasStack) remaining).getGasStack()));
                return TickRateModulation.FASTER;
            } else {
                inventory.setStackInSlot(0, ItemStack.EMPTY);
                return TickRateModulation.SLEEP;
            }
        }
        return TickRateModulation.SLEEP;
    }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation mc, ItemStack removedStack, ItemStack newStack) {
        try {
            getProxy().getTick().alertDevice(getProxy().getNode());
        } catch (GridAccessException e) {
            // NO-OP
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        inventory.writeToNBT(data, "Inventory");
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        inventory.readFromNBT(data, "Inventory");
    }

}
