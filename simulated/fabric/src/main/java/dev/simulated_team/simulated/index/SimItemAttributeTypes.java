package dev.simulated_team.simulated.index;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.item_attributes.BlockBouncinessItemAttribute;
import dev.simulated_team.simulated.content.item_attributes.BlockFrictionItemAttribute;
import dev.simulated_team.simulated.content.item_attributes.BlockMassItemAttribute;
import dev.simulated_team.simulated.backport.veil.platform.registry.RegistrationProvider;
import dev.simulated_team.simulated.backport.veil.platform.registry.RegistryObject;

public class SimItemAttributeTypes {
	private static final RegistrationProvider<ItemAttributeType> REGISTRY = RegistrationProvider.get(CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE, Simulated.MOD_ID);

	public static final RegistryObject<ItemAttributeType> BLOCK_MASS = REGISTRY.register("block_mass", BlockMassItemAttribute.Type::new);
	public static final RegistryObject<ItemAttributeType> BLOCK_FRICTION = REGISTRY.register("block_friction", BlockFrictionItemAttribute.Type::new);
	public static final RegistryObject<ItemAttributeType> BLOCK_BOUNCINESS = REGISTRY.register("block_bounciness", BlockBouncinessItemAttribute.Type::new);

	public static void init() {

	}
}
