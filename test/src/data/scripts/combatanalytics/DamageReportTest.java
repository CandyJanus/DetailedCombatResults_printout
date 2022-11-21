package data.scripts.combatanalytics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CombatDamageData;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatNebulaAPI;
import com.fs.starfarer.api.combat.CombatUIAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.FogOfWarAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.mission.FleetSide;
import data.scripts.DamageReportManagerV1;
import data.scripts.DamageReportV1;
import org.junit.Assert;
import org.junit.Test;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamageReportTest {

    @Test
    public void TestRoundTrip(){
        Global.setCombatEngine(new CombatEngineAPI() {
            Map<String, Object> customData = new HashMap<>();

            @Override
            public boolean isInCampaign() {
                return false;
            }

            @Override
            public boolean isInCampaignSim() {
                return false;
            }

            @Override
            public CombatUIAPI getCombatUI() {
                return null;
            }

            @Override
            public void setHyperspaceMode() {

            }

            @Override
            public List<BattleObjectiveAPI> getAllObjectives() {
                return null;
            }

            @Override
            public List<ShipAPI> getAllShips() {
                return null;
            }

            @Override
            public List<BattleObjectiveAPI> getObjectives() {
                return null;
            }

            @Override
            public List<ShipAPI> getShips() {
                return null;
            }

            @Override
            public List<MissileAPI> getMissiles() {
                return null;
            }

            @Override
            public List<CombatEntityAPI> getAsteroids() {
                return null;
            }

            @Override
            public List<BeamAPI> getBeams() {
                return null;
            }

            @Override
            public List<DamagingProjectileAPI> getProjectiles() {
                return null;
            }

            @Override
            public boolean isEntityInPlay(CombatEntityAPI entity) {
                return false;
            }

            @Override
            public FogOfWarAPI getFogOfWar(int owner) {
                return null;
            }

            @Override
            public void removeEntity(CombatEntityAPI entity) {

            }

            @Override
            public CombatFleetManagerAPI getFleetManager(FleetSide side) {
                return null;
            }

            @Override
            public CombatFleetManagerAPI getFleetManager(int owner) {
                return null;
            }

            @Override
            public ShipAPI getPlayerShip() {
                return null;
            }

            @Override
            public boolean isPaused() {
                return false;
            }

            @Override
            public void endCombat(float delay) {

            }

            @Override
            public void setDoNotEndCombat(boolean doNotEndCombat) {

            }

            @Override
            public void endCombat(float delay, FleetSide winner) {

            }

            @Override
            public ViewportAPI getViewport() {
                return null;
            }

            @Override
            public void applyDamage(CombatEntityAPI entity, Vector2f point, float damageAmount, DamageType damageType, float empAmount, boolean bypassShields, boolean dealsSoftFlux, Object source, boolean playSound) {

            }

            @Override
            public void applyDamage(CombatEntityAPI entity, Vector2f point, float damageAmount, DamageType damageType, float empAmount, boolean bypassShields, boolean dealsSoftFlux, Object source) {

            }

            @Override
            public void applyDamage(Object damageModifierParam, CombatEntityAPI entity, Vector2f point, float damageAmount, DamageType damageType, float empAmount, boolean bypassShields, boolean dealsSoftFlux, Object source, boolean playSound) {

            }

            @Override
            public void addHitParticle(Vector2f loc, Vector2f vel, float size, float brightness, float duration, Color color) {

            }

            @Override
            public void addSmoothParticle(Vector2f loc, Vector2f vel, float size, float brightness, float duration, Color color) {

            }

            @Override
            public void addSmokeParticle(Vector2f loc, Vector2f vel, float size, float opacity, float duration, Color color) {

            }

            @Override
            public void spawnExplosion(Vector2f loc, Vector2f vel, Color color, float size, float maxDuration) {

            }

            @Override
            public CombatEntityAPI spawnAsteroid(int size, float x, float y, float dx, float dy) {
                return null;
            }

            @Override
            public void addFloatingText(Vector2f loc, String text, float size, Color color, CombatEntityAPI attachedTo, float flashFrequency, float flashDuration) {

            }

            @Override
            public void addFloatingDamageText(Vector2f loc, float damage, Color color, CombatEntityAPI attachedTo, CombatEntityAPI damageSource) {

            }

            @Override
            public CombatEntityAPI spawnProjectile(ShipAPI ship, WeaponAPI weapon, String weaponId, Vector2f point, float angle, Vector2f shipVelocity) {
                return null;
            }

            @Override
            public EmpArcEntityAPI spawnEmpArc(ShipAPI damageSource, Vector2f point, CombatEntityAPI pointAnchor, CombatEntityAPI empTargetEntity, DamageType damageType, float damAmount, float empDamAmount, float maxRange, String impactSoundId, float thickness, Color fringe, Color core) {
                return null;
            }

            @Override
            public EmpArcEntityAPI spawnEmpArcPierceShields(ShipAPI damageSource, Vector2f point, CombatEntityAPI pointAnchor, CombatEntityAPI empTargetEntity, DamageType damageType, float damAmount, float empDamAmount, float maxRange, String impactSoundId, float thickness, Color fringe, Color core) {
                return null;
            }

            @Override
            public float getMapWidth() {
                return 0;
            }

            @Override
            public float getMapHeight() {
                return 0;
            }

            @Override
            public BattleCreationContext getContext() {
                return null;
            }

            @Override
            public float getTotalElapsedTime(boolean includePaused) {
                return 0;
            }

            @Override
            public float getElapsedInLastFrame() {
                return 0;
            }

            @Override
            public void addPlugin(EveryFrameCombatPlugin plugin) {

            }

            @Override
            public void removePlugin(EveryFrameCombatPlugin plugin) {

            }

            @Override
            public boolean isSimulation() {
                return false;
            }

            @Override
            public boolean isMission() {
                return false;
            }

            @Override
            public String getMissionId() {
                return null;
            }

            @Override
            public void setPlayerShipExternal(ShipAPI ship) {

            }

            @Override
            public boolean isUIShowingDialog() {
                return false;
            }

            @Override
            public boolean isUIShowingHUD() {
                return false;
            }

            @Override
            public boolean isUIAutopilotOn() {
                return false;
            }

            @Override
            public float getElapsedInContactWithEnemy() {
                return 0;
            }

            @Override
            public boolean isFleetsInContact() {
                return false;
            }

            @Override
            public void setSideDeploymentOverrideSide(FleetSide sideDeploymentOverrideSide) {

            }

            @Override
            public Map<String, Object> getCustomData() {
                return customData;
            }

            @Override
            public void maintainStatusForPlayerShip(Object key, String spriteName, String title, String data, boolean isDebuff) {

            }

            @Override
            public void setPaused(boolean paused) {

            }

            @Override
            public boolean playerHasNonAllyReserves() {
                return false;
            }

            @Override
            public boolean playerHasAllyReserves() {
                return false;
            }

            @Override
            public CombatDamageData getDamageData() {
                return null;
            }

            @Override
            public MutableStat getTimeMult() {
                return null;
            }

            @Override
            public void setMaxFleetPoints(FleetSide side, int fleetPoints) {

            }

            @Override
            public CombatNebulaAPI getNebula() {
                return null;
            }

            @Override
            public boolean isInFastTimeAdvance() {
                return false;
            }

            @Override
            public CombatEntityAPI spawnProjectile(ShipAPI ship, WeaponAPI weapon, String weaponId, String projSpecId, Vector2f point, float angle, Vector2f shipVelocity) {
                return null;
            }

            @Override
            public void updateStationModuleLocations(ShipAPI station) {

            }

            @Override
            public CollisionGridAPI getAllObjectGrid() {
                return null;
            }

            @Override
            public CollisionGridAPI getShipGrid() {
                return null;
            }

            @Override
            public CollisionGridAPI getMissileGrid() {
                return null;
            }

            @Override
            public CollisionGridAPI getAsteroidGrid() {
                return null;
            }

            @Override
            public DamagingProjectileAPI spawnDamagingExplosion(DamagingExplosionSpec spec, ShipAPI source, Vector2f location) {
                return null;
            }

            @Override
            public DamagingProjectileAPI spawnDamagingExplosion(DamagingExplosionSpec spec, ShipAPI source, Vector2f location, boolean canDamageSource) {
                return null;
            }

            @Override
            public int getWinningSideId() {
                return 0;
            }

            @Override
            public boolean isCombatOver() {
                return false;
            }

            @Override
            public void removeObject(Object object) {

            }

            @Override
            public CombatEntityAPI addLayeredRenderingPlugin(CombatLayeredRenderingPlugin plugin) {
                return null;
            }

            @Override
            public boolean isEnemyInFullRetreat() {
                return false;
            }

            @Override
            public boolean isMissileAlive(MissileAPI missile) {
                return false;
            }

            @Override
            public void spawnMuzzleFlashOrSmoke(ShipAPI ship, WeaponSlotAPI slot, WeaponSpecAPI spec, int barrel, float targetAngle) {

            }

            @Override
            public CollisionGridAPI getAiGridMissiles() {
                return null;
            }

            @Override
            public CollisionGridAPI getAiGridShips() {
                return null;
            }

            @Override
            public CollisionGridAPI getAiGridAsteroids() {
                return null;
            }

            @Override
            public boolean isAwareOf(int owner, CombatEntityAPI other) {
                return false;
            }

            @Override
            public void headInDirectionWithoutTurning(MissileAPI missile, float desiredHeading, float desiredSpeed) {

            }

            @Override
            public void headInDirectionWithoutTurning(ShipAPI ship, float desiredHeading, float desiredSpeed) {

            }

            @Override
            public Vector2f getAimPointWithLeadForAutofire(CombatEntityAPI from, float accuracyFactor, CombatEntityAPI to, float projSpeed) {
                return null;
            }

            @Override
            public CombatListenerManagerAPI getListenerManager() {
                return null;
            }

            @Override
            public void applyDamageModifiersToSpawnedProjectileWithNullWeapon(ShipAPI source, WeaponAPI.WeaponType type, boolean isBeam, DamageAPI damage) {

            }

            @Override
            public void addHitParticle(Vector2f loc, Vector2f vel, float size, float brightness, float durationIn, float totalDuration, Color color) {

            }

            @Override
            public void addHitParticle(Vector2f loc, Vector2f vel, float size, float brightness, Color color) {

            }

            @Override
            public EmpArcEntityAPI spawnEmpArcVisual(Vector2f from, CombatEntityAPI fromAnchor, Vector2f to, CombatEntityAPI toAnchor, float thickness, Color fringe, Color core) {
                return null;
            }

            @Override
            public void addSmoothParticle(Vector2f loc, Vector2f vel, float size, float brightness, float rampUpFraction, float totalDuration, Color color) {

            }

            @Override
            public void addNegativeParticle(Vector2f loc, Vector2f vel, float size, float rampUpFraction, float totalDuration, Color color) {

            }

            @Override
            public void addNebulaParticle(Vector2f loc, Vector2f vel, float size, float endSizeMult, float rampUpFraction, float fullBrightnessFraction, float totalDuration, Color color) {

            }

            @Override
            public void addNegativeNebulaParticle(Vector2f loc, Vector2f vel, float size, float endSizeMult, float rampUpFraction, float fullBrightnessFraction, float totalDuration, Color color) {

            }

            @Override
            public void addNebulaSmokeParticle(Vector2f loc, Vector2f vel, float size, float endSizeMult, float rampUpFraction, float fullBrightnessFraction, float totalDuration, Color color) {

            }

            @Override
            public boolean hasAttachedFloaty(CombatEntityAPI entity) {
                return false;
            }

            @Override
            public void addNebulaParticle(Vector2f loc, Vector2f vel, float size, float endSizeMult, float rampUpFraction, float fullBrightnessFraction, float totalDuration, Color color, boolean expandAsSqrt) {

            }

            @Override
            public void addSwirlyNebulaParticle(Vector2f loc, Vector2f vel, float size, float endSizeMult, float rampUpFraction, float fullBrightnessFraction, float totalDuration, Color color, boolean expandAsSqrt) {

            }

            @Override
            public void addNegativeSwirlyNebulaParticle(Vector2f loc, Vector2f vel, float size, float endSizeMult, float rampUpFraction, float fullBrightnessFraction, float totalDuration, Color color) {

            }

            @Override
            public boolean isInPlay(Object object) {
                return false;
            }

            @Override
            public void setCombatNotOverForAtLeast(float seconds) {

            }

            @Override
            public void setCombatNotOverFor(float seconds) {

            }

            @Override
            public float getCombatNotOverFor() {
                return 0;
            }

            @Override
            public void setCustomExit(String buttonTitle, String confirmString) {

            }

            @Override
            public String getCustomExitButtonTitle() {
                return null;
            }

            @Override
            public String getCustomExitButtonConfirmString() {
                return null;
            }

            @Override
            public void addFloatingTextAlways(Vector2f loc, String text, float size, Color color, CombatEntityAPI attachedTo, float flashFrequency, float flashDuration, float durInPlace, float durFloatingUp, float durFadingOut, float baseAlpha) {

            }

            @Override
            public WeaponAPI createFakeWeapon(ShipAPI ship, String weaponId) {
                return null;
            }

            @Override
            public ShipAPI getShipPlayerIsTransferringCommandFrom() {
                return null;
            }

            @Override
            public ShipAPI getShipPlayerIsTransferringCommandTo() {
                return null;
            }

            @Override
            public ShipAPI getShipPlayerLastTransferredCommandTo() {
                return null;
            }
        });

        DamageReportV1 one = new DamageReportV1(1, 2, 3, 4, DamageType.ENERGY, null, null, "weaponName");

        DamageReportManagerV1 manager1 = DamageReportManagerV1.getDamageReportManager();
        manager1.addDamageReport(one);
        manager1.addDamageReport(one);

        Assert.assertEquals(2, manager1.getDamageReports().size());
        manager1.clearDamageReports();
        Assert.assertEquals(0, manager1.getDamageReports().size());

        DamageReportManagerV1 manager2 = DamageReportManagerV1.getDamageReportManager();
        Assert.assertEquals(0, manager2.getDamageReports().size());

        manager1.addDamageReport(one);

        DamageReportV1 two = manager2.getDamageReports().get(0);

        Assert.assertEquals(one.getDamageType(), two.getDamageType());
        Assert.assertEquals(one.getSource(), two.getSource());
        Assert.assertEquals(one.getTarget(), two.getTarget());
        Assert.assertEquals(one.getWeaponName(), two.getWeaponName());
        Assert.assertEquals(one.getArmorDamage(), two.getArmorDamage(), .001f);
        Assert.assertEquals(one.getEmpDamage(), two.getEmpDamage(), .001f);
        Assert.assertEquals(one.getHullDamage(), two.getHullDamage(), .001f);
        Assert.assertEquals(one.getShieldDamage(), two.getShieldDamage(), .001f);
    }
}
