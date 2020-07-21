package com.miki.morepcs;

import com.google.inject.Inject;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import com.pixelmonmod.pixelmon.comm.packetHandlers.OpenScreen;
import com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.pc.ClientChangeOpenPC;
import com.pixelmonmod.pixelmon.enums.EnumGuiScreen;
import com.pixelmonmod.pixelmon.sounds.PixelSounds;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.SoundCategory;
import org.slf4j.Logger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

@Plugin(
        id = "makestuffuseful",
        name = "MakeStuffUseful",
        description = "Pixelmon WorkPlaces and Bins made useful",
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
        logger.info("MakeStuffUseful " + "is booting up");
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
}
