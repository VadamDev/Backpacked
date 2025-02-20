package com.mrcrayfish.backpacked.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mrcrayfish.backpacked.common.Backpack;
import com.mrcrayfish.backpacked.common.BackpackManager;
import com.mrcrayfish.backpacked.common.command.arguments.BackpackArgument;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

/**
 * Author: MrCrayfish
 */
public class UnlockBackpackCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(Commands.literal("unlockbackpack").requires(source -> {
            return source.hasPermission(2);
        }).then(Commands.argument("backpack", BackpackArgument.backpacks()).executes(context -> {
            Backpack backpack = context.getArgument("backpack", Backpack.class);
            BackpackManager.instance().unlockBackpack(context.getSource().getPlayerOrException(), backpack.getId());
            return 1;
        })));
    }
}
