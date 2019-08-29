/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.items;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.tool.ITool;
import blusunrize.immersiveengineering.common.blocks.IEBaseBlock;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IConfigurableSides;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IDirectionalTile;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.items.IEItemInterfaces.IItemDamageableIE;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import blusunrize.immersiveengineering.common.util.RotationUtil;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.advancements.IEAdvancements;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static blusunrize.immersiveengineering.common.IEConfig.Tools.hammerDurabiliy;

public class ItemHammer extends ItemIEBase implements IItemDamageableIE, ITool
{
	public static final ToolType HAMMER_TOOL = ToolType.get(ImmersiveEngineering.MODID+":hammer");

	public ItemHammer()
	{
		super("hammer", new Properties().maxStackSize(1).setNoRepair());
	}

	@Override
	public int getMaxDamageIE(ItemStack stack)
	{
		return hammerDurabiliy;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn)
	{
		super.addInformation(stack, worldIn, tooltip, flagIn);
		if(ItemNBTHelper.hasKey(stack, "multiblockPermission"))
		{
			ListNBT tagList = stack.getOrCreateTag().getList("multiblockPermission", 8);
			String s = I18n.format(Lib.DESC_INFO+"multiblocksAllowed");
			addInfo(tooltip, s, tagList);
		}
		if(ItemNBTHelper.hasKey(stack, "multiblockInterdiction"))
		{
			ListNBT tagList = stack.getOrCreateTag().getList("multiblockInterdiction", 8);
			String s = I18n.format(Lib.DESC_INFO+"multiblockForbidden");
			addInfo(tooltip, s, tagList);
		}
	}

	private void addInfo(List<ITextComponent> list, String s, ListNBT tagList)
	{
		if(!Screen.isShiftKeyDown())
			list.add(new TranslationTextComponent(Lib.DESC_INFO+"holdShift", s));
		else
		{
			list.add(new StringTextComponent(s));
			for(int i = 0; i < tagList.size(); i++)
				list.add(new TranslationTextComponent(Lib.DESC_INFO+"multiblock."+tagList.getString(i))
						.setStyle(new Style().setColor(TextFormatting.DARK_GRAY)));
		}
	}

	@Nonnull
	@Override
	public ActionResultType onItemUse(ItemUseContext context)
	{
		World world = context.getWorld();
		BlockPos pos = context.getPos();
		Direction side = context.getFace();
		TileEntity tileEntity = world.getTileEntity(pos);
		if(!(tileEntity instanceof IDirectionalTile)&&!(tileEntity instanceof IHammerInteraction)&&!(tileEntity instanceof IConfigurableSides))
			if(RotationUtil.rotateBlock(world, pos, side))
				return ActionResultType.SUCCESS;
		return ActionResultType.PASS;
	}

	@Override
	public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context)
	{
		World world = context.getWorld();
		BlockPos pos = context.getPos();
		PlayerEntity player = context.getPlayer();
		Direction side = context.getFace();
		String[] permittedMultiblocks = null;
		String[] interdictedMultiblocks = null;
		if(ItemNBTHelper.hasKey(stack, "multiblockPermission"))
		{
			ListNBT list = stack.getOrCreateTag().getList("multiblockPermission", 8);
			permittedMultiblocks = new String[list.size()];
			for(int i = 0; i < permittedMultiblocks.length; i++)
				permittedMultiblocks[i] = list.getString(i);
		}
		if(ItemNBTHelper.hasKey(stack, "multiblockInterdiction"))
		{
			ListNBT list = stack.getOrCreateTag().getList("multiblockInterdiction", 8);
			interdictedMultiblocks = new String[list.size()];
			for(int i = 0; i < interdictedMultiblocks.length; i++)
				interdictedMultiblocks[i] = list.getString(i);
		}
		for(MultiblockHandler.IMultiblock mb : MultiblockHandler.getMultiblocks())
			if(mb.isBlockTrigger(world.getBlockState(pos)))
			{
				boolean b = permittedMultiblocks==null;
				if(permittedMultiblocks!=null)
					for(String s : permittedMultiblocks)
						if(mb.getUniqueName().equalsIgnoreCase(s))
						{
							b = true;
							break;
						}
				if(!b)
					break;
				if(interdictedMultiblocks!=null)
					for(String s : interdictedMultiblocks)
						if(mb.getUniqueName().equalsIgnoreCase(s))
						{
							b = false;
							break;
						}
				if(!b)
					break;
				if(MultiblockHandler.postMultiblockFormationEvent(player, mb, pos, stack).isCanceled())
					continue;
				if(mb.createStructure(world, pos, side, player))
				{
					if(player instanceof ServerPlayerEntity)
						IEAdvancements.TRIGGER_MULTIBLOCK.trigger((ServerPlayerEntity)player, mb, stack);
					return ActionResultType.SUCCESS;
				}
			}
		return ActionResultType.PASS;
	}

	@Nonnull
	@Override
	public ItemStack getContainerItem(@Nonnull ItemStack stack)
	{
		ItemStack container = stack.copy();
		this.damageIETool(container, 1, Utils.RAND, null);
		return container;
	}

	@Override
	public boolean isEnchantable(@Nonnull ItemStack stack)
	{
		return true;
	}

	@Override
	public int getItemEnchantability()
	{
		return 14;
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment)
	{
		return enchantment==Enchantments.EFFICIENCY||enchantment==Enchantments.UNBREAKING||enchantment==Enchantments.MENDING;
	}

	@Override
	public boolean doesSneakBypassUse(ItemStack stack, IWorldReader world, BlockPos pos, PlayerEntity player)
	{
		return true;
	}

	@Override
	public boolean itemInteractionForEntity(ItemStack stack, PlayerEntity player, LivingEntity entity, Hand hand)
	{
		return !player.world.isRemote&&RotationUtil.rotateEntity(entity, player);
	}

	@Nonnull
	@Override
	public Set<ToolType> getToolTypes(ItemStack stack)
	{
		return ImmutableSet.of(HAMMER_TOOL);
	}

	@Override
	public int getHarvestLevel(ItemStack stack, @Nonnull ToolType tool, @Nullable PlayerEntity player, @Nullable BlockState blockState)
	{
		if(getToolTypes(stack).contains(tool))
			return 2;
		else
			return -1;
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state)
	{
		for(ToolType type : this.getToolTypes(stack))
			if(state.getBlock().isToolEffective(state, type))
				return 6;
		return super.getDestroySpeed(stack, state);
	}

	@Override
	public boolean isTool(ItemStack item)
	{
		return true;
	}

	@Override
	public boolean canHarvestBlock(ItemStack stack, BlockState state)
	{
		if(state.getBlock() instanceof IEBaseBlock)
		{
			if(((IEBaseBlock)state.getBlock()).allowHammerHarvest(state))
				return true;
		}
		else if(state.getBlock().isToolEffective(state, HAMMER_TOOL))
			return true;
		return false;
	}
}