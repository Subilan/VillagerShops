package de.dosmike.sponge.vshop.webapi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;

import de.dosmike.sponge.vshop.API;
import de.dosmike.sponge.vshop.FieldResolver;
import de.dosmike.sponge.vshop.InvPrep;
import de.dosmike.sponge.vshop.NPCguard;
import de.dosmike.sponge.vshop.StockItem;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.webapi.wrapper.CachedStockItem;
import de.dosmike.sponge.vshop.webapi.wrapper.CachedVShop;
import valandur.webapi.WebAPI;
import valandur.webapi.serialize.SerializeService;
import valandur.webapi.servlet.base.BaseServlet;
import valandur.webapi.servlet.base.Permission;
import valandur.webapi.servlet.base.ServletService;
import valandur.webapi.shadow.io.swagger.annotations.Api;
import valandur.webapi.shadow.io.swagger.annotations.ApiOperation;
import valandur.webapi.shadow.javax.ws.rs.BadRequestException;
import valandur.webapi.shadow.javax.ws.rs.Consumes;
import valandur.webapi.shadow.javax.ws.rs.DELETE;
import valandur.webapi.shadow.javax.ws.rs.GET;
import valandur.webapi.shadow.javax.ws.rs.InternalServerErrorException;
import valandur.webapi.shadow.javax.ws.rs.NotFoundException;
import valandur.webapi.shadow.javax.ws.rs.POST;
import valandur.webapi.shadow.javax.ws.rs.PUT;
import valandur.webapi.shadow.javax.ws.rs.Path;
import valandur.webapi.shadow.javax.ws.rs.PathParam;
import valandur.webapi.shadow.javax.ws.rs.Produces;
import valandur.webapi.shadow.javax.ws.rs.core.MediaType;
import valandur.webapi.shadow.javax.ws.rs.core.Response;

@Path("vshop")
@Api(	value = "Create, edit and delete villager shops",
		tags = {"Villager Shops", "VShops", "VillagerShops"})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class VShopServlet extends BaseServlet {

	@GET
	@Path("/shop")
	@Permission({"vshop", "list"})
	@ApiOperation(
			value = "List Shops",
			notes = "Return a list of all shops")
	public Collection<CachedVShop> listShops() {
		return WebAPI.runOnMain(()->{
			return API.list().stream().map((s)->new CachedVShop(s)).collect(Collectors.toList());
		});
	}
	
	@GET
	@Path("/shop/{id}")
	@Permission({"vshop", "one"})
	@ApiOperation(
			value = "Get a Shop",
			notes = "Get detailed information about a shop")
	public CachedVShop getShop(@PathParam("id") UUID id) 
		throws NotFoundException {
		
		return WebAPI.runOnMain(()->{
			Optional<NPCguard> npc = VillagerShops.getNPCfromShopUUID(id);
			if (!npc.isPresent()) {
				throw new NotFoundException("Shop with id "+id+" not found");
			}
			return new CachedVShop(npc.get());
		});
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@POST
	@Path("/shop")
	@Permission({"vshop", "create"})
	@ApiOperation(
			value = "Create Shops",
			notes = "Spawn a new shop with base values; Some values are only set by update",
			response = CachedVShop.class)
	public Response createShop(CachedVShop req) 
		throws URISyntaxException {
		
		if (req == null)
			throw new BadRequestException("Request body is required");
		
		CachedVShop shop = WebAPI.runOnMain(()->{
			//Optional<EntityType> et = Sponge.getRegistry().getType(EntityType.class, req.getEntityType());
			EntityType et = (EntityType)FieldResolver.getFinalStaticAuto(EntityType.class, req.getEntityType());
			if (et == null)
				throw new BadRequestException("EntityType " + req.getEntityType() + " is unknown");
			String va = req.getEntityVariant();
			Optional<Location> location = req.getLocation().getLive();
			if (!location.isPresent())
				throw new BadRequestException("Could not get Live version of Location");
			
			NPCguard npc = API.create(et, 
					va==null?"none":va, 
					TextSerializers.FORMATTING_CODE.deserialize(req.getName()),
					location.get(), 
					req.getRotation());
			
			return new CachedVShop(npc);
		});
		
		return Response.created(new URI(null,null,shop.getLink(),null))
				.entity(shop).build();	
	}
	
	@PUT
	@Path("/shop/{id}")
	@Permission({"vshop", "edit"})
	@ApiOperation(
			value = "Change Shop",
			notes = "Modifies values for this shop, but items")
	public CachedVShop updateShop(@PathParam("id") UUID id, CachedVShop req) 
		throws NotFoundException {
		
		if (req == null)
			throw new BadRequestException("Request body is required");
		
		return WebAPI.runOnMain(()->{ try {
			
			Optional<NPCguard> npc = VillagerShops.getNPCfromShopUUID(id);
			if (!npc.isPresent()) {
				throw new NotFoundException("Shop with id "+id+" not found");
			}
			VillagerShops.closeShopInventories(id);
			VShopCompareUtils.applyDiv(req, npc.get());
			
			return new CachedVShop(npc.get());
		} catch (RuntimeException rte) {
			if (rte instanceof BadRequestException) {
				throw rte;
			} else {
				throw new InternalServerErrorException(rte.getMessage());
			}
		}
		});
	}
	
	@DELETE
	@Path("/shop/{id}")
	@Permission({"vshop", "delete"})
	@ApiOperation(
			value = "Delete a Shop",
			notes = "Permanently delete a shop from the server",
			response = CachedVShop.class)
	public CachedVShop deleteShop(@PathParam("id") UUID id) 
		throws NotFoundException {
		
		return WebAPI.runOnMain(()->{ try {
			
			Optional<NPCguard> npc = VillagerShops.getNPCfromShopUUID(id);
			if (!npc.isPresent()) {
				throw new NotFoundException("Shop with id "+id+" not found");
			}
			API.delete(npc.get());
			
			return new CachedVShop(npc.get());
		} catch (RuntimeException rte) {
			if (rte instanceof BadRequestException) {
				throw rte;
			} else {
				throw new InternalServerErrorException(rte.getMessage());
			}
		}
		});
	}
	
	@GET
	@Path("/shop/{id}/item")
	@Permission({"vshop", "item", "list"})
	@ApiOperation(
			value = "List Shop Items",
			notes = "Return a list of all shops items")
	public Collection<CachedStockItem> listShopItems(@PathParam("id") UUID id) {
		return WebAPI.runOnMain(()->{
			Optional<NPCguard> npc = VillagerShops.getNPCfromShopUUID(id);
			if (!npc.isPresent()) {
				throw new NotFoundException("Shop with id "+id+" not found");
			}
			InvPrep inv = npc.get().getPreparator();
			int s=inv.size();
			
			List<CachedStockItem> csi = new ArrayList<>(s);
			for (int i=0;i<s;i++)
				csi.add(new CachedStockItem(inv.getItem(i)));
			
			return csi;
		});
	}
	
	@GET
	@Path("/shop/{id}/item/{item}")
	@Permission({"vshop", "item", "one"})
	@ApiOperation(
			value = "Get a Shop",
			notes = "Get detailed information about a shop item")
	public CachedStockItem getShopItem(@PathParam("id") UUID id, @PathParam("item") int item) 
		throws NotFoundException {
		
		return WebAPI.runOnMain(()->{
			Optional<NPCguard> npc = VillagerShops.getNPCfromShopUUID(id);
			if (!npc.isPresent()) {
				throw new NotFoundException("Shop with id "+id+" not found");
			}
			InvPrep inv = npc.get().getPreparator();
			int s=inv.size();
			if (item < 0 || item >= s) 
				throw new BadRequestException("Item index out of bounds (0 <= "+item+" < "+s+")");
			
			return new CachedStockItem(inv.getItem(item));
		});
	}
	
	@POST
	@Path("/shop/{id}/item")
	@Permission({"vshop", "item", "create"})
	@ApiOperation(
			value = "Add Shop Item",
			notes = "Add a item to the shops listing",
			response = CachedStockItem.class)
	public Response addShopItem(@PathParam("id") UUID id, CachedStockItem req) 
		throws URISyntaxException {
		
		if (req == null)
			throw new BadRequestException("Request body is required");
		req.validate();
		
		CachedStockItem shop = WebAPI.runOnMain(()->{
			Optional<NPCguard> npc = VillagerShops.getNPCfromShopUUID(id);
			if (!npc.isPresent()) {
				throw new NotFoundException("Shop with id "+id+" not found");
			}
			VillagerShops.closeShopInventories(id);
			InvPrep inv = npc.get().getPreparator();
			int s=inv.size();
			if (s>=27) 
				throw new BadRequestException("Tried to add more than 27 items to shop");
			
			inv.addItem(new StockItem(req.getItem().createStack(), req.getSellPrice(), req.getBuyPrice(), VillagerShops.getInstance().CurrencyByName(req.getCurrency()),req.getMaxStock()));
			
			return new CachedStockItem(inv.getItem(s));
		});
		
		return Response.created(new URI(null,null,shop.getLink(),null))
				.entity(shop).build();	
	}
	
	@PUT
	@Path("/shop/{id}/item/{item}")
	@Permission({"vshop", "item", "edit"})
	@ApiOperation(
			value = "Change Shop Item",
			notes = "Modifies values for this shop item")
	public CachedStockItem updateShopItem(@PathParam("id") UUID id, @PathParam("item") int item,  CachedStockItem req) 
		throws NotFoundException {
		
		if (req == null)
			throw new BadRequestException("Request body is required");
		req.validate();
		
		return WebAPI.runOnMain(()->{ try {
			
			Optional<NPCguard> npc = VillagerShops.getNPCfromShopUUID(id);
			if (!npc.isPresent()) {
				throw new NotFoundException("Shop with id "+id+" not found");
			}
			VillagerShops.closeShopInventories(id);
			InvPrep inv = npc.get().getPreparator();
			int s=inv.size();
			if (item < 0 || item >= s) 
				throw new BadRequestException("Item index out of bounds (0 <= "+item+" < "+s+")");
			
			inv.setItem(item, new StockItem(req.getItem().createStack(), req.getSellPrice(), req.getBuyPrice(), VillagerShops.getInstance().CurrencyByName(req.getCurrency()),req.getMaxStock()));
			
			return new CachedStockItem(inv.getItem(item));
			
		} catch (RuntimeException rte) {
			if (rte instanceof BadRequestException) {
				throw rte;
			} else {
				throw new InternalServerErrorException(rte.getMessage());
			}
		}
		});
	}
	
	@DELETE
	@Path("/shop/{id}/item/{item}")
	@Permission({"vshop", "item", "delete"})
	@ApiOperation(
			value = "Removes a Shop Item",
			notes = "Remove an item from this shop",
			response = CachedVShop.class)
	public CachedStockItem deleteShopItem(@PathParam("id") UUID id, @PathParam("item") int item) 
		throws NotFoundException {
		
		return WebAPI.runOnMain(()->{ try {
			
			Optional<NPCguard> npc = VillagerShops.getNPCfromShopUUID(id);
			if (!npc.isPresent()) {
				throw new NotFoundException("Shop with id "+id+" not found");
			}
			VillagerShops.closeShopInventories(id);
			InvPrep inv = npc.get().getPreparator();
			int s=inv.size();
			if (item < 0 || item >= s) 
				throw new BadRequestException("Item index out of bounds (0 <= "+item+" < "+s+")");
			
			CachedStockItem rem = new CachedStockItem(inv.getItem(item));
			inv.removeIndex(item);
			
			return rem;
			
		} catch (RuntimeException rte) {
			if (rte instanceof BadRequestException) {
				throw rte;
			} else {
				throw new InternalServerErrorException(rte.getMessage());
			}
		}
		});
	}

	public static void init() {
		Optional<ServletService> ss = Sponge.getServiceManager().provide(ServletService.class);
		if (!ss.isPresent()) {
			VillagerShops.l("WebAPI ServletService not running!");
			return;
		}
		ss.get().registerServlet(VShopServlet.class);

		SerializeService srv = WebAPI.getSerializeService();
        srv.registerCache(NPCguard.class, CachedVShop.class);
        srv.registerCache(StockItem.class, CachedStockItem.class);
		
		VillagerShops.l("WebAPI Servlet registered!");
	}
	
}
