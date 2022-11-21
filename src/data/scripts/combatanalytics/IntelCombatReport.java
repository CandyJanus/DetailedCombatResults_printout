package data.scripts.combatanalytics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combatanalytics.data.CombatResult;
import data.scripts.combatanalytics.data.Ship;
import data.scripts.combatanalytics.data.ShipStatus;
import data.scripts.combatanalytics.function.AggregateDamage;
import data.scripts.combatanalytics.function.AggregateProcessor;
import data.scripts.combatanalytics.function.DamageSet;
import data.scripts.combatanalytics.function.DamageSummary;
import data.scripts.combatanalytics.function.GroupedByShipDamage;
import data.scripts.combatanalytics.util.Helpers;
import data.scripts.combatanalytics.util.Localization;
import data.scripts.combatanalytics.util.Settings;
import data.scripts.combatanalytics.util.Sprites;
import org.apache.log4j.Logger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static data.scripts.combatanalytics.util.Helpers.INT_FORMAT;

public class IntelCombatReport extends BaseIntelPlugin {
    static final int CurrentVersion = 2; // version gets reved when we make breaking changes, versions other than this will be removed

    public static final double LowDamageCutoff = 75f;

    private static final int CELL_SIZE = 20;
    private static final int SHIP_IMAGE_SIZE = 100;
    private static final int KILL_IMAGE_SIZE = 50; // if this size changes, we'll have to change the size of the damage sprites
    private static final int CAPTAIN_IMAGE_SIZE = 35;
    private static final int OUTLINE_SIZE = 1;
    private static final int SUMMARY_BAR_OFFSET = 10;

    // limit just how many ships we display, too many could cause problems
    private static final int SHIP_COUNT_LIMIT = Settings.ShipCountLimit;

    private static final Logger log = Global.getLogger(data.scripts.combatanalytics.IntelCombatReport.class);

    private static final Color TotalColor = new Color(255, 255, 160);

    // all saved objects should be java lang objects so we can mutate our class structure as we like without breaking saves
    private final int _savedVersion; // if our saved version doesn't match our current version, delete
    private final Color _enemyFactionColor;
    private final String _enemyFactionName;
    private final String _enemyFactionId;

    // all ships, needed for total damage aggregation and what not
    public final Set<String> _combatResultIds;

    private boolean _shouldRemove = false;

    public boolean _isSimulation = false;

    public IntelCombatReport(FactionAPI enemyFaction, CombatResult combatResult) {
        _savedVersion = CurrentVersion;
        super.timestamp = combatResult.engagementEndTime;
        _enemyFactionColor = enemyFaction.getColor();
        _enemyFactionName = combatResult.faction;
        _enemyFactionId = enemyFaction.getId();
        _combatResultIds = new HashSet<>(Collections.singletonList(combatResult.combatId));

        _shouldRemove = !isTransientDataValid();
    }

    public IntelCombatReport(Color color, CombatResult combatResult) {
        _savedVersion = CurrentVersion;
        super.timestamp = combatResult.engagementEndTime;
        _enemyFactionColor = color;
        _enemyFactionName = combatResult.faction;
        _enemyFactionId = null;
        _combatResultIds = new HashSet<>(Collections.singletonList(combatResult.combatId));

        _shouldRemove = !isTransientDataValid();
        _isSimulation = true;
    }

    public IntelCombatReport(CombatResult... combatResults) {
        _savedVersion = CurrentVersion;
        _enemyFactionColor = TotalColor;
        _enemyFactionId = null;

        Set<String> factions = new HashSet<>();
        _combatResultIds = new HashSet<>();
        for(CombatResult combatResult : combatResults){
            _combatResultIds.add(combatResult.combatId);
            factions.add(combatResult.faction);
        }

        _enemyFactionName = String.format(Localization.AggregateFactionName, factions.size(), combatResults.length);

        _shouldRemove = !isTransientDataValid();
    }

    public boolean isValid(){
        return isTransientDataValid();
    }

    private boolean isTransientDataValid(){
        return getDataIfExists().size() > 0;
    }

    private List<CombatResult> getDataIfExists(){
        if(_combatResultIds == null || _combatResultIds.size() == 0){
            log.info("Unable to locate _combatResultIds");
            return new ArrayList<>();
        }

        List<CombatResult> ret = SerializationManager.getSavedCombatResults(_combatResultIds);

        if(ret.size() != _combatResultIds.size() || ret.size() == 0){
            return new ArrayList<>();
        }

        return ret;
    }

    private RenderCombatData getTransientData(){

        RenderCombatData ret = new RenderCombatData();
        ret.CombatResults = getDataIfExists();

        if(ret.CombatResults.size() == 0) {
            log.info("Unable to locate all CombatResults, expected " + _combatResultIds.size());
            return ret;
        }

        GroupedByShipDamage[] stats = AggregateProcessor.aggregateWeaponDamageByShip(ret.CombatResults);
        for (GroupedByShipDamage stat : stats) {
            if (stat.ship.owner == 0 && isOkToReportOnHull(stat.ship.hullSize)) { // only care about player ships
                ret.AllPlayerShipStats.add(stat);
            } else if(stat.ship.owner == 1 && isOkToReportOnHull(stat.ship.hullSize)){
                ret.EnemyShipStats.add(stat);
            }
        }

        //sort ships by total damage
        Collections.sort(ret.AllPlayerShipStats);
        Collections.sort(ret.EnemyShipStats);

        ret.DisplayedPlayerShipStats = ret.AllPlayerShipStats;
        if(ret.DisplayedPlayerShipStats.size() > SHIP_COUNT_LIMIT){
            ret.DisplayedPlayerShipStats = ret.DisplayedPlayerShipStats.subList(0, SHIP_COUNT_LIMIT);
        }

        for(GroupedByShipDamage gsd : ret.DisplayedPlayerShipStats) {
            for (Map.Entry<String, DamageSet> nameDamage : new ArrayList<>(gsd.weaponNameToDamage.entrySet())) {
                // some unknown damages can be low, ignore as it just clutters
                if (nameDamage.getValue().aggregateShips().totalAllDamage() < LowDamageCutoff
                   && nameDamage.getValue().aggregateFighters().getMajorityKills().size() == 0
                   && nameDamage.getValue().aggregateMissiles().getMajorityKills().size() == 0
                ) {
                    gsd.weaponNameToDamage.remove(nameDamage.getKey());
                }
            }
        }

        return ret;
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        if(_isSimulation){
            info.addPara(Localization.SimulatorInfo, 0f, Misc.getTextColor());
        } else if(_combatResultIds.size() == 1) {
            info.addPara(Localization.SingleCombatInfo, 0f, Misc.getTextColor(), _enemyFactionColor, _enemyFactionName);
        } else {
            info.addPara(Localization.MultiIntelInfo + _enemyFactionName, 0f, Misc.getTextColor(), _enemyFactionColor, _enemyFactionName);
        }
    }

    @Override
    public boolean hasSmallDescription() {
        return false;
    }

    @Override
    public boolean hasLargeDescription() {
        return true;
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        //todo should we/can we add a "dismiss" button?
        long start = System.currentTimeMillis();
        try {
            if(!isTransientDataValid()){
                _shouldRemove = true;
                return;
            }

            RenderCombatData data = getTransientData();

            TooltipMakerAPI outer = panel.createUIElement(width, height, true);
            CustomPanelAPI inner = panel.createCustomPanel(width, computeTotalHeight(data.DisplayedPlayerShipStats), null); // this height controls the scroll
            outer.addCustom(inner, 0);

            int currentHeight = 0;
            currentHeight = buildEnemyFleetImageSet( currentHeight, width, inner, data);

            currentHeight = renderSummaryDetails(currentHeight, width, inner, data);

            for(GroupedByShipDamage sas : data.DisplayedPlayerShipStats){
                currentHeight = renderShipDetails(currentHeight, width, sas, inner, data);
            }

            currentHeight = renderLegend(currentHeight, width, inner);

            outer.getPosition().setSize(width, currentHeight);
            panel.addUIElement(outer).inTL(0, 0);
        } catch (Throwable e){
            log.error("Exception building IntelCombatReport", e);
            Helpers.printErrorMessage("Exception building Intel Report");
        } finally {
            long end = System.currentTimeMillis();
            log.info("IntelCombatReport.createLargeDescription time in MS: " + (end - start));
        }
    }

    // summary data at the top
    private int renderSummaryDetails(int startHeight, float width, CustomPanelAPI panel, RenderCombatData data){
        TooltipMakerAPI summary = panel.createUIElement(width - SUMMARY_BAR_OFFSET, computeSummaryHeight(data.DisplayedPlayerShipStats), false);
        summary.addSectionHeading(Localization.SummaryDetailsHeading, Alignment.LMID, 10);

        DamageSet totalDamage = new DamageSet(Localization.TotalDamage);
        for(GroupedByShipDamage sa : data.AllPlayerShipStats){
            totalDamage.merge(sa.allDamages);
        }

        String shipsDisplayed = "";
        if(data.DisplayedPlayerShipStats.size() != data.AllPlayerShipStats.size()){
            shipsDisplayed += ", " + String.format(Localization.DisplayingTop, SHIP_COUNT_LIMIT);
        }

        String title = String.format(Localization.PlayerResultsText,
                    AggregateProcessor.getFleetSize(data.CombatResults, 0),
                    AggregateProcessor.getFleetDpValue(data.CombatResults, 0),
                    INT_FORMAT.format(totalDamage.aggregateShips().totalRealDamage()),
                    getPlayerFleetGoal(data.CombatResults),
                    shipsDisplayed
                );

        title = "%s" + title;

        summary.addPara(title, 10, Misc.getTextColor(), Misc.getBasePlayerColor(), "Player");
        buildSummaryStatsTable(summary, width - 45, data);
        panel.addUIElement(summary).inTL(0, startHeight);

        return computeSummaryHeight(data.DisplayedPlayerShipStats) + startHeight;
    }

    // a single ship details row, 1 per friendly ship active in combat
    private int renderShipDetails(int startHeight, float width, GroupedByShipDamage sas, CustomPanelAPI panel, RenderCombatData data){

        String heading = " " + sas.name;
        if(sas.ship.hasCaptain()){
            heading += " ";
            heading += Localization.CaptainedBy;
            heading += " ";
            heading += sas.ship.captain;
        }
        TooltipMakerAPI header = panel.createUIElement(width - SUMMARY_BAR_OFFSET, 25, false);
        header.addSectionHeading(heading, Alignment.LMID, 0);
        panel.addUIElement(header).inTL(0, startHeight);
        startHeight += 20;

        // a row with three cells: Image, Kill Summary, Received

        // render ship image (left)
        float shipSizeScalar = hullSizeScalar(sas.ship.hullSize);
        float shipImageSize = SHIP_IMAGE_SIZE * shipSizeScalar;
        float delta = SHIP_IMAGE_SIZE - shipImageSize;
        TooltipMakerAPI shipImage = panel.createUIElement(shipImageSize, shipImageSize, false);
        shipImage.addImage(Global.getSettings().getHullSpec(sas.ship.hullId).getSpriteName(), shipImageSize, shipImageSize, 10);
        panel.addUIElement(shipImage).inTL(delta/2, startHeight+delta/2);

        if(sas.ship.hasCaptain() && sas.ship.captainSprite.length() > 0){
            TooltipMakerAPI captainOutline = panel.createUIElement(CAPTAIN_IMAGE_SIZE + OUTLINE_SIZE * 2, CAPTAIN_IMAGE_SIZE + OUTLINE_SIZE * 2, false);
            captainOutline.addImage(Sprites.Outline, CAPTAIN_IMAGE_SIZE + OUTLINE_SIZE * 2, CAPTAIN_IMAGE_SIZE + OUTLINE_SIZE * 2, 10);
            panel.addUIElement(captainOutline).inTL(-OUTLINE_SIZE, startHeight - OUTLINE_SIZE);

            TooltipMakerAPI captainImage = panel.createUIElement(CAPTAIN_IMAGE_SIZE, CAPTAIN_IMAGE_SIZE, false);
            captainImage.addImage(sas.ship.captainSprite, CAPTAIN_IMAGE_SIZE, CAPTAIN_IMAGE_SIZE, 10);
            panel.addUIElement(captainImage).inTL(0, startHeight);
        }

        if(isPlayerShip(sas.ship)){
            TooltipMakerAPI playerIcon = panel.createUIElement(CAPTAIN_IMAGE_SIZE, CAPTAIN_IMAGE_SIZE, false);
            playerIcon.addImage(Sprites.PlayerShipIndicator, CAPTAIN_IMAGE_SIZE, CAPTAIN_IMAGE_SIZE, 10);
            panel.addUIElement(playerIcon).inTL(0, startHeight + CAPTAIN_IMAGE_SIZE + 3);
        }

        if(sas.ship.status != ShipStatus.OK && sas.ship.status != ShipStatus.NOT_FIELDED){
            String sprite = "";
            switch(sas.ship.status){

                case RETREATED: sprite = Sprites.Retreated;
                    break;
                case DISABLED: sprite = Sprites.Disabled;
                    break;
                case DESTROYED: sprite = Sprites.Destroyed;
                    break;
            }

            TooltipMakerAPI xImage = panel.createUIElement(SHIP_IMAGE_SIZE -30, SHIP_IMAGE_SIZE -30, false);
            xImage.addImage(sprite, SHIP_IMAGE_SIZE -30, SHIP_IMAGE_SIZE -30, 10);
            panel.addUIElement(xImage).inTL(15, startHeight+15);
        }

        // these are fixed width
        float killGridWidth = 165;
        float receivedGridWidth = 430;

        // images get everything else
        float killImagesWidth = width - killGridWidth - receivedGridWidth - SHIP_IMAGE_SIZE - 25;

        // render kills (center)
        TooltipMakerAPI killGrid = panel.createUIElement(killGridWidth, computeShipGridHeight(), false);
        buildKillsGrid(killGrid, sas, killGridWidth-10);
        panel.addUIElement(killGrid).inTL(SHIP_IMAGE_SIZE, startHeight);

        // render our list of ships killed
        buildSingleShipKillsImageSet(panel, sas, SHIP_IMAGE_SIZE + killGridWidth, startHeight, killImagesWidth - 10);

        // render received damage (center)
        TooltipMakerAPI receivedGrid = panel.createUIElement(receivedGridWidth, computeShipGridHeight(), false);
        buildReceivedGrid(receivedGrid, sas, data.AllPlayerShipStats, receivedGridWidth - 10);
        panel.addUIElement(receivedGrid).inTL(SHIP_IMAGE_SIZE + killGridWidth + killImagesWidth, startHeight);
        startHeight += computeShipGridHeight();

        TooltipMakerAPI weaponStats = panel.createUIElement(width, computeWeaponDetailsHeight(sas), false);
        buildWeaponStatsTable(weaponStats, new ArrayList<>(sas.weaponNameToDamage.values()), width - 40);
        panel.addUIElement(weaponStats).inTL(0, startHeight);
        startHeight += computeWeaponDetailsHeight(sas);

        return startHeight;
    }

    private int computeTotalHeight(List<GroupedByShipDamage> shipAggregateStats){
        int ret = computeSummaryHeight(shipAggregateStats);

        ret += computeEnemyFleetGridHeight();

        for(GroupedByShipDamage sas : shipAggregateStats){
            ret += computeWeaponDetailsHeight(sas) + computeShipGridHeight() + 20;
        }

        ret += computeLegendHeight();

        return ret + 50;
    }

    private int computeEnemyFleetGridHeight(){
        return KILL_IMAGE_SIZE * 4 + 70;
    }

    private int computeShipGridHeight(){
        return  SHIP_IMAGE_SIZE + 5; // buffer so we're not right next to the image
    }

    private int computeLegendHeight(){
        return 160;
    }

    private int computeWeaponDetailsHeight(GroupedByShipDamage shipAggregateStats){
        return  CELL_SIZE * 2 + shipAggregateStats.weaponNameToDamage.size() * CELL_SIZE + 50;
    }

    private int computeSummaryHeight(List<GroupedByShipDamage> shipAggregateStats){
        return 4 * CELL_SIZE // text and table header, total row
                + CELL_SIZE * shipAggregateStats.size() // just a table
                + 35; // spacing between summary and details
    }

    // this is the grid at the top
    public void buildSummaryStatsTable(TooltipMakerAPI surface, float tableWidth, RenderCombatData data) {

        surface.beginTable(Global.getSector().getPlayerFaction(), CELL_SIZE,
                Localization.ShipName, 0.15f * tableWidth,
                Localization.PilotName, 0.14f * tableWidth,
                Localization.Class, 0.11f * tableWidth,
                Localization.SoloKills, 0.06f * tableWidth,
                Localization.KillAssists, 0.06f * tableWidth,
                Localization.ProportionalDPDestroyed, 0.08f * tableWidth,
                Localization.PctOfTotalDmg, 0.06f * tableWidth,
                Localization.HullPct, 0.06f * tableWidth,
                Localization.ShieldDmg, 0.07f * tableWidth,
                Localization.ArmorDmg, 0.07f * tableWidth,
                Localization.HullDmg, 0.07f * tableWidth,
                Localization.EmpDmg, 0.07f * tableWidth);

        // total row comes first
        int totalSoloKills = 0;
        int totalAssists = 0;
        DamageSet totalDeliveredDamage = new DamageSet("totalDeliveredDamage");
        for (GroupedByShipDamage sgd : data.AllPlayerShipStats){
            totalDeliveredDamage.merge(sgd.allDamages);

            DamageSet shipDamageDealt = sgd.allDamages;
            AggregateDamage damageToShips = shipDamageDealt.aggregateShips();
            totalSoloKills += damageToShips.getSoloKills().size();
            totalAssists += damageToShips.getAssists().size();
        }

        AggregateDamage totalDamage = totalDeliveredDamage.aggregateShips();
        surface.addRow(
                Alignment.MID, TotalColor, Localization.Total.toUpperCase(),
                Alignment.LMID, TotalColor, "",
                Alignment.LMID, TotalColor, "",
                Alignment.MID, TotalColor, INT_FORMAT.format(totalSoloKills),
                Alignment.MID, TotalColor, INT_FORMAT.format(totalAssists),
                Alignment.MID, TotalColor, INT_FORMAT.format(totalDamage.getAllProRataDeploymentPointsDestroyed()),
                Alignment.MID, TotalColor, "100",
                Alignment.MID, TotalColor, "",
                Alignment.RMID, TotalColor, INT_FORMAT.format(totalDamage.getShieldDamage()),
                Alignment.RMID, TotalColor, INT_FORMAT.format(totalDamage.getArmorDamage()),
                Alignment.RMID, TotalColor, INT_FORMAT.format(totalDamage.getHullDamage()),
                Alignment.RMID, TotalColor, INT_FORMAT.format(totalDamage.getEmpDamage())
        );

        // for each ship
        for (GroupedByShipDamage sas : data.DisplayedPlayerShipStats) {
            DamageSet shipDamageDealt = sas.allDamages;
            AggregateDamage damageToShips = shipDamageDealt.aggregateShips();

            //defend against div0
            double pctDmg = (damageToShips.totalRealDamage() / totalDamage.totalRealDamage()) * 100;
            if(Double.isNaN(pctDmg) || Double.isInfinite(pctDmg)){
                pctDmg = 0;
            }

            surface.addRow(
                    Alignment.LMID, Misc.getTextColor(), sas.ship.name,
                    Alignment.LMID, Misc.getTextColor(), sas.ship.hasCaptain() ? sas.ship.captain : "",
                    Alignment.LMID, Misc.getTextColor(), sas.ship.hullClass,
                    Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getSoloKills().size()),
                    Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getAssists().size()),
                    Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getAllProRataDeploymentPointsDestroyed()),
                    Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(pctDmg),
                    Alignment.MID, getColorForHullDamage(sas.ship.getRemainingHullPct()), INT_FORMAT.format(sas.ship.getRemainingHullPct() * 100)+"%",
                    Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getShieldDamage()),
                    Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getArmorDamage()),
                    Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getHullDamage()),
                    Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getEmpDamage())
            );
        }

        surface.addTable("", 0, 10);
        surface.addPara("", 0);
    }

    public static void buildWeaponStatsTable(TooltipMakerAPI surface, List<DamageSet> weaponResults, float tableWidth) {
        Collections.sort(weaponResults, new Comparator<DamageSet>() {
            @Override
            public int compare(DamageSet o1, DamageSet o2) {
                int ret = Double.compare(o2.aggregateShips().totalRealDamage(), o1.aggregateShips().totalRealDamage());
                if(ret == 0){
                    ret = o1.groupName.compareTo(o2.groupName);
                }

                return ret;
            }
        });

//        surface.addPara("Weapon Performance (damage)", 5);
        surface.beginTable(Global.getSector().getPlayerFaction(), CELL_SIZE,
                Localization.WeaponName, 0.30f * tableWidth,
                Localization.Total, 0.12f * tableWidth,
                Localization.Shield, 0.09f * tableWidth,
                Localization.Armor, 0.08f * tableWidth,
                Localization.Hull, 0.09f * tableWidth,
                Localization.Emp, 0.08f * tableWidth,
                Localization.Hits, 0.06f * tableWidth,
                Localization.PctDmg, 0.06f * tableWidth,
                Localization.Fighter, 0.06f * tableWidth,
                Localization.Missile, 0.06f * tableWidth);

        // total row comes first
        DamageSet allDeliveredDamage = new DamageSet("allDeliveredDamage");
        for (DamageSet weaponDamage : weaponResults){
            allDeliveredDamage.merge(weaponDamage);
        }
        AggregateDamage damage = allDeliveredDamage.aggregateShips();
        AggregateDamage fighterDamage = allDeliveredDamage.aggregateFighters();
        AggregateDamage missileKills = allDeliveredDamage.aggregateMissiles();

        surface.addRow(
                Alignment.MID, TotalColor, Localization.Total.toUpperCase(),
                Alignment.RMID, TotalColor, INT_FORMAT.format(damage.totalRealDamage()),
                Alignment.RMID, TotalColor, INT_FORMAT.format(damage.getShieldDamage()),
                Alignment.RMID, TotalColor, INT_FORMAT.format(damage.getArmorDamage()),
                Alignment.RMID, TotalColor, INT_FORMAT.format(damage.getHullDamage()),
                Alignment.RMID, TotalColor, INT_FORMAT.format(damage.getEmpDamage()),
                Alignment.RMID, TotalColor, INT_FORMAT.format(damage.getHitCount()),
                Alignment.MID, TotalColor, "100",
                Alignment.MID, TotalColor, INT_FORMAT.format(fighterDamage.getMajorityKills().size()),
                Alignment.MID, TotalColor, INT_FORMAT.format(missileKills.getMajorityKills().size())
        );

        for (DamageSet wd : weaponResults) {
            AggregateDamage damageToShips = wd.aggregateShips();
            AggregateDamage damageToFighters = wd.aggregateFighters();
            AggregateDamage damageToMissiles = wd.aggregateMissiles();

            //defend against div0
            double pctDmg = (damageToShips.totalRealDamage() / damage.totalRealDamage()) * 100;
            if(Double.isNaN(pctDmg) || Double.isInfinite(pctDmg)){
                pctDmg = 0;
            }

            surface.addRow(
                    Alignment.LMID, Misc.getTextColor(), wd.groupName,
                    Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.totalRealDamage()),
                    Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getShieldDamage()),
                    Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getArmorDamage()),
                    Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getHullDamage()),
                    Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getEmpDamage()),
                    Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(damageToShips.getHitCount()),
                    Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(pctDmg),
                    Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(damageToFighters.getMajorityKills().size()),
                    Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(damageToMissiles.getMajorityKills().size())
            );
        }

        surface.addTable("", 0, 10);
        surface.addPara("", 0);
    }

    public static void buildKillsGrid(TooltipMakerAPI surface, GroupedByShipDamage sas, float tableWidth) {
        surface.beginTable(Global.getSector().getPlayerFaction(), CELL_SIZE * 1.5f,
                Localization.KillType, .41f * tableWidth,
                Localization.Total, .3f * tableWidth,
                Localization.DP, .3f * tableWidth);

        AggregateDamage shipDamage = sas.allDamages.aggregateShips();

        surface.addRow(
                Alignment.MID, Misc.getTextColor(), Localization.Solo,
                Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(shipDamage.getSoloKills().size()),
                Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(shipDamage.getSoloProRataDeploymentPointsDestroyed())
        );
        surface.addRow(
                Alignment.MID, Misc.getTextColor(), Localization.Assist,
                Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(shipDamage.getAssists().size()),
                Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(shipDamage.getAssistProRataDeploymentPointsDestroyed())
        );
        surface.addTable("", 0, 10);
        surface.addPara("", 0);
    }

    public static void buildSingleShipKillsImageSet(CustomPanelAPI panel, GroupedByShipDamage sas, float xPad, float yPad, float tableWidth) {
        AggregateDamage damageToOtherShips = sas.allDamages.aggregateShips();
        drawShipLine(damageToOtherShips.getSoloKills(), panel, xPad, yPad, tableWidth, false);
        drawShipLine(damageToOtherShips.getAssists(), panel, xPad, yPad + KILL_IMAGE_SIZE, tableWidth, true);
    }

    public int buildEnemyFleetImageSet(int startHeight, float tableWidth, CustomPanelAPI panel, RenderCombatData data) {
        DamageSet totalDamage = new DamageSet(Localization.TotalDamage);
        for(GroupedByShipDamage sa : data.EnemyShipStats){
            totalDamage.merge(sa.allDamages);
        }

        TooltipMakerAPI summary = panel.createUIElement(tableWidth - SUMMARY_BAR_OFFSET, startHeight, false);
        summary.addSectionHeading(Localization.EnemyFleetStatusTitle, Alignment.LMID, 10);
        String fleetName = data.CombatResults.size() == 1 ? data.CombatResults.get(0).fleetName : Localization.AggregateFleetName;
        String title = "%s " + String.format(Localization.EnemyResultsText,
                fleetName , AggregateProcessor.getFleetSize(data.CombatResults, 1), AggregateProcessor.getFleetDpValue(data.CombatResults, 1), Misc.getAgoStringForTimestamp(timestamp),
                    INT_FORMAT.format(totalDamage.aggregateShips().totalRealDamage()), INT_FORMAT.format(AggregateProcessor.getCombatDuration(data.CombatResults)),
                getEnemyFleetGoal(data.CombatResults));

        summary.addPara(title, 10, Misc.getTextColor(), _enemyFactionColor, _enemyFactionName);
        panel.addUIElement(summary).inTL(0, startHeight);


        int yPad = startHeight + 45;
        int xPad = 10;
        Map<Ship, DamageSummary> toDraw = new HashMap<>();

        final int textWidth = 80;

        List<Ship> allShips = AggregateProcessor.getAllShips(data.CombatResults, 1);

        // destroyed
        TooltipMakerAPI destroyedDescArea = panel.createUIElement(textWidth, KILL_IMAGE_SIZE, false);
        destroyedDescArea.addPara(Localization.Destroyed, Misc.getTextColor(), 10);
        panel.addUIElement(destroyedDescArea).inTL(5, yPad + 5);

        for(Ship s : allShips){
            if(s.owner == 1 && s.status == ShipStatus.DESTROYED && isOkToReportOnHull(s.hullSize)){
                toDraw.put(s, new DamageSummary());
            }
        }
        drawShipLine(toDraw, panel, xPad + textWidth, yPad, tableWidth - textWidth - 30, false);
        yPad += KILL_IMAGE_SIZE + 5;

        // disabled
        TooltipMakerAPI disabledDescArea = panel.createUIElement(textWidth, KILL_IMAGE_SIZE, false);
        disabledDescArea.addPara(Localization.Disabled, Misc.getTextColor(), 10);
        panel.addUIElement(disabledDescArea).inTL(5, yPad + 5);

        toDraw.clear();
        for(Ship s : allShips){
            if(s.owner == 1 && s.status == ShipStatus.DISABLED){
                toDraw.put(s, new DamageSummary());
            }
        }
        drawShipLine(toDraw, panel, xPad + textWidth, yPad, tableWidth - textWidth - 30, false);
        yPad += KILL_IMAGE_SIZE + 5;

        // retreated
        TooltipMakerAPI retreatedDescArea = panel.createUIElement(textWidth, KILL_IMAGE_SIZE, false);
        retreatedDescArea.addPara(Localization.Retreated, Misc.getTextColor(), 10);
        panel.addUIElement(retreatedDescArea).inTL(5, yPad + 5);

        toDraw.clear();
        for(Ship s : allShips){
            if(s.owner == 1 && s.status == ShipStatus.RETREATED){
                DamageSummary ds =  new DamageSummary();
                ds.hullDamage = s.maxHp - s.remainingHp;
                ds.pctOfDamageDoneToTarget = 1d -s.getRemainingHullPct();
                toDraw.put(s, ds);
            }
        }
        drawShipLine(toDraw, panel, xPad + textWidth, yPad, tableWidth - textWidth - 30, true);
        yPad += KILL_IMAGE_SIZE + 5;

        // reserved
        TooltipMakerAPI reservedDescArea = panel.createUIElement(textWidth, KILL_IMAGE_SIZE, false);
        reservedDescArea.addPara(Localization.Reserved, Misc.getTextColor(), 10);
        panel.addUIElement(reservedDescArea).inTL(5, yPad + 5);

        toDraw.clear();
        for(Ship s : allShips){
            if(s.owner == 1 && s.status == ShipStatus.NOT_FIELDED){
                toDraw.put(s, new DamageSummary());
            }
        }
        drawShipLine(toDraw, panel, xPad + textWidth, yPad, tableWidth - textWidth - 30, false);
        yPad += KILL_IMAGE_SIZE + 5;

        return startHeight + computeEnemyFleetGridHeight();
    }

    public static void drawShipLine(Map<Ship, DamageSummary> toDraw, CustomPanelAPI panel, float xPad, float yPad, float maxWidth, boolean drawDamage){
        List<Ship> ships = new ArrayList<>(toDraw.keySet());
        Collections.sort(ships, new Comparator<Ship>() {
            @Override
            public int compare(Ship o1, Ship o2) { // bigger ships first in the list
                int compare = Integer.compare(o2.deploymentPoints, o1.deploymentPoints);
                if(compare == 0){
                    compare = o1.hullId.compareTo(o2.hullId);
                }

                return compare;
            }
        });

        float drawnWidth = 0f;
        for(Ship s : ships){
            double dmgPct = toDraw.get(s).pctOfDamageDoneToTarget;
            float shipSizeScalar = hullSizeScalar(s.hullSize);
            float killImageSize = KILL_IMAGE_SIZE * shipSizeScalar;
            float delta = KILL_IMAGE_SIZE - killImageSize;

            TooltipMakerAPI shipImage = panel.createUIElement(killImageSize, killImageSize, false);
            shipImage.addImage(Global.getSettings().getHullSpec(s.hullId).getSpriteName(), killImageSize, killImageSize, 5);
            panel.addUIElement(shipImage).inTL(xPad + delta/2, yPad+delta/2);

            if(drawDamage){
                TooltipMakerAPI xImage = panel.createUIElement(KILL_IMAGE_SIZE, KILL_IMAGE_SIZE, false);
                xImage.addImage(Sprites.getDamageSpriteForDmgPct(dmgPct), KILL_IMAGE_SIZE, KILL_IMAGE_SIZE, 5);
                panel.addUIElement(xImage).inTL(xPad, yPad);
            }

            xPad += KILL_IMAGE_SIZE;
            drawnWidth += KILL_IMAGE_SIZE;
            if(drawnWidth + KILL_IMAGE_SIZE > maxWidth){
                break;
            }
        }
    }

    public static void buildReceivedGrid(TooltipMakerAPI surface, GroupedByShipDamage sas, List<GroupedByShipDamage> allShipDamages, float tableWidth) {
        AggregateDamage totalReceivedDamage = new AggregateDamage("totalReceivedDamage");
        for (GroupedByShipDamage sgd : allShipDamages) {
            totalReceivedDamage.merge(sgd.receivedSummary);
        }

        surface.addPara(Localization.ReceivedSummary, 5);
        surface.beginTable(Global.getSector().getPlayerFaction(), CELL_SIZE,
                Localization.PctDmg, 0.15f * tableWidth,
                Localization.Hull, 0.11f * tableWidth,
                Localization.Shield, 0.17f * tableWidth,
                Localization.Armor, 0.16f * tableWidth,
                Localization.Hull, 0.16f * tableWidth,
                Localization.Emp, 0.16f * tableWidth,
                Localization.Hits, 0.10f * tableWidth
        );

        //defend against div0
        double pctDmg = (sas.receivedSummary.totalRealDamage() / totalReceivedDamage.totalRealDamage()) * 100;
        if(Double.isNaN(pctDmg) || Double.isInfinite(pctDmg)){
            pctDmg = 0;
        }

        surface.addRow(
                Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(pctDmg),
                Alignment.MID, getColorForHullDamage(sas.ship.getRemainingHullPct()), INT_FORMAT.format(sas.ship.getRemainingHullPct() * 100)+"%",
                Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(sas.receivedSummary.getShieldDamage()),
                Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(sas.receivedSummary.getArmorDamage()),
                Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(sas.receivedSummary.getHullDamage()),
                Alignment.RMID, Misc.getTextColor(), INT_FORMAT.format(sas.receivedSummary.getEmpDamage()),
                Alignment.MID, Misc.getTextColor(), INT_FORMAT.format(sas.receivedSummary.getHitCount())
        );
        surface.addTable("", 0, 10);
        surface.addPara("", 0);
    }

    private int renderLegend(int startHeight, float width, CustomPanelAPI panel){

        String soloDesc =   String.format(Localization.SoloDesc, INT_FORMAT.format(AggregateDamage.SoloThreshold * 100));
        String assistDesc = String.format(Localization.AssistDesc, INT_FORMAT.format(AggregateDamage.AssistThreshold * 100));
        String proRataDpDesc =  Localization.ProRataDpDesc;
        String damageDesc =  Localization.DamageDesc;

        TooltipMakerAPI legend = panel.createUIElement(width - SUMMARY_BAR_OFFSET, computeLegendHeight(), false);
        legend.addSectionHeading(Localization.LegendName, Alignment.LMID, 10);
        panel.addUIElement(legend).inTL(0, startHeight);
        startHeight += 20;


        legend.addPara(soloDesc, Misc.getTextColor(), 10);
        legend.addPara(assistDesc, Misc.getTextColor(), 10);
        legend.addPara(proRataDpDesc, Misc.getTextColor(), 10);

        startHeight += 65;

        float exampleImageSize = 15;
        TooltipMakerAPI damageImage = panel.createUIElement(exampleImageSize, exampleImageSize, false);
        damageImage.addImage(Sprites.getDamageSpriteForDmgPct(1d), exampleImageSize, exampleImageSize);
        panel.addUIElement(damageImage).inTL(0, startHeight);

        TooltipMakerAPI damageDescArea = panel.createUIElement(width - exampleImageSize, exampleImageSize, false);
        damageDescArea.addPara(damageDesc, Misc.getTextColor(), 10);
        panel.addUIElement(damageDescArea).inTL(5 + exampleImageSize, startHeight + 5);


        startHeight += 20;
        TooltipMakerAPI destroyedImage = panel.createUIElement(exampleImageSize, exampleImageSize, false);
        destroyedImage.addImage(Sprites.Destroyed, exampleImageSize, exampleImageSize);
        panel.addUIElement(destroyedImage).inTL(0, startHeight);

        TooltipMakerAPI destroyedDescArea = panel.createUIElement(width - exampleImageSize, exampleImageSize, false);
        destroyedDescArea.addPara(Localization.Destroyed, Misc.getTextColor(), 10);
        panel.addUIElement(destroyedDescArea).inTL(5 + exampleImageSize, startHeight + 5);

        startHeight += 20;
        TooltipMakerAPI disabledImage = panel.createUIElement(exampleImageSize, exampleImageSize, false);
        disabledImage.addImage(Sprites.Disabled, exampleImageSize, exampleImageSize);
        panel.addUIElement(disabledImage).inTL(0, startHeight);

        TooltipMakerAPI disabledDescArea = panel.createUIElement(width - exampleImageSize, exampleImageSize, false);
        disabledDescArea.addPara(Localization.Disabled, Misc.getTextColor(), 10);
        panel.addUIElement(disabledDescArea).inTL(5 + exampleImageSize, startHeight + 5);

        startHeight += 20;
        TooltipMakerAPI retreatedImage = panel.createUIElement(exampleImageSize, exampleImageSize, false);
        retreatedImage.addImage(Sprites.Retreated, exampleImageSize, exampleImageSize);
        panel.addUIElement(retreatedImage).inTL(0, startHeight);

        TooltipMakerAPI retreatedDescArea = panel.createUIElement(width - exampleImageSize, exampleImageSize, false);
        retreatedDescArea.addPara(Localization.Retreated, Misc.getTextColor(), 10);
        panel.addUIElement(retreatedDescArea).inTL(5 + exampleImageSize, startHeight + 5);

        return startHeight + 40;
    }

    private boolean isPlayerShip(Ship ship){
        return Global.getSector().getPlayerPerson().getName().getFullName().equalsIgnoreCase(ship.captain);
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public String getSortString() {
        return _isSimulation ? Localization.SimulatorReportName : Localization.CombatReportName;
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        Global.getSector().getCampaignUI().showCoreUITab(CoreUITabId.REFIT, buttonId);
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("detailedcombatresults", "combat_details_icon");
    }

    @Override
    public boolean shouldRemoveIntel() {
        float days = Global.getSector().getClock().getElapsedDaysSince(timestamp);
        return _savedVersion != CurrentVersion
                || _shouldRemove
                || (_isSimulation && days >= Settings.SimulationResultLifetimeInDays)
                || (days >= Settings.CombatResultLifetimeInDays && _combatResultIds.size() == 1)
                || (days >= Settings.AggregateResultLifetimeInDays && _combatResultIds.size() > 1);
    }

    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        if(_isSimulation){
            tags.add(Localization.SimulatorReportTag);
        } else {
            tags.add(Localization.CombatReportTag);
        }
        if(_enemyFactionId != null && _enemyFactionId.length() > 0) {
            tags.add(_enemyFactionId);
        }
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntelCombatReport that = (IntelCombatReport) o;

        return _savedVersion == that._savedVersion &&
                _combatResultIds.equals(that._combatResultIds);
    }

    @Override
    public int hashCode(){
        return _savedVersion + _combatResultIds.hashCode();
    }

    private static float hullSizeScalar(ShipAPI.HullSize hullSize){
        switch (hullSize){
            case DEFAULT:
                return .10f;
            case FIGHTER:
                return .20f;
            case FRIGATE:
                return .45f;
            case DESTROYER:
                return .64f;
            case CRUISER:
                return .80f;
            case CAPITAL_SHIP:
                return 1f;
        }

        return 1f;
    }

    private static Color getColorForHullDamage(double pctHull){
        if(pctHull > .98){
            return Misc.getTextColor();
        } else if(pctHull > .8){
            return  new Color(209, 205, 0);
        } else if(pctHull > .6){
            return  new Color(229, 165, 0);
        }else if(pctHull > .4){
            return new Color(241, 95, 0);
        } else {
            return new Color(168, 49, 43);
        }
    }

    private static String getPlayerFleetGoal(List<CombatResult> combatResults){
        if(combatResults.size() == 1){
            switch (combatResults.get(0).enemyFleetGoal){
                case PURSUIT: return Localization.PlayerGoalPursuit;
                case ESCAPE: return Localization.PlayerGoalEscape;
                case BATTLE: return Localization.PlayerGoalBattle;
            }
        }

        return "";
    }

    private static String getEnemyFleetGoal(List<CombatResult> combatResults){
        if(combatResults.size() == 1){
            switch (combatResults.get(0).enemyFleetGoal){
                case PURSUIT: return Localization.EnemyGoalPursuit;
                case ESCAPE: return Localization.EnemyGoalEscape;
                case BATTLE: return Localization.EnemyGoalBattle;
            }
        }

        return "";
    }

    private static boolean isOkToReportOnHull(ShipAPI.HullSize hullSize){
        return hullSize != ShipAPI.HullSize.FIGHTER && hullSize != ShipAPI.HullSize.DEFAULT;
    }

    // Combat data that is used during rendering.  This is NOT persisted.
    private static class RenderCombatData {
        // all ships, needed for total damage aggregation and what not
        public List<GroupedByShipDamage> AllPlayerShipStats = new ArrayList<>();

        // just the ships we're rendering
        public List<GroupedByShipDamage> DisplayedPlayerShipStats = new ArrayList<>();

        public List<GroupedByShipDamage> EnemyShipStats = new ArrayList<>();
        public List<CombatResult> CombatResults = new ArrayList<>();
    }
}
