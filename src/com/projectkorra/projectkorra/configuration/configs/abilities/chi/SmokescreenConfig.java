package com.projectkorra.projectkorra.configuration.configs.abilities.chi;

import com.projectkorra.projectkorra.configuration.configs.abilities.AbilityConfig;

public class SmokescreenConfig extends AbilityConfig {

	public final long Cooldown = 5000;
	public final int Duration = 3000;
	public final double Radius = 3;
	
	public SmokescreenConfig() {
		super(true, "", "");
	}

	@Override
	public String getName() {
		return "Smokescreen";
	}

	@Override
	public String[] getParents() {
		return new String[] { "Abilities", "Chi" };
	}

}