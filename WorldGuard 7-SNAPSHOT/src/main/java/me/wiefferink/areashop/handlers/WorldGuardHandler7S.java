package me.wiefferink.areashop.handlers;

import me.wiefferink.areashop.interfaces.AreaShopInterface;

public class WorldGuardHandler7S extends WorldGuardHandler7 {

	public WorldGuardHandler7S(AreaShopInterface pluginInterface) {
		super(pluginInterface);
	}

//	@Override
//	public RegionManager getRegionManager(World world) {
//		return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
//	}
//
//	@Override
//	public Flag<?> fuzzyMatchFlag(String flagName) {
//		return Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
//	}
//
//	@Override
//	public <V> V parseFlagInput(Flag<V> flag, String input) throws InvalidFlagFormat {
//		return flag.parseInput(FlagContext.create().setInput(input).build());
//	}
//
//	@Override
//	public RegionGroup parseFlagGroupInput(RegionGroupFlag flag, String input) throws InvalidFlagFormat {
//		return flag.parseInput(FlagContext.create().setInput(input).build());
//	}
}
