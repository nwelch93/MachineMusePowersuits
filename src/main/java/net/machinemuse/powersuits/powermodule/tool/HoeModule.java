package net.machinemuse.powersuits.powermodule.tool;

import net.machinemuse.api.IModularItem;
import net.machinemuse.api.IPowerModule;
import net.machinemuse.api.ModuleManager;
import net.machinemuse.api.moduletrigger.IRightClickModule;
import net.machinemuse.powersuits.item.ItemComponent;
import net.machinemuse.powersuits.powermodule.PowerModuleBase;
import net.machinemuse.utils.ElectricItemUtils;
import net.machinemuse.utils.MuseCommonStrings;
import net.machinemuse.utils.MuseItemUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class HoeModule extends PowerModuleBase implements IPowerModule, IRightClickModule {
    public static final String MODULE_HOE = "Rototiller";
    public static final String HOE_ENERGY_CONSUMPTION = "Hoe Energy Consumption";
    public static final String HOE_SEARCH_RADIUS = "Hoe Search Radius";

    public HoeModule(List<IModularItem> validItems) {
        super(validItems);
        addInstallCost(MuseItemUtils.copyAndResize(ItemComponent.solenoid, 1));

        addBaseProperty(HOE_ENERGY_CONSUMPTION, 50);
        addTradeoffProperty("Search Radius", HOE_ENERGY_CONSUMPTION, 950);
        addTradeoffProperty("Search Radius", HOE_SEARCH_RADIUS, 8, "m");
    }

    @Override
    public ActionResult onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, EnumHand hand) {
        return ActionResult.newResult(EnumActionResult.PASS, itemStackIn);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack itemStack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        double energyConsumed = ModuleManager.computeModularProperty(itemStack, HOE_ENERGY_CONSUMPTION);
        if (!playerIn.canPlayerEdit(pos, facing, itemStack) || ElectricItemUtils.getPlayerEnergy(playerIn) < energyConsumed) {
            return EnumActionResult.FAIL;
        } else {
            int hook = net.minecraftforge.event.ForgeEventFactory.onHoeUse(itemStack, playerIn, worldIn, pos);
            if (hook != 0) return hook > 0 ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;

            double radius = (int) ModuleManager.computeModularProperty(itemStack, HOE_SEARCH_RADIUS);
            for (int i = (int) Math.floor(-radius); i < radius; i++) {
                for (int j = (int) Math.floor(-radius); j < radius; j++) {
                    if (i * i + j * j < radius * radius) {
                        BlockPos newPos = pos.add(i, 0, j);
                        IBlockState iblockstate = worldIn.getBlockState(newPos);
                        Block block = iblockstate.getBlock();
                        if (facing != EnumFacing.DOWN && (worldIn.isAirBlock(newPos.up()) || ToolHelpers.blockCheckAndHarvest(playerIn, worldIn, newPos.up()))) {
                            if (block == Blocks.GRASS || block == Blocks.GRASS_PATH) {
                                this.setBlock(itemStack, playerIn, worldIn, newPos, Blocks.FARMLAND.getDefaultState());
                            }

                            if (block == Blocks.DIRT) {
                                switch (iblockstate.getValue(BlockDirt.VARIANT)) {
                                    case DIRT:
                                        this.setBlock(itemStack, playerIn, worldIn, newPos, Blocks.FARMLAND.getDefaultState());
                                    case COARSE_DIRT:
                                        this.setBlock(itemStack, playerIn, worldIn, newPos, Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.DIRT));
                                }
                            }
                        }
                    }
                }
            }
            return EnumActionResult.SUCCESS;
        }
    }

    protected void setBlock(ItemStack stack, EntityPlayer player, World worldIn, BlockPos pos, IBlockState state) {
        // TODO: Proper sound effect, maybe some particle effects like dirt particles flying around.
        // note that the isRemote check was moved here because exiting with it seems to cancel sound
        worldIn.playSound(player, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!worldIn.isRemote) {
            ElectricItemUtils.drainPlayerEnergy(player, ModuleManager.computeModularProperty(stack, HOE_ENERGY_CONSUMPTION));
            worldIn.setBlockState(pos, state, 11);
        }
    }

    @Override
    public EnumActionResult onItemUseFirst(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        return EnumActionResult.PASS;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft) {

    }

    @Override
    public String getCategory() {
        return MuseCommonStrings.CATEGORY_TOOL;
    }

    @Override
    public String getDataName() {
        return MODULE_HOE;
    }

    @Override
    public String getUnlocalizedName() {
        return "hoe";
    }

    @Override
    public TextureAtlasSprite getIcon(ItemStack item) {
        return Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(new ItemStack(Items.GOLDEN_HOE)).getParticleTexture();
    }
}