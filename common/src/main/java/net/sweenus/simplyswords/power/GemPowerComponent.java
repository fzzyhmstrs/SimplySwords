package net.sweenus.simplyswords.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public record GemPowerComponent(boolean hasRunicPower, boolean hasNetherPower, RegistryEntry<GemPower> runicPower, RegistryEntry<GemPower> netherPower) {

	public static final Codec<GemPowerComponent> CODEC = RecordCodecBuilder.create(instance ->
				instance.group(
						Codec.BOOL.fieldOf("has_runic_power").forGetter(GemPowerComponent::hasRunicPower),
						Codec.BOOL.fieldOf("has_nether_power").forGetter(GemPowerComponent::hasNetherPower),
						RegistryFixedCodec.of(GemPowerRegistry.REGISTRY.key()).fieldOf("runic_power").forGetter(GemPowerComponent::runicPower),
						RegistryFixedCodec.of(GemPowerRegistry.REGISTRY.key()).fieldOf("nether_power").forGetter(GemPowerComponent::netherPower)
				).apply(instance, GemPowerComponent::new)
			);

	public static final PacketCodec<RegistryByteBuf, GemPowerComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL,
			GemPowerComponent::hasRunicPower,
			PacketCodecs.BOOL,
			GemPowerComponent::hasNetherPower,
			PacketCodecs.registryEntry(GemPowerRegistry.REGISTRY.key()),
			GemPowerComponent::runicPower,
			PacketCodecs.registryEntry(GemPowerRegistry.REGISTRY.key()),
			GemPowerComponent::netherPower,
			GemPowerComponent::new
	);

	public static final GemPowerComponent DEFAULT = new GemPowerComponent(false, false, GemPowerRegistry.EMPTY, GemPowerRegistry.EMPTY);

	public static GemPowerComponent runic(@NotNull RegistryEntry<GemPower> power) {
		return new GemPowerComponent(true, false, power, GemPowerRegistry.EMPTY);
	}

	public static GemPowerComponent nether(@NotNull RegistryEntry<GemPower> power) {
		return new GemPowerComponent(false, true, GemPowerRegistry.EMPTY, power);
	}

	public static GemPowerComponent create(@Nullable RegistryEntry<GemPower> runic, @Nullable RegistryEntry<GemPower> nether) {
		return new GemPowerComponent(runic != null, nether != null, runic != null ? runic : GemPowerRegistry.EMPTY, nether != null ? nether : GemPowerRegistry.EMPTY);
	}

	public static GemPowerComponent createEmpty(boolean hasRunic, boolean hasNether) {
		return new GemPowerComponent(hasRunic, hasNether, GemPowerRegistry.EMPTY, GemPowerRegistry.EMPTY);
	}

	public GemPowerComponent fill(BiFunction<Boolean, RegistryEntry<GemPower>, RegistryEntry<GemPower>> runicFiller, BiFunction<Boolean, RegistryEntry<GemPower>, RegistryEntry<GemPower>> netherFiller) {
		return new GemPowerComponent(this.hasRunicPower, this.hasNetherPower, runicFiller.apply(this.hasRunicPower, this.runicPower), netherFiller.apply(this.hasNetherPower, this.netherPower));
	}

	public boolean canBeFilled() {
		return hasRunicPower || hasNetherPower;
	}

	public boolean isEmpty() {
		return runicPower.value().isEmpty() && netherPower.value().isEmpty();
	}

	public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		runicPower.value().postHit(stack, target, attacker);
		netherPower.value().postHit(stack, target, attacker);
	}

	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);
		TypedActionResult<ItemStack> result1 = runicPower.value().use(world, user, hand, itemStack);
		TypedActionResult<ItemStack> result2 = netherPower.value().use(world, user, hand, itemStack);
		if (result1.getResult().compareTo(result2.getResult()) < 0) {
			return result1;
		} else {
			return result2;
		}
	}

	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		runicPower.value().usageTick(world, user, stack, remainingUseTicks);
		netherPower.value().usageTick(world, user, stack, remainingUseTicks);
	}

	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		runicPower.value().onStoppedUsing(stack, world, user, remainingUseTicks);
		netherPower.value().onStoppedUsing(stack, world, user, remainingUseTicks);
	}

	public int getMaxUseTime(ItemStack stack) {
		return Math.max(runicPower.value().getMaxUseTime(stack), netherPower.value().getMaxUseTime(stack));
	}

	public void inventoryTick(ItemStack stack, World world, LivingEntity user, int slot, boolean selected) {
		runicPower.value().inventoryTick(stack, world, user, slot, selected);
		netherPower.value().inventoryTick(stack, world, user, slot, selected);
	}

	public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
		appendTooltip(itemStack, tooltipContext, tooltip, type, false);
	}

	public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
		if (runicPower.value().isGreater()) {
			tooltip.add(Text.translatable("item.simplyswords.greater_runic_power").setStyle(Styles.RUNIC));
		}
		if (!runicPower.value().isEmpty()) {
			runicPower.value().appendTooltip(itemStack, tooltipContext, tooltip, type, isRunic);
			if (!netherPower.value().isEmpty()) {
				tooltip.add(Text.literal(""));
			}
		} else if (!isRunic && hasRunicPower) {
			tooltip.add(Text.translatable("item.simplyswords.empty_runic_slot").formatted(Formatting.GRAY));
		}

		if (netherPower.value().isGreater()) {
			tooltip.add(Text.translatable("item.simplyswords.greater_nether_power").setStyle(Styles.NETHERFUSED));
		}
		if (!netherPower.value().isEmpty()) {
			netherPower.value().appendTooltip(itemStack, tooltipContext, tooltip, type, isRunic);
		} else if (!isRunic && hasNetherPower) {
			tooltip.add(Text.translatable("item.simplyswords.empty_nether_slot").formatted(Formatting.GRAY));
		}
	}
}