package no.vestlandetmc.BanFromClaim.listener;

import java.util.UUID;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import no.vestlandetmc.BanFromClaim.BfcPlugin;
import no.vestlandetmc.BanFromClaim.config.ClaimData;
import no.vestlandetmc.BanFromClaim.config.Config;
import no.vestlandetmc.BanFromClaim.handler.LocationFinder;
import no.vestlandetmc.BanFromClaim.handler.MessageHandler;

public class GPListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onPlayerEnterClaim(PlayerMoveEvent e) {
		if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
			return;
		}

		final Location locFrom = e.getFrom();
		final Location locTo = e.getTo();

		final ClaimData claimData = new ClaimData();
		final Player player = e.getPlayer();
		final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(locTo, true, null);

		if(player.hasPermission("bfc.bypass") || player.getGameMode().equals(GameMode.SPECTATOR)) { return; }

		if(claim != null) {
			final UUID ownerUUID =  claim.ownerID;
			final String claimID = claim.getID().toString();
			boolean hasAttacked = false;

			if(CombatMode.attackerContains(player.getUniqueId()))
				hasAttacked = CombatMode.getAttacker(player.getUniqueId()).equals(ownerUUID);

			if((claimData.isAllBanned(claimID) || playerBanned(player, claimID)) && !hasAttacked && !hasTrust(player, claim)) {
				if(claim.contains(locFrom, true, false)) {
					if(playerBanned(player, claimID) || claimData.isAllBanned(claimID)) {
						final int sizeRadius = Math.max(claim.getHeight(), claim.getWidth());

						final LocationFinder lf = new LocationFinder(claim.getGreaterBoundaryCorner(), claim.getLesserBoundaryCorner(), player.getWorld().getUID(), sizeRadius);
						Bukkit.getScheduler().runTaskAsynchronously(BfcPlugin.getInstance(), () -> lf.IterateCircumferences(randomCircumferenceRadiusLoc -> {
							if(randomCircumferenceRadiusLoc == null) {
								if(Config.SAFE_LOCATION == null) { player.teleport(player.getWorld().getSpawnLocation()); }
								else { player.teleport(Config.SAFE_LOCATION); }
							}
							else { player.teleport(randomCircumferenceRadiusLoc);	}

						}));

					} else {
						final Location tpLoc = player.getLocation().add(e.getFrom().toVector().subtract(e.getTo().toVector()).normalize().multiply(3));

						if(tpLoc.getBlock().getType().equals(Material.AIR)) {
							player.teleport(tpLoc);
						}
						else {
							final Location safeLoc = tpLoc.getWorld().getHighestBlockAt(tpLoc).getLocation().add(0D, 1D, 0D);
							player.teleport(safeLoc);
						}

					}

				} else {
					final Location tpLoc = player.getLocation().add(e.getFrom().toVector().subtract(e.getTo().toVector()).normalize().multiply(3));
					if(tpLoc.getBlock().getType().equals(Material.AIR)) { player.teleport(tpLoc); }
					else {
						final Location safeLoc = tpLoc.getWorld().getHighestBlockAt(tpLoc).getLocation().add(0D, 1D, 0D);
						player.teleport(safeLoc);
					}

				}

				if(!MessageHandler.spamMessageClaim.contains(player.getUniqueId().toString())) {
					player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
					MessageHandler.sendMessage(player, "&cYou are banned from this claim.");
					MessageHandler.spamMessageClaim.add(player.getUniqueId().toString());

					Bukkit.getScheduler().runTaskLater(BfcPlugin.getInstance(), () -> {
						MessageHandler.spamMessageClaim.remove(player.getUniqueId().toString());
					}, 5L * 20L);
				}
			}

		}

	}

	private boolean playerBanned(Player player, String claimID) {
		final ClaimData claimData = new ClaimData();
		if(claimData.checkClaim(claimID)) {
			if(claimData.bannedPlayers(claimID) != null) {
				for(final String bp : claimData.bannedPlayers(claimID)) {
					if(bp.equals(player.getUniqueId().toString())) {
						return true;
					}
				}
			}
		}

		return false;
	}

	@SuppressWarnings("deprecation")
	private boolean hasTrust(Player player, Claim claim) {
		final String accessDenied = claim.allowGrantPermission(player);
		final String buildDenied = claim.allowBuild(player, Material.DIRT);

		if(accessDenied == null || buildDenied == null || player.getUniqueId().equals(claim.getOwnerID())) { return true; }
		else { return false; }
	}
}
