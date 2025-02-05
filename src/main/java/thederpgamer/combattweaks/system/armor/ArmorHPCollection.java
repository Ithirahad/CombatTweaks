package thederpgamer.combattweaks.system.armor;

import api.utils.game.SegmentControllerUtils;
import it.unimi.dsi.fastutil.shorts.Short2IntArrayMap;
import org.schema.common.util.StringTools;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.structurecontrol.GUIKeyValueEntry;
import org.schema.game.client.view.gui.structurecontrol.ModuleValueEntry;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ElementCollectionManager;
import org.schema.game.common.controller.elements.VoidElementManager;
import org.schema.game.common.data.element.Element;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.Timer;
import thederpgamer.combattweaks.manager.ConfigManager;

import java.util.ArrayList;

/**
 * CollectionManager for ArmorHP.
 * <p>Rather than storing each block index, this just stores the type and count for efficiency.</p>
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class ArmorHPCollection extends ElementCollectionManager<ArmorHPUnit, ArmorHPCollection, VoidElementManager<ArmorHPUnit, ArmorHPCollection>> {

	public static ArmorHPCollection getCollection(SegmentController segmentController) {
		if(!(segmentController instanceof ManagedUsableSegmentController)) return null;
		ArrayList<ElementCollectionManager<?, ?, ?>> managers = SegmentControllerUtils.getCollectionManagers((ManagedUsableSegmentController<?>) segmentController, ArmorHPCollection.class);
		for(ElementCollectionManager<?, ?, ?> manager : managers) {
			if(manager instanceof ArmorHPCollection) return (ArmorHPCollection) manager;
		}
		return null;
	}

	private final Short2IntArrayMap blockMap = new Short2IntArrayMap();
	private double currentHP;
	private double maxHP;
	private boolean flagCollectionChanged;

	public ArmorHPCollection(SegmentController segmentController, VoidElementManager<ArmorHPUnit, ArmorHPCollection> armorHPManager) {
		super(Element.TYPE_NONE, segmentController, armorHPManager);
	}

	@Override
	public int getMargin() {
		return 0;
	}

	@Override
	protected Class<ArmorHPUnit> getType() {
		return ArmorHPUnit.class;
	}

	@Override
	public boolean needsUpdate() {
		return true;
	}

	@Override
	public ArmorHPUnit getInstance() {
		return new ArmorHPUnit();
	}

	@Override
	protected void onChangedCollection() {
		if(!getSegmentController().isOnServer()) ((GameClientState) getSegmentController().getState()).getWorldDrawer().getGuiDrawer().managerChanged(this);
		flagCollectionChanged = true;
	}

	@Override
	public GUIKeyValueEntry[] getGUICollectionStats() {
		return new GUIKeyValueEntry[] {
			new ModuleValueEntry(Lng.str("HP Status"), StringTools.formatPointZero(currentHP) + "/" + StringTools.formatPointZero(maxHP) + " [" + getHPPercentage() + "%]")
		};
	}

	@Override
	public String getModuleName() {
		return Lng.str("Armor HP System");
	}

	@Override
	public void update(Timer timer) {
		if(currentHP < maxHP && maxHP > 0) flagCollectionChanged = false; //This should prevent people from just placing blocks to get their HP back
		if((flagCollectionChanged || maxHP <= 0) && getSegmentController().isFullyLoadedWithDock()) recalcHP();
	}

	public void recalcHP() {
		currentHP = 0;
		maxHP = 0;
		float armorMult = (float) ConfigManager.getSystemConfig().getDouble("armor-value-multiplier");
		for(short type : blockMap.keySet()) {
			if(type != 0) {
				int count = blockMap.get(type);
				if(count <= 0) {
					blockMap.put(type, 0);
					continue;
				}
				currentHP += (ElementKeyMap.getInfo(type).getArmorValue() * armorMult) * count;
				maxHP += (ElementKeyMap.getInfo(type).getArmorValue() * armorMult) * count;
			}
		}
		if(currentHP < 0) currentHP = 0;
		if(maxHP < 0) maxHP = 0;
		if(currentHP > maxHP) currentHP = maxHP;
		flagCollectionChanged = false;
	}

	public double getCurrentHP() {
		return currentHP;
	}

	public void setCurrentHP(double currentHP) {
		this.currentHP = Math.min(Math.max(0, currentHP), maxHP);
	}

	public double getMaxHP() {
		return maxHP;
	}

	public double getHPPercentage() {
		return currentHP / maxHP * 100;
	}

	public void addBlock(long index, short type) {
		try {
			if(rawCollection == null && type != 0) doAdd(index, type); //Dumb hack to get it to call the update method
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		blockMap.put(type, blockMap.get(type) + 1);
		flagCollectionChanged = true;
	}

	public void removeBlock(short type) {
		blockMap.put(type, blockMap.get(type) - 1);
		if(blockMap.get(type) < 0) blockMap.put(type, 0);
		flagCollectionChanged = true;
	}
}
