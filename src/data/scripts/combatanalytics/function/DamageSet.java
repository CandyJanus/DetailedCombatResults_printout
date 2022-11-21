package data.scripts.combatanalytics.function;

import data.scripts.combatanalytics.data.WeaponTargetDamage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.fs.starfarer.api.combat.ShipAPI.HullSize;

/*
A grouping of groupName damages that can be aggregated and filtered
 */
public class DamageSet implements Iterable<WeaponTargetDamage> {
    private static final Set<HullSize> SHIPS = new HashSet<>(Arrays.asList(
            HullSize.FRIGATE,
            HullSize.DESTROYER,
            HullSize.CRUISER,
            HullSize.CAPITAL_SHIP));

    private static final Set<HullSize> ALL = new HashSet<>(Arrays.asList(
            HullSize.FIGHTER,
            HullSize.FRIGATE,
            HullSize.DESTROYER,
            HullSize.CRUISER,
            HullSize.CAPITAL_SHIP));

    private static final Set<HullSize> MISSILE = new HashSet<>(Collections.singletonList(HullSize.DEFAULT));
    private static final Set<HullSize> FIGHTER = new HashSet<>(Collections.singletonList(HullSize.FIGHTER));
    private static final Set<HullSize> FRIGATE = new HashSet<>(Collections.singletonList(HullSize.FRIGATE));
    private static final Set<HullSize> DESTROYER = new HashSet<>(Collections.singletonList(HullSize.DESTROYER));
    private static final Set<HullSize> CRUISER = new HashSet<>(Collections.singletonList(HullSize.CRUISER));
    private static final Set<HullSize> CAPITAL_SHIP = new HashSet<>(Collections.singletonList(HullSize.CAPITAL_SHIP));



    public final String groupName; // either weaponName or allWeapons
    private final List<WeaponTargetDamage> _damages = new ArrayList<>();

    public DamageSet(String weapon){
        this.groupName = weapon;
    }

    public void merge(WeaponTargetDamage wtd){
        _damages.add(wtd);
    }

    public void merge(DamageSet other){
        _damages.addAll(other._damages);
    }

    public AggregateDamage aggregateDamages(Set<HullSize> validForCounting)
    {
        AggregateDamage wd = new AggregateDamage(groupName);
        for(WeaponTargetDamage wtd : _damages) {
            if (validForCounting.contains(wtd.target.hullSize)) {
                wd.merge(wtd);
            }
        }

        return wd;
    }

    public AggregateDamage aggregateShips()
    {
        return aggregateDamages(SHIPS);
    }

    public AggregateDamage aggregateFighters()
    {
        return aggregateDamages(FIGHTER);
    }

    public AggregateDamage aggregateMissiles()
    {
        return aggregateDamages(MISSILE);
    }

    @Override
    public Iterator<WeaponTargetDamage> iterator() {
        return _damages.iterator();
    }
}
