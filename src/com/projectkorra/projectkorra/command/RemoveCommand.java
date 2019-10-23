package com.projectkorra.projectkorra.command;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.configuration.configs.commands.RemoveCommandConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.event.PlayerChangeElementEvent;
import com.projectkorra.projectkorra.event.PlayerChangeElementEvent.Result;
import com.projectkorra.projectkorra.event.PlayerChangeSubElementEvent;

/**
 * Executor for /bending remove. Extends {@link PKCommand}.
 */
public class RemoveCommand extends PKCommand<RemoveCommandConfig> {

	private final String succesfullyRemovedElementSelf, wrongElementSelf, invalidElement, playerOffline, wrongElementTarget, succesfullyRemovedElementTarget, succesfullyRemovedElementTargetConfirm, succesfullyRemovedAllElementsTarget, succesfullyRemovedAllElementsTargetConfirm;

	public RemoveCommand(final RemoveCommandConfig config) {
		super(config, "remove", "/bending remove <Player> [Element]", config.Description, new String[] { "remove", "rm" });

		this.succesfullyRemovedElementSelf = config.RemovedElement;
		this.succesfullyRemovedAllElementsTarget = config.RemovedAllElements_ByOther;
		this.succesfullyRemovedAllElementsTargetConfirm = config.RemovedAllElements_Other;
		this.succesfullyRemovedElementTarget = config.RemovedElement_ByOther;
		this.succesfullyRemovedElementTargetConfirm = config.RemovedAllElements_Other;
		this.invalidElement = config.InvalidElement;
		this.wrongElementSelf = config.WrongElement;
		this.wrongElementTarget = config.WrongElement_Other;
		this.playerOffline = config.PlayerOffline;
	}

	@Override
	public void execute(final CommandSender sender, final List<String> args) {
		if (!this.hasPermission(sender) || !this.correctLength(sender, args.size(), 1, 2)) {
			return;
		}

		final Player player = Bukkit.getPlayer(args.get(0));
		if (player == null) {
			if (args.size() == 1) {
				final Element e = Element.fromString(args.get(0));
				final BendingPlayer senderBPlayer = BendingPlayer.getBendingPlayer(sender.getName());

				if (senderBPlayer != null && sender instanceof Player) {
					if (e != null) {
						if (e instanceof SubElement) {
							if (senderBPlayer.hasElement(e)) {
								senderBPlayer.getSubElements().remove(e);
								GeneralMethods.deleteElement(senderBPlayer, e);
								GeneralMethods.removeUnusableAbilities(sender.getName());
								GeneralMethods.sendBrandingMessage(sender, e.getColor() + this.succesfullyRemovedElementSelf.replace("{element}", e.getName() + e.getType().getBending()).replace("{sender}", ChatColor.DARK_AQUA + sender.getName() + e.getColor()));
								Bukkit.getServer().getPluginManager().callEvent(new PlayerChangeSubElementEvent(sender, player, (SubElement) e, PlayerChangeSubElementEvent.Result.REMOVE));
							} else {
								GeneralMethods.sendBrandingMessage(sender, ChatColor.RED + this.wrongElementSelf);
							}
							return;
						} else if (e instanceof Element) {
							if (senderBPlayer.hasElement(e)) {
								senderBPlayer.getElements().remove(e);
								GeneralMethods.deleteElement(senderBPlayer, e);
								GeneralMethods.removeUnusableAbilities(sender.getName());

								GeneralMethods.sendBrandingMessage(sender, e.getColor() + this.succesfullyRemovedElementSelf.replace("{element}", e.getName() + e.getType().getBending()));
								Bukkit.getServer().getPluginManager().callEvent(new PlayerChangeElementEvent(sender, (Player) sender, e, Result.REMOVE));
								return;
							} else {
								GeneralMethods.sendBrandingMessage(sender, ChatColor.RED + this.wrongElementSelf);
							}
							return;
						}
					} else {
						GeneralMethods.sendBrandingMessage(sender, ChatColor.RED + this.invalidElement);
					}
					return;
				}
				GeneralMethods.sendBrandingMessage(sender, ChatColor.RED + this.playerOffline);
				return;
			} else {
				this.help(sender, false);
				return;
			}
		}

		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null) {
			GeneralMethods.createBendingPlayer(player.getUniqueId(), player.getName());
			bPlayer = BendingPlayer.getBendingPlayer(player);
		}
		if (args.size() == 2) {
			final Element e = Element.fromString(args.get(1));
			if (e != null) {
				if (!bPlayer.hasElement(e)) {
					GeneralMethods.sendBrandingMessage(sender, ChatColor.DARK_RED + this.wrongElementTarget.replace("{target}", player.getName()));
					return;
				}
				if (e instanceof SubElement) {
					bPlayer.getSubElements().remove(e);
				} else {
					bPlayer.getElements().remove(e);
				}
				GeneralMethods.deleteElement(bPlayer, e);

				GeneralMethods.removeUnusableAbilities(player.getName());
				GeneralMethods.sendBrandingMessage(player, e.getColor() + this.succesfullyRemovedElementTarget.replace("{element}", e.getName() + e.getType().getBending()).replace("{sender}", ChatColor.DARK_AQUA + sender.getName() + e.getColor()));
				GeneralMethods.sendBrandingMessage(sender, e.getColor() + this.succesfullyRemovedElementTargetConfirm.replace("{element}", e.getName() + e.getType().getBending()).replace("{target}", ChatColor.DARK_AQUA + player.getName() + e.getColor()));
				Bukkit.getServer().getPluginManager().callEvent(new PlayerChangeElementEvent(sender, player, e, Result.REMOVE));
				return;
			}
		} else if (args.size() == 1) {
			List<Element> removed = new LinkedList<>();
			removed.addAll(bPlayer.getElements());
			removed.addAll(bPlayer.getSubElements());
			
			bPlayer.getElements().clear();
			bPlayer.getSubElements().clear();
			
			GeneralMethods.deleteElements(bPlayer, removed);
			GeneralMethods.removeUnusableAbilities(player.getName());
			if (!player.getName().equalsIgnoreCase(sender.getName())) {
				GeneralMethods.sendBrandingMessage(sender, ChatColor.YELLOW + this.succesfullyRemovedAllElementsTargetConfirm.replace("{target}", ChatColor.DARK_AQUA + player.getName() + ChatColor.YELLOW));
			}

			GeneralMethods.sendBrandingMessage(player, ChatColor.YELLOW + this.succesfullyRemovedAllElementsTarget.replace("{sender}", ChatColor.DARK_AQUA + sender.getName() + ChatColor.YELLOW));
			Bukkit.getServer().getPluginManager().callEvent(new PlayerChangeElementEvent(sender, player, null, Result.REMOVE));
		}
	}

	/**
	 * Checks if the CommandSender has the permission 'bending.admin.remove'. If
	 * not, it tells them they don't have permission to use the command.
	 *
	 * @return True if they have the permission, false otherwise
	 */
	@Override
	public boolean hasPermission(final CommandSender sender) {
		if (sender.hasPermission("bending.admin." + this.getName())) {
			return true;
		}
		GeneralMethods.sendBrandingMessage(sender, super.noPermissionMessage);
		return false;
	}

	@Override
	protected List<String> getTabCompletion(final CommandSender sender, final List<String> args) {
		if (args.size() >= 2 || !sender.hasPermission("bending.command.remove")) {
			return new ArrayList<String>();
		}
		final List<String> l = new ArrayList<String>();
		if (args.size() == 0) {
			for (final Player p : Bukkit.getOnlinePlayers()) {
				l.add(p.getName());
			}
		} else {
			l.add("Air");
			l.add("Earth");
			l.add("Fire");
			l.add("Water");
			l.add("Chi");
			for (final Element e : Element.getAddonElements()) {
				l.add(e.getName());
			}

			l.add("Blood");
			l.add("Combustion");
			l.add("Flight");
			l.add("Healing");
			l.add("Ice");
			l.add("Lava");
			l.add("Lightning");
			l.add("Metal");
			l.add("Plant");
			l.add("Sand");
			l.add("Spiritual");

			for (final SubElement e : Element.getAddonSubElements()) {
				l.add(e.getName());
			}
		}
		return l;
	}
}
