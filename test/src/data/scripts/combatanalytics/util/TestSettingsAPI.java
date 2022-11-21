package data.scripts.combatanalytics.util;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SpriteId;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketSpecAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatReadinessPlugin;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import com.fs.starfarer.api.loading.BarEventSpec;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.EventSpecAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.loading.PersonMissionSpec;
import com.fs.starfarer.api.loading.RoleEntryAPI;
import com.fs.starfarer.api.loading.TerrainSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.plugins.LevelupPlugin;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.ListMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;

public class TestSettingsAPI implements SettingsAPI {
    @Override
    public int getBattleSize() {
        return 0;
    }

    @Override
    public PersonAPI createPerson() {
        return null;
    }

    @Override
    public LabelAPI createLabel(String text, String font) {
        return null;
    }

    @Override
    public float getBonusXP(String key) {
        return 0;
    }

    @Override
    public float getFloat(String key) {
        return 0;
    }

    @Override
    public boolean getBoolean(String key) {
        return false;
    }

    @Override
    public ClassLoader getScriptClassLoader() {
        return null;
    }

    @Override
    public boolean isDevMode() {
        return false;
    }

    @Override
    public void setDevMode(boolean devMode) {

    }

    @Override
    public Color getColor(String id) {
        return null;
    }

    @Override
    public Object getInstanceOfScript(String className) {
        return null;
    }

    @Override
    public String getString(String category, String id) {
        return null;
    }

    @Override
    public SpriteAPI getSprite(String filename) {
        return null;
    }

    @Override
    public SpriteAPI getSprite(String category, String key) {
        return null;
    }

    @Override
    public SpriteAPI getSprite(SpriteId id) {
        return null;
    }

    @Override
    public String getSpriteName(String category, String id) {
        return null;
    }

    @Override
    public InputStream openStream(String filename) throws IOException {
        return null;
    }

    @Override
    public String loadText(String filename) throws IOException {
        return null;
    }

    @Override
    public JSONObject loadJSON(String filename) throws IOException, JSONException {
        // todo figure this out at test time
        filename = "C:\\dev\\src\\starsectorcombatanalytics\\CombatAnalyticsMod\\text\\DetailedCombatResultsStrings_en.json";

        String rawJson = new String(Files.readAllBytes(new File(filename).toPath()));

        StringBuilder cleanedJson = new StringBuilder();
        for(String line : rawJson.split("\\r?\\n")){
            if(!line.trim().startsWith("#")){
                cleanedJson.append(line);
                cleanedJson.append(System.lineSeparator());
            }
        }

        return new JSONObject(cleanedJson.toString());
    }

    @Override
    public JSONArray loadCSV(String filename) throws IOException, JSONException {
        return null;
    }

    @Override
    public JSONArray getMergedSpreadsheetDataForMod(String idColumn, String path, String masterMod) throws IOException, JSONException {
        return null;
    }

    @Override
    public JSONObject getMergedJSONForMod(String path, String masterMod) throws IOException, JSONException {
        return null;
    }

    @Override
    public float getScreenWidth() {
        return 0;
    }

    @Override
    public float getScreenHeight() {
        return 0;
    }

    @Override
    public float getScreenWidthPixels() {
        return 0;
    }

    @Override
    public float getScreenHeightPixels() {
        return 0;
    }

    @Override
    public Description getDescription(String id, Description.Type type) {
        return null;
    }

    @Override
    public CombatReadinessPlugin getCRPlugin() {
        return null;
    }

    @Override
    public int getCodeFor(String key) {
        return 0;
    }

    @Override
    public WeaponSpecAPI getWeaponSpec(String weaponId) {
        return null;
    }

    @Override
    public void loadTexture(String filename) throws IOException {

    }

    @Override
    public float getTargetingRadius(Vector2f from, CombatEntityAPI target, boolean considerShield) {
        return 0;
    }

    @Override
    public ShipVariantAPI getVariant(String variantId) {
        return null;
    }

    @Override
    public Object getPlugin(String id) {
        return null;
    }

    @Override
    public List<String> getSortedSkillIds() {
        return null;
    }

    @Override
    public SkillSpecAPI getSkillSpec(String skillId) {
        return null;
    }

    @Override
    public String getString(String key) {
        return null;
    }

    @Override
    public AbilitySpecAPI getAbilitySpec(String abilityId) {
        return null;
    }

    @Override
    public List<String> getSortedAbilityIds() {
        return null;
    }

    @Override
    public float getBaseTravelSpeed() {
        return 0;
    }

    @Override
    public float getSpeedPerBurnLevel() {
        return 0;
    }

    @Override
    public float getUnitsPerLightYear() {
        return 0;
    }

    @Override
    public int getMaxShipsInFleet() {
        return 0;
    }

    @Override
    public TerrainSpecAPI getTerrainSpec(String terrainId) {
        return null;
    }

    @Override
    public EventSpecAPI getEventSpec(String eventId) {
        return null;
    }

    @Override
    public CustomEntitySpecAPI getCustomEntitySpec(String id) {
        return null;
    }

    @Override
    public GameState getCurrentState() {
        return null;
    }

    @Override
    public int getMaxSensorRange() {
        return 0;
    }

    @Override
    public int getMaxSensorRangeHyper() {
        return 0;
    }

    @Override
    public int getMaxSensorRange(LocationAPI loc) {
        return 0;
    }

    @Override
    public List<String> getAllVariantIds() {
        return null;
    }

    @Override
    public List<String> getAptitudeIds() {
        return null;
    }

    @Override
    public List<String> getSkillIds() {
        return null;
    }

    @Override
    public LevelupPlugin getLevelupPlugin() {
        return null;
    }

    @Override
    public String getVersionString() {
        return null;
    }

    @Override
    public JSONObject loadJSON(String filename, String modId) throws IOException, JSONException {
        return null;
    }

    @Override
    public JSONArray loadCSV(String filename, String modId) throws IOException, JSONException {
        return null;
    }

    @Override
    public String loadText(String filename, String modId) throws IOException, JSONException {
        return null;
    }

    @Override
    public ModManagerAPI getModManager() {
        return null;
    }

    @Override
    public float getBaseFleetSelectionRadius() {
        return 0;
    }

    @Override
    public float getFleetSelectionRadiusPerUnitSize() {
        return 0;
    }

    @Override
    public float getMaxFleetSelectionRadius() {
        return 0;
    }

    @Override
    public List<RoleEntryAPI> getEntriesForRole(String factionId, String role) {
        return null;
    }

    @Override
    public void addEntryForRole(String factionId, String role, String variantId, float weight) {

    }

    @Override
    public void removeEntryForRole(String factionId, String role, String variantId) {

    }

    @Override
    public List<RoleEntryAPI> getDefaultEntriesForRole(String role) {
        return null;
    }

    @Override
    public void addDefaultEntryForRole(String role, String variantId, float weight) {

    }

    @Override
    public void removeDefaultEntryForRole(String role, String variantId) {

    }

    @Override
    public void profilerBegin(String id) {

    }

    @Override
    public void profilerEnd() {

    }

    @Override
    public void profilerPrintResultsTree() {

    }

    @Override
    public List<PlanetSpecAPI> getAllPlanetSpecs() {
        return null;
    }

    @Override
    public Object getSpec(Class c, String id, boolean nullOnNotFound) {
        return null;
    }

    @Override
    public void putSpec(Class c, String id, Object spec) {

    }

    @Override
    public Collection<Object> getAllSpecs(Class c) {
        return null;
    }

    @Override
    public String getRoman(int n) {
        return null;
    }

    @Override
    public void greekLetterReset() {

    }

    @Override
    public String getNextCoolGreekLetter(Object context) {
        return null;
    }

    @Override
    public String getNextGreekLetter(Object context) {
        return null;
    }

    @Override
    public MarketConditionSpecAPI getMarketConditionSpec(String conditionId) {
        return null;
    }

    @Override
    public ShipAIPlugin createDefaultShipAI(ShipAPI ship, ShipAIConfig config) {
        return null;
    }

    @Override
    public HullModSpecAPI getHullModSpec(String modId) {
        return null;
    }

    @Override
    public FighterWingSpecAPI getFighterWingSpec(String wingId) {
        return null;
    }

    @Override
    public List<HullModSpecAPI> getAllHullModSpecs() {
        return null;
    }

    @Override
    public List<FighterWingSpecAPI> getAllFighterWingSpecs() {
        return null;
    }

    @Override
    public List<WeaponSpecAPI> getAllWeaponSpecs() {
        return null;
    }

    @Override
    public boolean isSoundEnabled() {
        return false;
    }

    @Override
    public boolean isInCampaignState() {
        return false;
    }

    @Override
    public boolean isGeneratingNewGame() {
        return false;
    }

    @Override
    public float getAngleInDegreesFast(Vector2f v) {
        return 0;
    }

    @Override
    public float getAngleInDegreesFast(Vector2f from, Vector2f to) {
        return 0;
    }

    @Override
    public CommoditySpecAPI getCommoditySpec(String commodityId) {
        return null;
    }

    @Override
    public ShipHullSpecAPI getHullSpec(String hullId) {
        return null;
    }

    @Override
    public int computeNumFighterBays(ShipVariantAPI variant) {
        return 0;
    }

    @Override
    public boolean isInGame() {
        return false;
    }

    @Override
    public Object getNewPluginInstance(String id) {
        return null;
    }

    @Override
    public String getControlStringForAbilitySlot(int index) {
        return null;
    }

    @Override
    public String getControlStringForEnumName(String name) {
        return null;
    }

    @Override
    public boolean isNewPlayer() {
        return false;
    }

    @Override
    public IndustrySpecAPI getIndustrySpec(String industryId) {
        return null;
    }

    @Override
    public List<CommoditySpecAPI> getAllCommoditySpecs() {
        return null;
    }

    @Override
    public int getInt(String key) {
        return 0;
    }

    @Override
    public List<IndustrySpecAPI> getAllIndustrySpecs() {
        return null;
    }

    @Override
    public SpecialItemSpecAPI getSpecialItemSpec(String itemId) {
        return null;
    }

    @Override
    public List<SpecialItemSpecAPI> getAllSpecialItemSpecs() {
        return null;
    }

    @Override
    public List<ShipHullSpecAPI> getAllShipHullSpecs() {
        return null;
    }

    @Override
    public SpriteAPI getSprite(String category, String id, boolean emptySpriteOnNotFound) {
        return null;
    }

    @Override
    public ShipVariantAPI createEmptyVariant(String hullVariantId, ShipHullSpecAPI hullSpec) {
        return null;
    }

    @Override
    public ListMap<String> getHullIdToVariantListMap() {
        return null;
    }

    @Override
    public String readTextFileFromCommon(String filename) throws IOException {
        return null;
    }

    @Override
    public void writeTextFileToCommon(String filename, String data) throws IOException {

    }

    @Override
    public boolean fileExistsInCommon(String filename) {
        return false;
    }

    @Override
    public void deleteTextFileFromCommon(String filename) {

    }

    @Override
    public Color getBasePlayerColor() {
        return null;
    }

    @Override
    public Color getDesignTypeColor(String designType) {
        return null;
    }

    @Override
    public boolean doesVariantExist(String variantId) {
        return false;
    }

    @Override
    public void addCommodityInfoToTooltip(TooltipMakerAPI tooltip, float initPad, CommoditySpecAPI spec, int max, boolean withText, boolean withSell, boolean withBuy) {

    }

    @Override
    public JSONObject getJSONObject(String key) throws JSONException {
        return null;
    }

    @Override
    public JSONArray getJSONArray(String key) throws JSONException {
        return null;
    }

    @Override
    public FactionAPI createBaseFaction(String factionId) {
        return null;
    }

    @Override
    public List<MarketConditionSpecAPI> getAllMarketConditionSpecs() {
        return null;
    }

    @Override
    public List<SubmarketSpecAPI> getAllSubmarketSpecs() {
        return null;
    }

    @Override
    public float getMinArmorFraction() {
        return 0;
    }

    @Override
    public float getMaxArmorDamageReduction() {
        return 0;
    }

    @Override
    public ShipSystemSpecAPI getShipSystemSpec(String id) {
        return null;
    }

    @Override
    public List<ShipSystemSpecAPI> getAllShipSystemSpecs() {
        return null;
    }

    @Override
    public float getScreenScaleMult() {
        return 0;
    }

    @Override
    public int getAASamples() {
        return 0;
    }

    @Override
    public int getMouseX() {
        return 0;
    }

    @Override
    public int getMouseY() {
        return 0;
    }

    @Override
    public int getShippingCapacity(MarketAPI market, boolean inFaction) {
        return 0;
    }

    @Override
    public JSONObject getSettingsJSON() {
        return null;
    }

    @Override
    public void resetCached() {

    }

    @Override
    public void setFloat(String key, Float value) {

    }

    @Override
    public void setBoolean(String key, Boolean value) {

    }

    @Override
    public List<PersonMissionSpec> getAllMissionSpecs() {
        return null;
    }

    @Override
    public PersonMissionSpec getMissionSpec(String id) {
        return null;
    }

    @Override
    public List<BarEventSpec> getAllBarEventSpecs() {
        return null;
    }

    @Override
    public BarEventSpec getBarEventSpec(String id) {
        return null;
    }

    @Override
    public void setAutoTurnMode(boolean autoTurnMode) {

    }

    @Override
    public boolean isAutoTurnMode() {
        return false;
    }

    @Override
    public boolean isShowDamageFloaties() {
        return false;
    }

    @Override
    public float getFloatFromArray(String key, int index) {
        return 0;
    }

    @Override
    public int getIntFromArray(String key, int index) {
        return 0;
    }

    @Override
    public void loadTextureConvertBlackToAlpha(String filename) throws IOException {

    }

    @Override
    public String getControlDescriptionForEnumName(String name) {
        return null;
    }

    @Override
    public ShipAIPlugin pickShipAIPlugin(FleetMemberAPI member, ShipAPI ship) {
        return null;
    }

    @Override
    public void unloadTexture(String filename) {

    }

    @Override
    public void profilerSetEnabled(boolean enabled) {

    }

    @Override
    public void profilerReset() {

    }

    @Override
    public void profilerRestore() {

    }

    @Override
    public Color getBrightPlayerColor() {
        return null;
    }

    @Override
    public Color getDarkPlayerColor() {
        return null;
    }

    @Override
    public void forceMipmapsFor(String filename, boolean forceMipmaps) throws IOException {

    }

    @Override
    public String getGameVersion() {
        return null;
    }

    @Override
    public float computeStringWidth(String in, String font) {
        return 0;
    }

    @Override
    public TextFieldAPI createTextField(String text, String font) {
        return null;
    }

    @Override
    public ButtonAPI createCheckbox(String text, ButtonAPI.UICheckboxSize size) {
        return null;
    }

    @Override
    public ButtonAPI createCheckbox(String text, String font, Color checkColor, ButtonAPI.UICheckboxSize size) {
        return null;
    }

    @Override
    public CustomPanelAPI createCustom(float width, float height, CustomUIPanelPlugin plugin) {
        return null;
    }

    @Override
    public int getMissionScore(String id) {
        return 0;
    }
}
