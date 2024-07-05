package com.glodblock.github.integration.jei;

import com.glodblock.github.integration.jei.interfaces.IngredientExtractor;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluidGas;
import mekanism.api.gas.GasStack;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.IIngredientType;

import java.util.Objects;
import java.util.stream.Stream;

public class ModMachHybridGasStackExtractor implements IngredientExtractor<GasStack> {

    private final IIngredientType<HybridFluidGas> ingTypeHybridFluid;

    ModMachHybridGasStackExtractor(IModRegistry registry) {
        ingTypeHybridFluid = Objects.requireNonNull(registry.getIngredientRegistry().getIngredientType(HybridFluidGas.class));
    }

    public Stream<WrappedIngredient<GasStack>> extract(IRecipeLayout recipeLayout) {
        return recipeLayout.getIngredientsGroup(ingTypeHybridFluid).getGuiIngredients().values().stream()
                .map(ing -> {
                    HybridFluidGas hf = ing.getDisplayedIngredient();
                    return new WrappedIngredient<>(hf != null ? hf.asGasStack() : null, ing.isInput());
                });
    }

}
