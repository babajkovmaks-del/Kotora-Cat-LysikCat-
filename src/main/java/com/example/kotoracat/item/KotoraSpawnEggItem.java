package com.example.kotoracat.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.List;
import java.util.function.Supplier;

public class KotoraSpawnEggItem extends ForgeSpawnEggItem {
    public KotoraSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight, Properties properties) {
        super(type, background, highlight, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.literal("Wither Storm от Kotora"));
        lines.add(Component.literal("Добрый 50% • Злой 50%"));
        lines.add(Component.literal("ПКМ пустой рукой — приручить сразу"));
        lines.add(Component.literal("/kotoracat tame — ближайший"));
        lines.add(Component.literal("/kotoracat tameall — все рядом"));
    }
}
