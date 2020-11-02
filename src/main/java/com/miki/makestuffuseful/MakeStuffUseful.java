package com.miki.makestuffuseful;

import com.google.inject.Inject;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import com.pixelmonmod.pixelmon.api.world.WorldTime;
import com.pixelmonmod.pixelmon.comm.packetHandlers.OpenScreen;
import com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.pc.ClientChangeOpenPC;
import com.pixelmonmod.pixelmon.entities.bikes.EntityBike;
import com.pixelmonmod.pixelmon.enums.EnumGuiScreen;
import com.pixelmonmod.pixelmon.sounds.PixelSounds;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.SoundCategory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

@Plugin(
        id = "makestuffuseful",
        name = "MakeStuffUseful",
        description = "Pixelmon WorkPlaces, Bins, Clocks, Bikes, and Vanilla Clocks made useful",
        authors = {
                "02Miki"
        }
)
public class MakeStuffUseful {
    public static MakeStuffUseful instance;

    @Inject
    private Logger logger;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        instance = this;
        logger.info("MakeStuffUseful is booting up");
    }

    @Listener
    public void onBlockInteract(InteractBlockEvent.Secondary e) {
        String b = e.getTargetBlock().getExtendedState().getId();
        Player p = (Player) e.getSource();
        EntityPlayerMP pl = (EntityPlayerMP) e.getSource();
        if (b.contains("pixelmon:darkworkplace") || b.contains("pixelmon:lightworkplace")) {
            PCStorage pc = Pixelmon.storageManager.getPC(pl, null);
            Pixelmon.network.sendTo(new ClientChangeOpenPC(pc.uuid), pl);
            OpenScreen.open(pl, EnumGuiScreen.PC, EnumGuiScreen.PC.getIndex());
            pl.getEntityWorld().playSound(null, pl.posX, pl.posY, pl.posZ, PixelSounds.pc, SoundCategory.BLOCKS, 0.7F, 1.0F);
            e.setCancelled(true);
        } else if (b.contains("pixelmon:trash_can")) {
            p.openInventory(Inventory.builder().of(InventoryArchetypes.DOUBLE_CHEST)
                            .property(InventoryTitle.PROPERTY_NAME, new InventoryTitle(Text.of("Trash Can")))
                            .build(instance));
            e.setCancelled(true);
        }

    }
    @Listener
    public void onItemDrop(CollideBlockEvent e) {
        if (e.getCause().containsType(EntityItem.class) && (e.getTargetBlock().toString().equalsIgnoreCase("pixelmon:trash_can") || e.getTargetLocation().add(0, 1, 0).getBlock().getId().equalsIgnoreCase("pixelmon:trash_can"))) {
            EntityItem i = e.getCause().first(EntityItem.class).get();
            i.setDead();
        }
    }
    @Listener
    public void onBikeSpawn(SpawnEntityEvent e, @Root EntityPlayerMP playerMP) {
        if (e.getEntities().stream().noneMatch(entity -> entity instanceof EntityBike)) {
            return;
        }
        EntityBike bike = (EntityBike) e.getEntities().stream().filter(entity -> entity instanceof EntityBike).findFirst().get();
        Player player = (Player) playerMP;
        bike.getEntityData().setString("owner-id", player.getUniqueId().toString());
    }
    @Listener
    public void onBikeRemove(InteractEntityEvent.Primary e, @Root EntityPlayerMP playerMP) {
        if (!(e.getTargetEntity() instanceof EntityBike)) {
            return;
        }
        EntityBike bike = (EntityBike) e.getTargetEntity();
        Player player = (Player) playerMP;
        if (playerMP.isSneaking() && player.hasPermission("makestuffuseful.bikes.admin")) {
            return;
        }
        if (!bike.getEntityData().getString("owner-id").equalsIgnoreCase(player.getUniqueId().toString()) && player.hasPermission("makestuffuseful.bikes.admin")) {
            e.setCancelled(true);
            player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize("&cI'm sorry, this is not your bike! You can bypass this, by shifting"));
            return;
        }
        if (!bike.getEntityData().getString("owner-id").equalsIgnoreCase(player.getUniqueId().toString())) {
            e.setCancelled(true);
            player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize("&cI'm sorry, this is not your bike!"));
        }
    }
    @Listener
    public void onClockBlockClick(InteractBlockEvent.Secondary.MainHand e, @Root EntityPlayerMP playerMP) {
        Player player = (Player) playerMP;
        String b = e.getTargetBlock().getExtendedState().getId();
        if (b.contains("clock")) {
            player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize("&8[&3Clock&8] &aIt's currently " + getTimeString(playerMP) + "&a!"));
        }
    }
    @Listener
    public void onClockItemClick(InteractItemEvent.Secondary e, @Root EntityPlayerMP playerMP) {
        Player player = (Player) playerMP;
        ItemType itemType = e.getItemStack().getType();
        if (itemType.equals(ItemTypes.CLOCK)) {
            player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize("&8[&3Clock&8] &aIt's currently " + getTimeString(playerMP) + "&a!"));
            e.setCancelled(true);
        }
    }

    public String getTimeString(EntityPlayerMP playerMP) {
        return WorldTime.getCurrent(playerMP.world).toString()
                .replace("[", "")
                .replace("]", "")
                .replace("NIGHT", "&3Night")
                .replace("MORNING", "&6Morning")
                .replace("DUSK", "&5Dusk")
                .replace("DAWN", "&5Dawn")
                .replace("MIDDAY", "&fMidday")
                .replace("DAY", "&eDay")
                .replace("MIDNIGHT", "&9Midnight")
                .replace("AFTERNOON", "&6Afternoon")
                .replace(",", " &aand");

    }


}
