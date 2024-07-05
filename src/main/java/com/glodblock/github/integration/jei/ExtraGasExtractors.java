package com.glodblock.github.integration.jei;

import com.glodblock.github.integration.jei.interfaces.IngredientExtractor;
import mekanism.api.gas.GasStack;
import mezz.jei.api.gui.IRecipeLayout;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public class ExtraGasExtractors {

    @Nullable
    private final IngredientExtractor<GasStack> extModMach;

    public ExtraGasExtractors(@Nullable IngredientExtractor<GasStack> extModMach) {
        this.extModMach = extModMach;
    }

    public Stream<WrappedIngredient<GasStack>> extractGases(IRecipeLayout recipeLayout) {
        return extModMach != null ? extModMach.extract(recipeLayout) : Stream.empty();
    }

}
