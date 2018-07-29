package de.dosmike.sponge.vshop.webapi.wrapper;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import de.dosmike.sponge.vshop.InvPrep;
import de.dosmike.sponge.vshop.NPCguard;
import de.dosmike.sponge.vshop.VillagerShops;
import valandur.webapi.cache.CachedObject;
import valandur.webapi.cache.world.CachedLocation;
import valandur.webapi.serialize.JsonDetails;
import valandur.webapi.shadow.io.swagger.annotations.ApiModel;
import valandur.webapi.shadow.io.swagger.annotations.ApiModelProperty;
import valandur.webapi.util.Constants;

@ApiModel("VillagerShopsShop")
public class CachedVShop extends CachedObject<NPCguard> {
	
	String uid;
	@ApiModelProperty(value = "The unique shop identifier; this is not the mob uuid")
	public UUID getUID() {
		return UUID.fromString(uid);
	}
	
	String name;
	@JsonDetails
	@ApiModelProperty(value = "The escaped shop name", required = true)
	public String getName() {
		return name;
	}
	
	CachedLocation location;
	@JsonDetails
	@ApiModelProperty(value = "Where the shop is currently located", required = true)
	public CachedLocation getLocation() {
		return location;
	}
	
	Double rotation;
	@JsonDetails
	@ApiModelProperty(value = "The mobs roations around their up-axis", required = true)
	public Double getRotation() {
		return rotation;
	}
	
	String entityType;
	@JsonDetails
	@ApiModelProperty(value = "The minecraft entity type string for this shops visual entity", required = true,
					example = "minecraft:villager")
	public String getEntityType() {
		return entityType;
	}
		
	String entityVariant;
	@JsonDetails
	@ApiModelProperty(value = "A very dynamic variant string for vanilla mobs, most variants as in the minecraft wiki should be supported", required = true,
					example = "butcher")
	public String getEntityVariant() {
		return entityVariant;
	}
	
	UUID owner;
	@JsonDetails
	@ApiModelProperty(value = "If this shop is a player shop this conatins the UUID of this shops owner", required = true)
	public UUID getOwner() {
		return owner;
	}
//	Boolean isPlayerShop;
//	@JsonDetails
//	@ApiModelProperty(value = "Returns wether this is a player shop or not", required = true)
//	public Boolean isPlayerShop() {
//		return isPlayerShop;
//	}
	
	List<CachedStockItem> stockItems;
	@JsonDetails
	@ApiModelProperty(value = "Returns a list of all stock items currently listed", required = true)
	public List<CachedStockItem> getStockItems() {
		return stockItems;
	}
	
	CachedLocation stockContainer;
	@JsonDetails
	@ApiModelProperty(value = "Location where a container should reside for stocking items", required = true)
	public CachedLocation getStockContainer() {
		return stockContainer;
	}
	
	public CachedVShop() {
		super(null);
	}
	public CachedVShop(NPCguard shop) {
		super(shop);
		
		this.uid = shop.getIdentifier().toString();
		this.name = TextSerializers.FORMATTING_CODE.serialize(shop.getDisplayName());
		this.location = new CachedLocation(shop.getLoc());
		this.rotation = shop.getRot().getY(); 
		this.entityType = shop.getNpcType().getId();
		this.entityVariant = shop.getVariantName();
		this.owner = shop.getShopOwner().orElse(null);
//		this.isPlayerShop = shop.getShopOwner().isPresent();
		
		Optional<Location<World>> stock = shop.getStockContainer();
		if (stock.isPresent()) {
			this.stockContainer = new CachedLocation(shop.getStockContainer().get());
		} else {
			this.stockContainer = null;
		}
		
		InvPrep inv = shop.getPreparator();
		this.stockItems = new LinkedList<>();
		int s = inv.size();
		for (int i = 0; i < s; i++)
			stockItems.add(new CachedStockItem(inv.getItem(i), i, getUID()));
	}
	
	@Override
	public Optional<NPCguard> getLive() {
		return VillagerShops.getNPCfromShopUUID(getUID());
	}
	
	@Override
	public String getLink() {
		return Constants.BASE_PATH + "/vshop/shop/" + uid;
	}
}
