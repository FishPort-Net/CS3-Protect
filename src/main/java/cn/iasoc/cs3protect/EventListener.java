package cn.iasoc.cs3protect;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.Acrobot.ChestShop.Events.PreTransactionEvent.TransactionOutcome.SPAM_CLICKING_PROTECTION;

public class EventListener implements Listener {
    private final Plugin plugin;

    public EventListener(Plugin plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrePurchaseEvent(PreTransactionEvent event) {
        if (event.getClient() == null) return;

        Player player = event.getClient();
        Block targetBlock = player.getTargetBlock(null, 5);

        RayTraceResult RaytraceResult = player.rayTraceBlocks(5);
        if (RaytraceResult == null || RaytraceResult.getHitBlock() == null) {
            player.sendMessage("§c[FPCSP] 您距离过远或未对准商店牌子!");
            event.setCancelled(SPAM_CLICKING_PROTECTION);
            return;
        };

        boolean isTargetBlockSign = (RaytraceResult.getHitBlock().getState() instanceof Sign);
        double distance = event.getSign().getLocation().distance(event.getClient().getLocation());

        // Prevent buying from a far distance or not targeting a sign
        if (!isTargetBlockSign || distance > 3.5) {
            // Send a message to the player
            player.sendMessage("§c[FPCSP] 您距离过远或未对准商店牌子!");
            String pos = "[" + player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ() + "]";
            this.plugin.getLogger().info("Player " + player.getName() + " tried to buy " + event.getSign().getLine(3) + " at  " + pos + " .");
            // Prevent spam clicking
            event.setCancelled(SPAM_CLICKING_PROTECTION);
        }
    }

    public boolean isContainer(Block block) {
        BlockData data = block.getBlockData();
        return block.getState() instanceof InventoryHolder; // 還是最穩妥
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        Material type = clicked.getType();

        // 只处理台阶和箱子
        if (!(type.toString().contains("STAIRS") || type.toString().contains("SLAB") || type.toString().contains("CARPET") || type.toString().contains("CHEST"))) {
            // 不是容器
            if (!isContainer(clicked)) return;
        }

        Location eye = player.getEyeLocation();

        // 使用事件自带的点击点（更准确，避免中心点误判，1.19+ 可用）
        Location target = event.getInteractionPoint();
        if (target == null) {
            target = clicked.getBoundingBox().getCenter().toLocation(player.getWorld()); // fallback
        }

        Vector direction = target.toVector().subtract(eye.toVector()).normalize();
        double distance = eye.distance(target);

        RayTraceResult result = player.getWorld().rayTraceBlocks(
                eye,
                direction,
                distance,
                FluidCollisionMode.NEVER,
                false // 不忽略透明方块，保证玻璃/门也算遮挡
        );

        if (result != null) {
            Block hit = result.getHitBlock();
            if (hit != null && !hit.equals(clicked)) {
                // 被别的方块挡住 → 禁止交互
                event.setCancelled(true);
            }
        }
    }

}
