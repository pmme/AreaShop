package me.wiefferink.areashop.handlers;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import me.wiefferink.areashop.interfaces.AreaShopInterface;
import me.wiefferink.areashop.interfaces.GeneralRegionInterface;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.entity.Player;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class WorldEditHandler7S extends WorldEditHandler7 {

	public WorldEditHandler7S(AreaShopInterface pluginInterface) {
		super(pluginInterface);
	}

	@Override
	public boolean restoreRegionBlocks(File rawFile, GeneralRegionInterface regionInterface) {
		File file = null;
		ClipboardFormat format = null;
		for (ClipboardFormat formatOption : ClipboardFormats.getAll()) {
			for (String extension : formatOption.getFileExtensions()) {
				if (new File(rawFile.getAbsolutePath() + "." + extension).exists()) {
					file = new File(rawFile.getAbsolutePath() + "." + extension);
					format = formatOption;
				}
			}
		}
		if(file == null) {
			pluginInterface.getLogger().info("Did not restore region " + regionInterface.getName() + ", schematic file does not exist: " + rawFile.getAbsolutePath());
			return false;
		}
		pluginInterface.debugI("Trying to restore region", regionInterface.getName(), " from file", file.getAbsolutePath(), "with format", format.getName());

		com.sk89q.worldedit.world.World world = null;
		if(regionInterface.getName() != null) {
			world = BukkitAdapter.adapt(regionInterface.getWorld());
		}
		if(world == null) {
			pluginInterface.getLogger().info("Did not restore region " + regionInterface.getName() + ", world not found: " + regionInterface.getWorldName());
			return false;
		}
		EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit().getEditSessionFactory().getEditSession(world, pluginInterface.getConfig().getInt("maximumBlocks"));
		// EditSession is MULTI_STAGE by default, the ReorderMode that is set by the implementation of deprecated enableQueue.
		//editSession.enableQueue();
		ProtectedRegion region = regionInterface.getRegion();
		// Get the origin and size of the region
		BlockVector3 origin = BlockVector3.at(region.getMinimumPoint().getBlockX(), region.getMinimumPoint().getBlockY(), region.getMinimumPoint().getBlockZ());

		// Read the schematic and paste it into the world
		try(Closer closer = Closer.create()) {
			FileInputStream fis = closer.register(new FileInputStream(file));
			BufferedInputStream bis = closer.register(new BufferedInputStream(fis));
			ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(file);
			if (clipboardFormat == null) {
				pluginInterface.getLogger().warning("WorldEdit could not detect format type of the schematic file:" + file.getAbsolutePath() + ", try updating WorldEdit");
				return false;
			}
			ClipboardReader reader = clipboardFormat.getReader(bis);

			//WorldData worldData = world.getWorldData();
			LocalSession session = new LocalSession(pluginInterface.getWorldEdit().getLocalConfiguration());
			Clipboard clipboard = reader.read();
			if(clipboard.getDimensions().getY() != regionInterface.getHeight()
					|| clipboard.getDimensions().getX() != regionInterface.getWidth()
					|| clipboard.getDimensions().getZ() != regionInterface.getDepth()) {
				pluginInterface.getLogger().warning("Size of the region " + regionInterface.getName() + " is not the same as the schematic to restore!");
				pluginInterface.debugI("schematic|region, x:" + clipboard.getDimensions().getX() + "|" + regionInterface.getWidth() + ", y:" + clipboard.getDimensions().getY() + "|" + regionInterface.getHeight() + ", z:" + clipboard.getDimensions().getZ() + "|" + regionInterface.getDepth());
			}
			clipboard.setOrigin(clipboard.getMinimumPoint());
			ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);
			session.setBlockChangeLimit(pluginInterface.getConfig().getInt("maximumBlocks"));
			session.setClipboard(clipboardHolder);

			// Build operation
			BlockTransformExtent extent = new BlockTransformExtent(clipboardHolder.getClipboard(), clipboardHolder.getTransform());
			ForwardExtentCopy copy = new ForwardExtentCopy(extent, clipboard.getRegion(), clipboard.getOrigin(), editSession, origin);
			copy.setCopyingEntities(false);
			copy.setTransform(clipboardHolder.getTransform());
			// Mask to region (for polygon and other weird shaped regions)
			// TODO make this more efficient (especially for polygon regions)
			if(region.getType() != RegionType.CUBOID) {
				copy.setSourceMask(new Mask() {
					@Override
					public boolean test(BlockVector3 vector) {
						return region.contains(vector);
					}

					@Override
					public Mask2D toMask2D() {
						return null;
					}
				});
			}
			Operations.completeLegacy(copy);
		} catch(MaxChangedBlocksException e) {
			pluginInterface.getLogger().warning("exceeded the block limit while restoring schematic of " + regionInterface.getName() + ", limit in exception: " + e.getBlockLimit() + ", limit passed by AreaShop: " + pluginInterface.getConfig().getInt("maximumBlocks"));
			return false;
		} catch(IOException e) {
			pluginInterface.getLogger().warning("An error occured while restoring schematic of " + regionInterface.getName() + ", enable debug to see the complete stacktrace");
			pluginInterface.debugI(ExceptionUtils.getStackTrace(e));
			return false;
		} catch (Exception e) {
			pluginInterface.getLogger().warning("crashed during restore of " + regionInterface.getName());
			pluginInterface.debugI(ExceptionUtils.getStackTrace(e));
			return false;
		}
		editSession.flushSession();

		return true;
	}

}






















