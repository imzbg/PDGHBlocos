package me.zbg.PDGHBlocos;
import org.bukkit.plugin.java.*;
import org.bukkit.block.*;
import net.milkbowl.vault.permission.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import java.util.*;
import com.sk89q.worldguard.bukkit.*;
import org.bukkit.plugin.*;
import java.io.*;
import org.bukkit.event.block.*;
import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.block.BlockPhysicsEvent;

public class Main extends JavaPlugin implements CommandExecutor, Listener {
    private List<BlockState> bs;
    static boolean Ativar;
    static String Mapa;
    static boolean needTool;
    static List<Integer> IDFerramentas;
    static List<Integer> IDBlocosQuebraveis;
    static int TempoReset;
    private String base;
    public static Permission permission;

    static {
        Main.permission = null;
    }

    public Main() {
        this.bs = new ArrayList<BlockState>();
    }

    private boolean setupPermissions() {
        final RegisteredServiceProvider<Permission> permissionProvider = (RegisteredServiceProvider<Permission>) this.getServer()
                .getServicesManager().getRegistration((Class) Permission.class);
        if (permissionProvider != null) {
            Main.permission = (Permission) permissionProvider.getProvider();
        }
        return Main.permission != null;
    }

    public boolean onCommand(final CommandSender s, final Command cmd, final String String, final String[] args) {
        if (cmd.getName().equalsIgnoreCase("pdghblocos")) {
            if (args.length == 0) {
                if (s.hasPermission("pdgh.diretor")) {
                    this.Config();
                    s.sendMessage("§3[PDGHBlocos] §aConfiguração Recarregada");
                } else {
                    s.sendMessage("§3[PDGHBlocos] §cSem Permissões");
                }
            } else if (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("replace")) {
                if (s.hasPermission("pdgh.diretor")) {
                    try {
                        if (!this.bs.isEmpty()) {
                            for (final BlockState block : this.bs) {
                                block.getLocation().getBlock().setType(block.getType());
                            }
                            s.sendMessage("§3[PDGHBlocos] §aVocê recolocou blocos quebrados");
                            this.bs.clear();
                        } else {
                            s.sendMessage("§3[PDGHBlocos] §cSem blocos quebrados para serem recolocados");
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                } else {
                    s.sendMessage("§3[PDGHBlocos] §cSem permissão para recarregar blocos");
                }
            }
        }
        return false;
    }

    public void Config() {
        this.reloadConfig();
        Main.Ativar = this.getConfig().getBoolean("Ativar");
        Main.Mapa = this.getConfig().getString("Mapa");
        Main.needTool = this.getConfig().getBoolean("PrecisaDeFerramenta");
        Main.IDFerramentas = (List<Integer>) this.getConfig().getIntegerList("IDFerramentas");
        Main.IDBlocosQuebraveis = (List<Integer>) this.getConfig().getIntegerList("IDBlocosQuebraveis");
        Main.TempoReset = this.getConfig().getInt("TempoReset");
    }

    private WorldGuardPlugin getWorldGuard() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("WorldGuard");
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null;
        }
        return (WorldGuardPlugin) plugin;
    }

    @Override
    public void onEnable() {
        if (!Bukkit.getPluginManager().getPlugin("WorldGuard").isEnabled()) {
            Bukkit.getPluginManager().disablePlugin((Plugin) this);
            Bukkit.getConsoleSender().sendMessage(ChatColor.WHITE + "[PDGHBlocos] World Guard não localizado, desabilitando plugin.");
        }
        final File file = new File(this.getDataFolder(), "config.yml");
        if (!file.exists()) {
            this.saveResource("config.yml", false);
            this.Config();
        }
        this.setupPermissions();
        this.Config();
        Bukkit.getPluginCommand("pdghblocos").setExecutor((CommandExecutor) this);
        Bukkit.getPluginManager().registerEvents((Listener) this, (Plugin) this);
        Bukkit.getConsoleSender().sendMessage(ChatColor.WHITE + "[PDGHBlocos] Carregado com sucesso.");
        Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin) this, (Runnable) new Runnable() {
            @Override
            public void run() {
                if (!Main.this.bs.isEmpty()) {
                    for (final BlockState block : Main.this.bs) {
                        block.getLocation().getBlock().setType(block.getType());
                    }
                    Main.this.bs.clear();
                }
            }
        }, 20L, (long) (Main.TempoReset * 60 * 20));
    }

    @Override
    public void onDisable() {
        for (final BlockState block : this.bs) {
            block.getLocation().getBlock().setType(block.getType());
        }
    }

    @EventHandler
    public void BreakBlock(Block block) {
        if (Main.IDBlocosQuebraveis.contains(block.getTypeId())) {
            block.setType(Material.AIR);
        }
    }

    @EventHandler
    public void onArrowHitBlock(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            if (arrow.getShooter() instanceof Player) {
                Player player = (Player) arrow.getShooter();
                Block hitBlock = arrow.getLocation().getBlock();

                if (Main.IDBlocosQuebraveis.contains(hitBlock.getTypeId())) {
                    BreakBlock(hitBlock);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (Main.IDBlocosQuebraveis.contains(e.getBlock().getTypeId())) {
            final String mapa = e.getPlayer().getWorld().getName().toString();
            final GameMode gm = e.getPlayer().getGameMode();
            final BlockState block = e.getBlock().getState();
            try {
                if (mapa.equalsIgnoreCase(Main.Mapa) && gm != GameMode.CREATIVE) {
                    if (Main.needTool) {
                        if (Main.IDFerramentas.contains(e.getPlayer().getItemInHand().getTypeId())) {
                            if (!this.getWorldGuard().canBuild(e.getPlayer(), e.getBlock().getLocation())) {
                                e.setCancelled(true);
                                BreakBlock(e.getBlock());
                                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.SUCCESSFUL_HIT, 3.0f, 4.0f);
                                this.bs.add(block);
                            } else {
                                e.setCancelled(true);
                                BreakBlock(e.getBlock());
                                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.SUCCESSFUL_HIT, 3.0f, 4.0f);
                                this.bs.add(block);
                            }
                        } else {
                            e.setCancelled(true);
                        }
                    } else if (!this.getWorldGuard().canBuild(e.getPlayer(), e.getBlock().getLocation())) {
                        e.setCancelled(true);
                        BreakBlock(e.getBlock());
                        e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.SUCCESSFUL_HIT, 3.0f, 4.0f);
                        this.bs.add(block);
                    } else {
                        e.setCancelled(true);
                        BreakBlock(e.getBlock());
                        e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.SUCCESSFUL_HIT, 3.0f, 4.0f);
                        this.bs.add(block);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } else {
            final String mapa = e.getPlayer().getWorld().getName().toString();
            try {
                if (mapa.equalsIgnoreCase(Main.Mapa)) {
                    final GameMode g = e.getPlayer().getGameMode();
                    if (!Main.IDBlocosQuebraveis.contains(e.getBlock().getTypeId())) {
                        if (g == GameMode.CREATIVE) {
                            e.setCancelled(false);
                        } else {
                            e.setCancelled(true);
                        }
                    }
                }
            } catch (Exception e3) {
                e3.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (Main.IDBlocosQuebraveis.contains(block.getTypeId())) {
            event.setCancelled(true);
        }
    }
}
