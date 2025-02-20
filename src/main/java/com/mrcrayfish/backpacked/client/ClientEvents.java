package com.mrcrayfish.backpacked.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mrcrayfish.backpacked.Backpacked;
import com.mrcrayfish.backpacked.Config;
import com.mrcrayfish.backpacked.Reference;
import com.mrcrayfish.backpacked.client.gui.screen.inventory.BackpackScreen;
import com.mrcrayfish.backpacked.client.model.BackpackModel;
import com.mrcrayfish.backpacked.client.renderer.entity.layers.BackpackLayer;
import com.mrcrayfish.backpacked.common.BackpackModelProperty;
import com.mrcrayfish.backpacked.common.data.PickpocketChallenge;
import com.mrcrayfish.backpacked.integration.Curios;
import com.mrcrayfish.backpacked.inventory.ExtendedPlayerInventory;
import com.mrcrayfish.backpacked.network.Network;
import com.mrcrayfish.backpacked.network.message.MessageEntityBackpack;
import com.mrcrayfish.backpacked.network.message.MessageOpenBackpack;
import com.mrcrayfish.backpacked.util.PickpocketUtil;
import com.mrcrayfish.backpacked.util.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public class ClientEvents
{
    public static final ResourceLocation EMPTY_BACKPACK_SLOT = new ResourceLocation(Reference.MOD_ID, "item/empty_backpack_slot");
    private static ItemGroup currentGroup = null;

    @SubscribeEvent
    public void onPlayerLogin(ClientPlayerNetworkEvent.LoggedInEvent event)
    {
        Backpacked.updateBannedItemsList();
    }

    @SubscribeEvent
    public void onPlayerRenderScreen(GuiContainerEvent.DrawBackground event)
    {
        if(Backpacked.isCuriosLoaded())
            return;

        ContainerScreen<?> screen = event.getGuiContainer();
        if(screen instanceof InventoryScreen)
        {
            InventoryScreen inventoryScreen = (InventoryScreen) screen;
            int left = inventoryScreen.getGuiLeft();
            int top = inventoryScreen.getGuiTop();
            inventoryScreen.getMinecraft().getTextureManager().bind(ContainerScreen.INVENTORY_LOCATION);
            Screen.blit(event.getMatrixStack(), left + 76, top + 43, 7, 7, 18, 18, 256, 256);
        }
        else if(screen instanceof CreativeScreen)
        {
            CreativeScreen creativeScreen = (CreativeScreen) screen;
            if(creativeScreen.getSelectedTab() == ItemGroup.TAB_INVENTORY.getId())
            {
                int left = creativeScreen.getGuiLeft();
                int top = creativeScreen.getGuiTop();
                creativeScreen.getMinecraft().getTextureManager().bind(ContainerScreen.INVENTORY_LOCATION);
                Screen.blit(event.getMatrixStack(), left + 126, top + 19, 7, 7, 18, 18, 256, 256);
            }
        }
    }

    public static void onTextureStitch(TextureStitchEvent.Pre event)
    {
        if(event.getMap().location().equals(PlayerContainer.BLOCK_ATLAS))
        {
            event.addSprite(EMPTY_BACKPACK_SLOT);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.screen instanceof BackpackScreen)
        {
            if(event.getAction() == GLFW.GLFW_PRESS && event.getKey() == ClientHandler.KEY_BACKPACK.getKey().getValue())
            {
                minecraft.player.closeContainer();
            }
        }
        else if(minecraft.player != null && minecraft.screen == null)
        {
            ClientPlayerEntity player = minecraft.player;
            if(ClientHandler.KEY_BACKPACK.isDown() && ClientHandler.KEY_BACKPACK.consumeClick())
            {
                if(!Backpacked.getBackpackStack(player).isEmpty())
                {
                    Network.getPlayChannel().sendToServer(new MessageOpenBackpack());
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientTickEnd(TickEvent.ClientTickEvent event)
    {
        if(event.phase != TickEvent.Phase.END)
            return;

        Minecraft mc = Minecraft.getInstance();
        if(mc.level == null || mc.player == null)
            return;

        List<PlayerEntity> players = mc.level.getEntities(EntityType.PLAYER, mc.player.getBoundingBox().inflate(16F), player -> true);
        for(PlayerEntity player : players)
        {
            if(Backpacked.isCuriosLoaded() && !Curios.isBackpackVisible(player))
                continue;

            ItemStack stack = Backpacked.getBackpackStack(player);
            if(stack.isEmpty())
                continue;

            if(!canShowBackpackEffects(stack))
                continue;

            String modelName = stack.getOrCreateTag().getString("BackpackModel");
            BackpackModel model = BackpackLayer.getModel(modelName);
            model.tickForPlayer(PickpocketUtil.getBackpackBox(player, 1.0F).getCenter(), player);
        }
    }

    public static boolean canShowBackpackEffects(ItemStack stack)
    {
        CompoundNBT tag = stack.getOrCreateTag();
        if(tag.contains(BackpackModelProperty.SHOW_EFFECTS.getTagName(), Constants.NBT.TAG_BYTE))
        {
            return tag.getBoolean(BackpackModelProperty.SHOW_EFFECTS.getTagName());
        }
        return true;
    }

    @SubscribeEvent
    public void onRightClick(InputEvent.ClickInputEvent event)
    {
        if(event.isUseItem())
        {
            if(Config.SERVER.pickpocketBackpacks.get())
            {
                this.performBackpackRaytrace(event);
            }
        }
    }

    private void performBackpackRaytrace(InputEvent.ClickInputEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.level == null || mc.player == null || mc.gameMode == null)
            return;

        double range = Config.SERVER.pickpocketMaxReachDistance.get();
        List<LivingEntity> entities = new ArrayList<>();
        entities.addAll(mc.level.getEntities(EntityType.PLAYER, mc.player.getBoundingBox().inflate(range), player -> {
            return !Backpacked.getBackpackStack(player).isEmpty() && !player.equals(mc.player) && PickpocketUtil.canPickpocketEntity(player, mc.player);
        }));
        entities.addAll(mc.level.getEntities(EntityType.WANDERING_TRADER, mc.player.getBoundingBox().inflate(mc.gameMode.getPickRange()), entity -> {
            return PickpocketChallenge.get(entity).map(PickpocketChallenge::isBackpackEquipped).orElse(false) && PickpocketUtil.canPickpocketEntity(entity, mc.player, mc.gameMode.getPickRange());
        }));

        if(entities.isEmpty())
            return;

        Vector3d start = mc.player.getEyePosition(1.0F);
        Vector3d end = mc.player.getViewVector(1.0F).scale(mc.gameMode.getPickRange()).add(start);

        double closestDistance = Double.MAX_VALUE;
        LivingEntity hitEntity = null;
        for(LivingEntity entity : entities)
        {
            AxisAlignedBB box = PickpocketUtil.getBackpackBox(entity, 1.0F);
            Optional<Vector3d> optionalHitVec = box.clip(start, end);
            if(!optionalHitVec.isPresent())
                continue;

            double distance = start.distanceTo(optionalHitVec.get());
            if(distance < closestDistance)
            {
                closestDistance = distance;
                hitEntity = entity;
            }
        }

        if(hitEntity != null)
        {
            event.setCanceled(true);
            event.setSwingHand(false);
            if(PickpocketUtil.canSeeBackpack(hitEntity, mc.player))
            {
                Network.getPlayChannel().sendToServer(new MessageEntityBackpack(hitEntity.getId()));
                event.setSwingHand(true);
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLastEvent(RenderWorldLastEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if(!mc.getEntityRenderDispatcher().shouldRenderHitBoxes())
            return;

        if(!Config.SERVER.pickpocketBackpacks.get())
            return;

        MatrixStack stack = event.getMatrixStack();
        stack.pushPose();
        Vector3d view = mc.gameRenderer.getMainCamera().getPosition();
        stack.translate(-view.x(), -view.y, -view.z());
        IRenderTypeBuffer.Impl source = mc.renderBuffers().bufferSource();
        for(PlayerEntity player : mc.level.players())
        {
            if(Backpacked.getBackpackStack(player).isEmpty())
                continue;

            if(player.isLocalPlayer())
                continue;

            boolean inReach = PickpocketUtil.inReachOfBackpack(player, mc.player, Config.SERVER.pickpocketMaxReachDistance.get()) && PickpocketUtil.canSeeBackpack(player, mc.player);
            float boxRed = inReach ? 0.0F : 1.0F;
            float boxGreen = inReach ? 1.0F : 1.0F;
            float boxBlue = inReach ? 0.0F : 1.0F;
            IVertexBuilder builder = source.getBuffer(RenderType.lines());
            WorldRenderer.renderLineBox(stack, builder, PickpocketUtil.getBackpackBox(player, event.getPartialTicks()), boxRed, boxGreen, boxBlue, 1.0F);

            float bodyRotation = MathHelper.lerp(event.getPartialTicks(), player.yBodyRotO, player.yBodyRot);
            boolean inRange = PickpocketUtil.inRangeOfBackpack(player, mc.player);
            float lineRed = inRange ? 0.0F : 1.0F;
            float lineGreen = inRange ? 1.0F : 1.0F;
            float lineBlue = inRange ? 0.0F : 1.0F;
            Matrix4f matrix4f = stack.last().pose();
            Vector3d pos = player.getPosition(event.getPartialTicks());
            Vector3d start = Vector3d.directionFromRotation(0, bodyRotation + 180 - Config.SERVER.pickpocketMaxRangeAngle.get().floatValue()).scale(Config.SERVER.pickpocketMaxReachDistance.get());
            Vector3d end = Vector3d.directionFromRotation(0, bodyRotation - 180 + Config.SERVER.pickpocketMaxRangeAngle.get().floatValue()).scale(Config.SERVER.pickpocketMaxReachDistance.get());
            builder.vertex(matrix4f, (float) (pos.x + start.x),(float) (pos.y + start.y), (float) (pos.z + start.z)).color(lineRed, lineGreen, lineBlue, 1.0F).endVertex();
            builder.vertex(matrix4f, (float) pos.x,(float) pos.y, (float) pos.z).color(lineRed, lineGreen, lineBlue, 1.0F).endVertex();
            builder.vertex(matrix4f, (float) (pos.x + end.x),(float) (pos.y + end.y), (float) (pos.z + end.z)).color(lineRed, lineGreen, lineBlue, 1.0F).endVertex();
            builder.vertex(matrix4f, (float) pos.x,(float) pos.y, (float) pos.z).color(lineRed, lineGreen, lineBlue, 1.0F).endVertex();
        }
        source.endBatch(RenderType.lines());
        stack.popPose();
    }

    @SubscribeEvent
    public void onRenderTickStart(TickEvent.RenderTickEvent event)
    {
        if(event.phase != TickEvent.Phase.START)
            return;

        if(Backpacked.isCuriosLoaded())
            return;

        Minecraft mc = Minecraft.getInstance();
        if(!(mc.screen instanceof CreativeScreen)) {
            currentGroup = null;
            return;
        }

        CreativeScreen screen = (CreativeScreen) mc.screen;
        ItemGroup group = ItemGroup.TABS[screen.getSelectedTab()];
        if(currentGroup == null || currentGroup != group)
        {
            currentGroup = group;
            if(currentGroup == ItemGroup.TAB_INVENTORY)
            {
                List<Slot> slots = screen.getMenu().slots;
                slots.stream().filter(slot -> slot.container instanceof ExtendedPlayerInventory && slot.getSlotIndex() == 41).findFirst().ifPresent(slot -> {
                    ReflectionHelper.repositionSlot(slot, 127, 20);
                });
            }
        }
    }
}
