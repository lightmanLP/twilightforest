package twilightforest.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import twilightforest.TFGenericPacketHandler;
import twilightforest.TwilightForestMod;
import twilightforest.biomes.TFBiomeBase;
import twilightforest.item.ItemTFOreMagnet;
import twilightforest.item.TFItems;

public class BlockTFMagicLogSpecial extends BlockTFMagicLog {

    protected BlockTFMagicLogSpecial() {
        super();
        this.setCreativeTab(TFItems.creativeTab);
    }

    @Override
    public int tickRate(World world) {
        return 20;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
    }

    @Override
    public Item getItemDropped(int par1, Random rand, int par3) {
        return Item.getItemFromBlock(TFBlocks.magicLog); // change into normal magic log
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        int orient = meta & 12;
        int woodType = meta & 3;

        if (orient == 12) {
            // off blocks
            return switch (woodType) {
                default -> (side == 1 || side == 0) ? SPR_TIMETOP : SPR_TIMECLOCKOFF;
                case META_TRANS -> (side == 1 || side == 0) ? SPR_TRANSTOP : SPR_TRANSHEARTOFF;
                case META_MINE -> (side == 1 || side == 0) ? SPR_MINETOP : SPR_MINEGEMOFF;
                case META_SORT -> (side == 1 || side == 0) ? SPR_SORTTOP : SPR_SORTEYEOFF;
            };
        } else {
            return switch (woodType) {
                default -> orient == 0 && (side == 1 || side == 0) ? SPR_TIMETOP
                        : (orient == 4 && (side == 5 || side == 4) ? SPR_TIMETOP
                                : (orient == 8 && (side == 2 || side == 3) ? SPR_TIMETOP : SPR_TIMECLOCK));
                case META_TRANS -> orient == 0 && (side == 1 || side == 0) ? SPR_TRANSTOP
                        : (orient == 4 && (side == 5 || side == 4) ? SPR_TRANSTOP
                                : (orient == 8 && (side == 2 || side == 3) ? SPR_TRANSTOP : SPR_TRANSHEART));
                case META_MINE -> orient == 0 && (side == 1 || side == 0) ? SPR_MINETOP
                        : (orient == 4 && (side == 5 || side == 4) ? SPR_MINETOP
                                : (orient == 8 && (side == 2 || side == 3) ? SPR_MINETOP : SPR_MINEGEM));
                case META_SORT -> orient == 0 && (side == 1 || side == 0) ? SPR_SORTTOP
                        : (orient == 4 && (side == 5 || side == 4) ? SPR_SORTTOP
                                : (orient == 8 && (side == 2 || side == 3) ? SPR_SORTTOP : SPR_SORTEYE));
            };
        }
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random rand) {
        int meta = world.getBlockMetadata(x, y, z);

        if ((meta & 12) == 12) {
            // block is off, do not tick
            return;
        }
        if (!world.isRemote) {
            switch (meta & 3) {
                case 0 -> {
                    // tree of time effect
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.1F, 0.5F);
                    doTreeOfTimeEffect(world, x, y, z, rand);
                }
                case 1 ->
                    // tree of transformation effect
                    doTreeOfTransformationEffect(world, x, y, z, rand);
                case 2 ->
                    // miner's tree effect
                    doMinersTreeEffect(world, x, y, z, rand);
                case 3 ->
                    // sorting tree effect
                    doSortingTreeEffect(world, x, y, z, rand);
            }
        }
        world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7,
            float par8, float par9) {
        int meta = world.getBlockMetadata(x, y, z);

        int orient = meta & 12;
        int woodType = meta & 3;

        if (orient == 0) {
            // turn off
            world.setBlockMetadataWithNotify(x, y, z, woodType | 12, 3);
            return true;
        } else if (orient == 12) {
            // turn on
            world.setBlockMetadataWithNotify(x, y, z, woodType | 0, 3);
            world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
            return true;
        }
        return false;
    }

    /**
     * The tree of time adds extra ticks to blocks, so that they have twice the normal chance to get a random tick
     */
    private void doTreeOfTimeEffect(World world, int x, int y, int z, Random rand) {
        int numticks = 8 * 3 * this.tickRate(world);

        int successes = 0;

        for (int i = 0; i < numticks; i++) {
            // find a nearby block
            int dx = rand.nextInt(32) - 16;
            int dy = rand.nextInt(32) - 16;
            int dz = rand.nextInt(32) - 16;

            Block thereID = world.getBlock(x + dx, y + dy, z + dz);

            if (thereID != Blocks.air && thereID.getTickRandomly()) {
                world.scheduleBlockUpdate(x + dx, y + dy, z + dz, thereID, 20);
                thereID.updateTick(world, x + dx, y + dy, z + dz, rand);

                // System.out.println("tree of time ticked a block at " + (x + dx) + ", " + (y + dy) + ", " + (z +
                // dz) + " and the block was " + Blocks.blocksList[thereID] );

                successes++;
            }
        }

        // System.out.println("Tree of time had " + successes + " successes out of " + numticks + "
        // attempts");
    }

    /**
     * The tree of transformation transforms the biome in the area near it into the enchanted forest biome.
     *
     * TODO: also change entities
     */
    private void doTreeOfTransformationEffect(World world, int x, int y, int z, Random rand) {
        for (int i = 0; i < 1; i++) {
            int dx = rand.nextInt(32) - 16;
            int dz = rand.nextInt(32) - 16;

            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "note.harp", 0.1F, rand.nextFloat() * 2F);

            if (Math.sqrt(dx * dx + dz * dz) < 16) {
                BiomeGenBase biomeAt = world.getBiomeGenForCoords(x + dx, z + dz);

                if (biomeAt != TFBiomeBase.enchantedForest) {
                    // I wonder how possible it is to change this

                    Chunk chunkAt = world.getChunkFromBlockCoords(x + dx, z + dz);

                    chunkAt.getBiomeArray()[((z + dz) & 15) << 4
                            | ((x + dx) & 15)] = (byte) TFBiomeBase.enchantedForest.biomeID;

                    world.markBlockForUpdate((x + dx), y, (z + dz));

                    // System.out.println("Set biome at " + (x + dx) + ", " + (z + dz) + " to enchanted forest.");

                    // send chunk?!

                    if (world instanceof WorldServer) {
                        sendChangedBiome(world, x + dx, z + dz, chunkAt);
                    }

                }

            }
        }
    }

    /**
     * Send a tiny update packet to the client to inform it of the changed biome
     */
    private void sendChangedBiome(World world, int x, int z, Chunk chunkAt) {
        FMLProxyPacket message = TFGenericPacketHandler
                .makeBiomeChangePacket(x, z, (byte) TFBiomeBase.enchantedForest.biomeID);

        NetworkRegistry.TargetPoint targetPoint = new NetworkRegistry.TargetPoint(
                world.provider.dimensionId,
                x,
                128,
                z,
                128);

        TwilightForestMod.genericChannel.sendToAllAround(message, targetPoint);
        // FMLLog.info("Sent chunk update packet from tree.");

    }

    /**
     * The miner's tree generates the ore magnet effect randomly every second
     */
    private void doMinersTreeEffect(World world, int x, int y, int z, Random rand) {
        int dx = rand.nextInt(64) - 32;
        int dy = rand.nextInt(64) - 32;
        int dz = rand.nextInt(64) - 32;

        // world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.1F, 0.5F);

        int moved = ItemTFOreMagnet.doMagnet(world, x, y, z, x + dx, y + dy, z + dz);

        if (moved > 0) {
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "mob.endermen.portal", 0.1F, 1.0F);
        } else {
            // System.out.println("Miner block did not find ore at " + (x + dx) + ", " + (y + dy) + ", " + (z +
            // dz) + ", nope.");
        }
    }

    /**
     * The sorting tree finds two chests nearby and then attempts to sort a random item.
     */
    private void doSortingTreeEffect(World world, int x, int y, int z, Random rand) {
        // find all the chests nearby
        int xsearch = TwilightForestMod.sortingX;
        int ysearch = TwilightForestMod.sortingY;
        int zsearch = TwilightForestMod.sortingZ;

        ArrayList<IInventory> chests = new ArrayList<>();
        int itemCount = 0;

        for (int sx = x - xsearch; sx < x + xsearch; sx++) {
            for (int sy = y - ysearch; sy < y + ysearch; sy++) {
                for (int sz = z - zsearch; sz < z + zsearch; sz++) {
                    Block block = world.getBlock(sx, sy, sz);
                    IInventory thisChest = null;
                    if (block == Blocks.chest) {
                        thisChest = Blocks.chest.func_149951_m(world, sx, sy, sz);
                    } else if (block instanceof BlockContainer) {
                        TileEntity entity = world.getTileEntity(sx, sy, sz);
                        if (entity instanceof IInventory) {
                            thisChest = (IInventory) entity;
                        }
                    }

                    if (thisChest != null) {
                        // make sure we haven't counted this chest
                        if (thisChest != null
                                && !checkIfChestsContains(chests, (IInventory) world.getTileEntity(sx, sy, sz))) {
                            int itemsInChest = 0;

                            // count items
                            for (int i = 0; i < thisChest.getSizeInventory(); i++) {
                                if (thisChest.getStackInSlot(i) != null) {
                                    itemsInChest++;
                                    itemCount++;
                                }
                            }

                            // only add non-empty chests
                            if (itemsInChest > 0) {
                                chests.add(thisChest);
                            }
                        }
                    }
                }
            }
        }

        // FMLLog.info("Found " + chests.size() + " non-empty chests, containing " + itemCount + " items");

        // find a random item in one of the chests
        ItemStack beingSorted = null;
        int sortedChestNum = -1;
        int sortedSlotNum = -1;

        if (itemCount > 0) {
            int itemNumber = rand.nextInt(itemCount);
            int currentNumber = 0;

            for (int i = 0; i < chests.size(); i++) {
                IInventory chest = chests.get(i);
                for (int slotNum = 0; slotNum < chest.getSizeInventory(); slotNum++) {
                    ItemStack currentItem = chest.getStackInSlot(slotNum);

                    if (currentItem != null) {
                        if (currentNumber++ == itemNumber) {
                            beingSorted = currentItem;
                            sortedChestNum = i;
                            sortedSlotNum = slotNum;
                        }
                    }
                }
            }
        }

        // FMLLog.info("Decided to sort item " + beingSorted);

        if (beingSorted != null) {
            int matchChestNum = -1;
            int matchCount = 0;

            // decide where to put it, if anywhere
            for (int chestNum = 0; chestNum < chests.size(); chestNum++) {
                IInventory chest = chests.get(chestNum);
                int currentChestMatches = 0;

                for (int slotNum = 0; slotNum < chest.getSizeInventory(); slotNum++) {

                    ItemStack currentItem = chest.getStackInSlot(slotNum);
                    if (currentItem != null && isSortingMatch(beingSorted, currentItem)) {
                        currentChestMatches += currentItem.stackSize;
                    }
                }

                if (currentChestMatches > matchCount) {
                    matchCount = currentChestMatches;
                    matchChestNum = chestNum;
                }
            }

            // soooo, did we find a better match?
            if (matchChestNum >= 0 && matchChestNum != sortedChestNum) {
                IInventory moveChest = chests.get(matchChestNum);
                IInventory oldChest = chests.get(sortedChestNum);

                // is there an empty inventory slot in the new chest?
                int moveSlot = getEmptySlotIn(moveChest);

                if (moveSlot >= 0) {
                    // remove old item
                    oldChest.setInventorySlotContents(sortedSlotNum, null);

                    // add new item
                    moveChest.setInventorySlotContents(moveSlot, beingSorted);

                    // FMLLog.info("Moved sorted item " + beingSorted + " to chest " + matchChestNum + ", slot " +
                    // moveSlot);
                }
            }

            // if the stack is not full, combine items from other stacks
            if (beingSorted.stackSize < beingSorted.getMaxStackSize()) {
                for (IInventory chest : chests) {
                    for (int slotNum = 0; slotNum < chest.getSizeInventory(); slotNum++) {
                        ItemStack currentItem = chest.getStackInSlot(slotNum);

                        if (currentItem != null && currentItem != beingSorted && beingSorted.isItemEqual(currentItem)) {
                            if (currentItem.stackSize <= (beingSorted.getMaxStackSize() - beingSorted.stackSize)) {
                                chest.setInventorySlotContents(slotNum, null);
                                beingSorted.stackSize += currentItem.stackSize;
                                currentItem.stackSize = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isSortingMatch(ItemStack beingSorted, ItemStack currentItem) {
        return getCreativeTab(currentItem.getItem()).equals(getCreativeTab(beingSorted.getItem()));
    }

    private Object getCreativeTab(Item item) {
        try {
            return ObfuscationReflectionHelper.getPrivateValue(Item.class, item, 0);
        } catch (IllegalArgumentException | SecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Is the chest we're testing part of our chest list already?
     */
    private boolean checkIfChestsContains(ArrayList<IInventory> chests, IInventory testChest) {
        for (IInventory chest : chests) {
            if (chest.equals(testChest)) {
                return true;
            }

            if (chest instanceof InventoryLargeChest && ((InventoryLargeChest) chest).isPartOfLargeChest(testChest)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return an empty slot number in the chest, or -1 if the chest is full
     */
    private int getEmptySlotIn(IInventory chest) {
        for (int i = 0; i < chest.getSizeInventory(); i++) {
            if (chest.getStackInSlot(i) == null) {
                return i;
            }
        }
        return -1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    /**
     * A randomly called display update to be able to add particles or other items for display
     */
    public void randomDisplayTick(World world, int x, int y, int z, Random rand) {

    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return 15;
    }

    /**
     * returns a list of blocks with the same ID, but different meta (eg: wood returns 4 blocks)
     */
    @Override
    public void getSubBlocks(Item item, CreativeTabs par2CreativeTabs, List<ItemStack> itemList) {
        itemList.add(new ItemStack(item, 1, 0));
        itemList.add(new ItemStack(item, 1, 1));
        itemList.add(new ItemStack(item, 1, 2));
        itemList.add(new ItemStack(item, 1, 3));
    }
}
