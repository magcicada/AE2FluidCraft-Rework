package com.glodblock.github.common.tile;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.events.MENetworkStorageEvent;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.helpers.Reflected;
import appeng.me.GridAccessException;
import appeng.me.cache.CraftingGridCache;
import appeng.me.helpers.MachineSource;
import appeng.me.storage.MEInventoryHandler;
import appeng.tile.grid.AENetworkTile;
import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.glodblock.github.util.Util;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class TileFluidDiscretizer extends AENetworkTile implements ICellContainer {

    private final FluidDiscretizingInventory fluidDropInv = new FluidDiscretizingInventory();
    private final FluidCraftingInventory fluidCraftInv = new FluidCraftingInventory();
    private final IActionSource ownActionSource = new MachineSource(this);
    private boolean prevActiveState = false;

    @Reflected
    public TileFluidDiscretizer() {
        getProxy().setIdlePowerUsage(3D);
        getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    @Override
    public boolean canBeRotated() {
        return false;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<IMEInventoryHandler> getCellArray(IStorageChannel<?> channel) {
        try {
            if (getProxy().isActive() && getProxy().getGrid().getMachines(this.getClass()).size() < 2) {
                if (channel == Util.getItemChannel()) {
                    return Collections.singletonList(fluidDropInv.invHandler);
                } else if (channel == Util.getFluidChannel()) {
                    return Collections.singletonList(fluidCraftInv.invHandler);
                }
            }
        } catch (GridAccessException e) {
            //NO-OP
        }
        return Collections.emptyList();
    }

    @Override
    public void saveChanges(@Nullable ICellInventory<?> cellInventory) {
        world.markChunkDirty(pos, this); // optimization, i guess?
    }

    @Override
    public void gridChanged() {
        IMEMonitor<IAEFluidStack> fluidGrid = getFluidGrid();
        if (fluidGrid != null) {
            fluidGrid.addListener(fluidDropInv, fluidGrid);
        }
    }

    @MENetworkEventSubscribe
    public void onPowerUpdate(MENetworkPowerStatusChange event) {
        updateState();
    }

    @MENetworkEventSubscribe
    public void onChannelUpdate(MENetworkChannelsChanged event) {
        updateState();
    }

    @MENetworkEventSubscribe
    public void onStorageUpdate(MENetworkStorageEvent event) {
        updateState();
    }

    private void updateState() {
        boolean isActive = getProxy().isActive();
        if (isActive != prevActiveState) {
            prevActiveState = isActive;
            try {
                getProxy().getGrid().postEvent(new MENetworkCellArrayUpdate());
            } catch (GridAccessException e) {
                // NO-OP
            }
        }
    }

    @Override
    public void blinkCell(int slot) {
        // NO-OP
    }

    @Nullable
    private IEnergyGrid getEnergyGrid() {
        try {
            return getProxy().getGrid().getCache(IEnergyGrid.class);
        } catch (GridAccessException e) {
            return null;
        }
    }

    @Nullable
    private IMEMonitor<IAEFluidStack> getFluidGrid() {
        try {
            return getProxy().getGrid().<IStorageGrid>getCache(IStorageGrid.class).getInventory(Util.getFluidChannel());
        } catch (GridAccessException e) {
            return null;
        }
    }

    private class FluidDiscretizingInventory implements IMEInventory<IAEItemStack>, IMEMonitorHandlerReceiver<IAEFluidStack> {

        private final MEInventoryHandler<IAEItemStack> invHandler = new MEInventoryHandler<>(this, getChannel());
        @Nullable
        private ObjectArrayList<IAEItemStack> itemCache = null;

        FluidDiscretizingInventory() {
            invHandler.setPriority(Integer.MAX_VALUE);
        }

        @SuppressWarnings("DuplicatedCode")
        @Nullable
        @Override
        public IAEItemStack extractItems(IAEItemStack request, Actionable mode, IActionSource src) {
            Object fluidStack = FakeItemRegister.getAEStack(request);
            if (!(fluidStack instanceof IAEFluidStack)) {
                return null;
            }
            IMEMonitor<IAEFluidStack> fluidGrid = getFluidGrid();
            if (fluidGrid == null) {
                return null;
            }
            IEnergyGrid energyGrid = getEnergyGrid();
            if (energyGrid == null) {
                return null;
            }
            return FakeFluids.packFluid2AEDrops(fluidGrid.extractItems((IAEFluidStack) fluidStack, mode, ownActionSource));
        }

        @SuppressWarnings("DuplicatedCode")
        @Nullable
        @Override
        public IAEItemStack injectItems(IAEItemStack input, Actionable type, IActionSource src) {
            Object fluidStack = FakeItemRegister.getAEStack(input);
            if (!(fluidStack instanceof IAEFluidStack)) {
                return input;
            }
            IMEMonitor<IAEFluidStack> fluidGrid = getFluidGrid();
            if (fluidGrid == null) {
                return input;
            }
            IEnergyGrid energyGrid = getEnergyGrid();
            if (energyGrid == null) {
                return input;
            }
            return FakeFluids.packFluid2AEDrops(fluidGrid.injectItems((IAEFluidStack) fluidStack, type, ownActionSource));
        }

        @Override
        public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
            if (itemCache == null) {
                itemCache = new ObjectArrayList<>();
                IMEMonitor<IAEFluidStack> fluidGrid = getFluidGrid();
                if (fluidGrid != null) {
                    for (IAEFluidStack fluid : fluidGrid.getStorageList()) {
                        IAEItemStack stack = FakeFluids.packFluid2AEDrops(fluid);
                        if (stack != null) {
                            itemCache.add(stack);
                        }
                    }
                }
            }
            for (IAEItemStack stack : itemCache) {
                out.addStorage(stack);
            }
            return out;
        }

        @Override
        public boolean isValid(Object verificationToken) {
            IMEMonitor<IAEFluidStack> fluidGrid = getFluidGrid();
            return fluidGrid != null && fluidGrid == verificationToken;
        }

        @Override
        public void postChange(IBaseMonitor<IAEFluidStack> monitor, Iterable<IAEFluidStack> change, IActionSource actionSource) {
            itemCache = null;
            try {
                ObjectArrayList<IAEItemStack> mappedChanges = new ObjectArrayList<>();
                for (IAEFluidStack fluidStack : change) {
                    boolean isNg = false;
                    if (fluidStack.getStackSize() < 0) {
                        isNg = true;
                        fluidStack.setStackSize(-fluidStack.getStackSize());
                    }
                    IAEItemStack itemStack = FakeFluids.packFluid2AEDrops(fluidStack);
                    if (itemStack != null) {
                        if (isNg) itemStack.setStackSize(-itemStack.getStackSize());
                        mappedChanges.add(itemStack);
                    }
                }
                getProxy().getGrid().<IStorageGrid>getCache(IStorageGrid.class).postAlterationOfStoredItems(getChannel(), mappedChanges, ownActionSource);
            } catch (GridAccessException e) {
                // NO-OP
            }
        }

        @Override
        public void onListUpdate() {
            // NO-OP
        }

        @Override
        public IStorageChannel<IAEItemStack> getChannel() {
            return Util.getItemChannel();
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private class FluidCraftingInventory implements IMEInventory {

        private final MEInventoryHandler invHandler = new MEInventoryHandler<>(this, this.getChannel());

        FluidCraftingInventory() {
            invHandler.setPriority(Integer.MAX_VALUE);
        }

        @Nullable
        @Override
        public IAEStack injectItems(IAEStack aeStack, Actionable type, IActionSource src) {
            if (!(aeStack instanceof IAEFluidStack)) {
                return null;
            }
            IAEFluidStack input = (IAEFluidStack) aeStack;
            ICraftingGrid craftingGrid;
            try {
                craftingGrid = getProxy().getGrid().getCache(ICraftingGrid.class);
            } catch (GridAccessException e) {
                return null;
            }
            if (craftingGrid instanceof CraftingGridCache) {
                IAEItemStack remaining = ((CraftingGridCache)craftingGrid).injectItems(FakeFluids.packFluid2AEDrops(input), type, ownActionSource);
                if (remaining != null) {
                    return FakeItemRegister.getAEStack(remaining);
                }
            }
            return null;
        }

        @Nullable
        @Override
        public IAEStack extractItems(IAEStack request, Actionable mode, IActionSource src) {
            return null;
        }

        @Override
        public IItemList getAvailableItems(IItemList out) {
            return out;
        }

        @Override
        public IStorageChannel<IAEFluidStack> getChannel() {
            return Util.getFluidChannel();
        }

    }

}
