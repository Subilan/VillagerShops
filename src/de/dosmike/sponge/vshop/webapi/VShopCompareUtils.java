package de.dosmike.sponge.vshop.webapi;

import java.util.Optional;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;

import com.flowpowered.math.vector.Vector3d;

import de.dosmike.sponge.vshop.API;
import de.dosmike.sponge.vshop.FieldResolver;
import de.dosmike.sponge.vshop.NPCguard;
import de.dosmike.sponge.vshop.webapi.wrapper.CachedVShop;
import valandur.webapi.shadow.javax.ws.rs.BadRequestException;

public class VShopCompareUtils {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static void applyDiv(CachedVShop update, NPCguard shop) {
		Entity rep = shop.getLe(); 
		boolean respawnShop = false; //if the entity changed a new entity / mob has to be spawned
		if (update.getEntityType()!=null &&  !update.getEntityType().equalsIgnoreCase(shop.getNpcType().toString())) {
			EntityType et = (EntityType) FieldResolver.getFinalStaticAuto(EntityType.class, update.getEntityType());
			if (et == null)
				throw new BadRequestException("EntityType " + update.getEntityType() + " is unknown");
			respawnShop = true;
			shop.setNpcType(et);
		}
		if (update.getEntityVariant()!=null &&  !update.getEntityVariant().equalsIgnoreCase(shop.getVariantName())) {
			respawnShop = true;
			shop.setVariant(update.getEntityVariant());
		}
		if (update.getLocation()!=null) {
			Optional<Location> ol = update.getLocation().getLive();
			if (!ol.isPresent())
				throw new BadRequestException("Could not get Live version of Location");
			shop.move(ol.get());
		}
		if (update.getName()!=null &&  !update.getName().equals(TextSerializers.FORMATTING_CODE.serialize(shop.getDisplayName()))) {
			shop.setDisplayName(TextSerializers.FORMATTING_CODE.deserialize(update.getName()));
		}
		if (update.getOwner()!=null) {
			if (update.isPlayerShop() == null)
				throw new BadRequestException("Missing flag playershop");
			if (!update.isPlayerShop()) {
				throw new BadRequestException("Can't set owner of non player-shops");
			} else {
				shop.setPlayerShop(update.getOwner());
			}
		} else if (!update.isPlayerShop()) {
			shop.setPlayerShop(null);
		} else {
			throw new BadRequestException("Missing owner UUID for player-shop");
		}
		if (update.getRotation()!=null &&  update.getRotation()!=shop.getRot().getY()) {
			shop.setRot(new Vector3d(0.0, update.getRotation(), 0.0));
		}
		if (update.getStockContainer() == null && shop.getStockContainer().isPresent()) {
			API.playershop(shop, null, null);
		} else if (update.getStockContainer() != null) {
			Optional<Location> ol = update.getLocation().getLive();
			if (!ol.isPresent())
				throw new BadRequestException("Could not get Live version of Stock Location");
			if (!ol.get().equals(shop.getStockContainer().orElse(null))) {
				API.playershop(shop, shop.getShopOwner().get(), ol.get());
			}
		}
		
		if (respawnShop && !rep.isRemoved()) {
			rep.remove();
			//tick will respawn the new entity
		}
	}
	
}
