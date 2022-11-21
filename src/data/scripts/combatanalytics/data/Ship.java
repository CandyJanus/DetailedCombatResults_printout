package data.scripts.combatanalytics.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.combatanalytics.Exportable;
import data.scripts.combatanalytics.Saveable;
import data.scripts.combatanalytics.util.Helpers;
import org.apache.log4j.Logger;

import static data.scripts.combatanalytics.util.Helpers.INT_FORMAT_NO_GROUP_FORMAT;
import static data.scripts.combatanalytics.util.Helpers.coalesce;
import static data.scripts.combatanalytics.util.Helpers.ownerAsString;

/**
 * Represents A ship in A battle.  Id is stable across battles, so combatId is also used to determine "uniqueness"
 */
public class Ship implements Saveable, Exportable, Comparable<Ship> {
    private static final Logger log = Global.getLogger(data.scripts.combatanalytics.data.Ship.class);

    public static final String NO_CAPTAIN = "No Captain";
    public static final String NO_NAME = "No Name";

    public final String id;   // Should be the same as FleetMember.Id
    public final String combatId;
    public String name;
    public final ShipAPI.HullSize hullSize;
    public final String hullClass;
    public final int owner;

    public final float maxHp;
    public final float maxFlux;
    public int crew;
    public float remainingHp; // we might want to track damage sustained, but that's a bit more work

    public int deploymentPoints; // unmodified value
    public String captain = NO_CAPTAIN;

    public final String hullId;
    public ShipStatus status = ShipStatus.OK; // start as OK

    // only used by the intel report on initial construction
    public String captainSprite = "";

    public Ship(String[] serialized){
        try {
            id = serialized[0];
            combatId = serialized[1];
            name = coalesce(serialized[2], id, NO_NAME);
            hullSize = ShipAPI.HullSize.valueOf(serialized[3]);
            hullClass = serialized[4];
            owner = Integer.parseInt(serialized[5]);
            maxHp = Helpers.parseFloat(serialized[6]);
            maxFlux = Helpers.parseFloat(serialized[7]);
            deploymentPoints = Integer.parseInt(serialized[8]);
            this.captain = serialized[9];
            if(this.captain.equals("")){
                this.captain = NO_CAPTAIN;
            }
            hullId = serialized[10];
            status = ShipStatus.valueOf(serialized[11]);
            captainSprite = serialized[12];
            crew = Integer.parseInt(serialized[13]);
            remainingHp = Helpers.parseFloat(serialized[14]);

            if(hullSize == ShipAPI.HullSize.FIGHTER){
                name = hullClass +" Fighter";
            }
        } catch (Throwable e){
            StringBuilder line = new StringBuilder(); // String.join would be nice here
            for(String s : serialized){
                line.append("'").append(s).append("'");
                line.append('\t');
            }

            log.error("Error deserializing line (Column Count: "+serialized.length+"): " +line, e);
            throw e;
        }
    }

    //Constructed during combat
    public Ship(ShipAPI ship, String combatId){
        id = ship.isFighter() || ship.isDrone() || ship.getFleetMemberId() == null ? Helpers.getSmallUuid() : ship.getFleetMemberId();
        this.combatId = combatId;
        name = coalesce(ship.getName(), id, NO_NAME);
        hullSize = ship.getHullSpec().getHullSize();
        hullClass = cleanUpDesignation(ship.getHullSpec().getHullName());

        owner = ship.getOriginalOwner();
        maxHp = ship.getMaxHitpoints();
        maxFlux = ship.getFluxTracker().getMaxFlux();
        hullId = ship.getHullSpec().getHullId();

        if(hullSize == ShipAPI.HullSize.FIGHTER){
            name = hullClass +" Fighter";
        }
    }

    public Ship(FleetMemberAPI fm, String combatId){
        id = fm.isFighterWing() ? Helpers.getSmallUuid() : fm.getId();
        this.combatId = combatId;
        name = coalesce(fm.getShipName(), id, NO_NAME);
        hullSize = fm.getHullSpec().getHullSize();
        hullClass = cleanUpDesignation(fm.getHullSpec().getHullName());

        owner = fm.getOwner();
        maxHp = fm.getHullSpec().getHitpoints();
        maxFlux = fm.getHullSpec().getFluxCapacity();
        hullId = fm.getHullSpec().getHullId();

        if(hullSize == ShipAPI.HullSize.FIGHTER){
            name = hullClass +" Fighter";
        }
    }

    public void setCaptain(PersonAPI captain){
        if(captain != null){
            this.captain = captain.getName().getFullName();
            this.captainSprite = captain.getPortraitSprite();
        }
    }

    // set post-combat
    public void setFleetMemberData(PersonAPI captain, int deploymentPoints, ShipStatus status, int crew, float remainingHp){
        setCaptain(captain);

        this.deploymentPoints = deploymentPoints;

        this.status = status;
        this.crew = crew;
        this.remainingHp = remainingHp;

        if(status.wasKilled()){
            this.remainingHp = 0;
        }
    }

    public Ship(String id, String combatId, String name, ShipAPI.HullSize hullSize, String hullClass, float maxHitpoints,
                float maxFlux, int owner, int deploymentPoints, String captain, String hullId, ShipStatus shipStatus, String captainSprite, float remainingHp){
        this.id = id;
        this.combatId = combatId;
        this.name = name;
        this.hullSize = hullSize;
        this.hullClass = hullClass;
        this.owner = owner;
        this.maxFlux = maxFlux;
        this.deploymentPoints = deploymentPoints;
        this.maxHp = maxHitpoints;
        this.captain = captain;

        if(this.captain.equals("")){
            this.captain = NO_CAPTAIN;
        }
        this.captainSprite = captainSprite;
        this.hullId = hullId;
        this.status = shipStatus;
        this.remainingHp = remainingHp;
    }

    private String cleanUpDesignation(String designation) {
        // probably a better way to do do this, but I couldn't figure it out based on the docs
        int dashIndex = designation.lastIndexOf("-class");
        if(dashIndex > -1){
            return designation.substring(0, dashIndex);
        }

        dashIndex = designation.lastIndexOf(" class");
        if (dashIndex > -1) {
            return designation.substring(0, dashIndex);
        }
        return designation.trim();
    }

    public boolean hasCaptain(){
        return !this.captain.equals(NO_CAPTAIN);
    }

    public String toString(){
        return String.format("%1$s - %2$s - %3$s - %4$s", name, hullSize, hullClass, ownerAsString(owner));

        //"ISS ship - Destroyer - Medeusa - Player"
    }

    public String getHullSizeString(){
        switch (this.hullSize){
            case CAPITAL_SHIP: return "Capital";
            case CRUISER: return "Cruiser";
            case DESTROYER: return "Destroyer";
            case FRIGATE: return "Frigate";
            case FIGHTER: return "Fighter";
            default: return hullSize.toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ship ship = (Ship) o;

        return id.equals(ship.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String toTsv(){
        return Helpers.toTsv(
                id,
                name,
                hullSize,
                hullClass,
                ownerAsString(owner),
                INT_FORMAT_NO_GROUP_FORMAT.format(maxHp),
                INT_FORMAT_NO_GROUP_FORMAT.format(maxFlux),
                deploymentPoints,
                captain,
                hullId,
                status,
                crew,
                remainingHp);
    }

    public static String getTsvHeader(){
        return Helpers.toTsv("ShipId", "Name", "HullSize", "HullClass", "Owner", "MaxHP", "MaxFlux", "DeploymentPoints", "Captain", "HullId", "Status", "Crew", "RemainingHP");
    }

    public String serialize(){
        String saveCaptain = captain;
        if(saveCaptain.equals(NO_CAPTAIN)){
            saveCaptain = "";
        }

        String saveName = name;
        if(this.hullSize == ShipAPI.HullSize.FIGHTER){
            name = "";
        }

        return Helpers.toTsv(
                id, combatId, saveName, hullSize, hullClass, owner, INT_FORMAT_NO_GROUP_FORMAT.format(maxHp),
                INT_FORMAT_NO_GROUP_FORMAT.format(maxFlux), deploymentPoints, saveCaptain, hullId, status,
                captainSprite, crew, INT_FORMAT_NO_GROUP_FORMAT.format(remainingHp));
    }


    @Override
    public int compareTo(Ship o) {
        int ret = Integer.compare(this.owner, o.owner);
        if(ret == 0){
            ret = Integer.compare(this.deploymentPoints, o.deploymentPoints);
        }
        if(ret == 0){
            ret = this.name.compareTo(o.name);
        }

        return ret;
    }

    public double getRemainingHullPct(){
        if(remainingHp < .001){
            return 0d;
        }

        double ret = this.remainingHp / this.maxHp;
        if(Double.isNaN(ret) || Double.isInfinite(ret)){
            return 1;
        } else {
            return ret;
        }
    }
}