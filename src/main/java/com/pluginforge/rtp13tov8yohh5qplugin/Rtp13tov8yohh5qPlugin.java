package com.pluginforge.rtp13tov8yohh5qplugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class Rtp13tov8yohh5qPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        RtpCommand command = new RtpCommand(this);
        if (getCommand("rtp") != null) {
            getCommand("rtp").setExecutor(command);
            getCommand("rtp").setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(command, this);
        getLogger().info("Rtp13tov8yohh5qPlugin enabled.");
    }
}
