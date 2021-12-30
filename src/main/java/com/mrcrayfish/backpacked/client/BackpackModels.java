package com.mrcrayfish.backpacked.client;

import com.mrcrayfish.backpacked.client.model.BackpackModel;
import com.mrcrayfish.backpacked.client.model.BambooBasketBackpackModel;
import com.mrcrayfish.backpacked.client.model.ClassicBackpackModel;
import com.mrcrayfish.backpacked.client.model.MiniChestBackpackModel;
import com.mrcrayfish.backpacked.client.model.RocketBackpackModel;
import com.mrcrayfish.backpacked.client.model.StandardBackpackModel;

/**
 * Author: MrCrayfish
 */
public class BackpackModels
{
    public static final BackpackModel STANDARD = new StandardBackpackModel();
    public static final BackpackModel CLASSIC = new ClassicBackpackModel();
    public static final BackpackModel BAMBOO_BASKET = new BambooBasketBackpackModel();
    public static final BackpackModel ROCKET = new RocketBackpackModel();
    public static final BackpackModel MINI_CHEST = new MiniChestBackpackModel();
}
