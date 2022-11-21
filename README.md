# DetailedCombatResults_printout

NuclearStudent's Changes

note: NuclearStudent is messing around with this to get the results to print to a text file or something, so he people can write a program to automatically analyze the data. It'd be nice to have an autosimulator continuously set up battles and output statistics on how the AI performs with given loadouts against given adversaries.

0.0.1 - trying to make this compile

Had to change how Pair was being added.
Couldn't find DamagingEntities in the api.
____

Starsector Detailed Combat Results

Ever wonder how many ships you've killed?  Or how a new ship load-out you've created is performing in combat?  Or how useless a cautious officer is in an Onslaught?
No more guessing, now you can know for sure!

Features:
 * Various reports summarizing ship performance over time or per battle
 * Kill count of various hull sizes
 * Individual weapon performance
 * Easy to use, just add the mod and you're good to go.
 * Disabling the mod is possible, set MaxCombatResultCount=0, load your save, advance one day, save, quit and then disable the mod.

How to use:
 1. Download & install this mod
 2. Make sure this mod is enabled
 3. Fight at least one battle (simulation doesn't count)
 4. View the generated intel event using the 'e' key OR
 5. Press the 'l' key (configurable in mod's settings.json) when in the campaign UI

 Mod compatibility:
 Requires no extra mods to function, plays nice with all tested mods (bunches of factions, SS+ and Nexerlin)

Known Issues/Limitations:
 * Occasionally the killing blow calculation will be incorrect
 * Wont work for simulation battles (requires on combat end events to be fired that aren't fired in sim)
 * Only tracks raw damage before mitigation and armor
 * Can't track damage done by area-of-effect weapons (API limitation).
 * Can't track damage done by on-hit effects
 * Rarely I've seen the destroyed & disabled lists switched.  I don't think this is a mod issue.

Using this mod in other mods:
This mod is MIT license, you can do what ever you want with this, no need to ask.  Just be nice and mention that this mod exists and where you got it from :)


Change Log:
Detailed Combat Results mod changelog
v5.2.3
    Supports having damage values supplied by mods (specifically Realistic Combat)

v5.2.2
   Ship count limit now configurable via JSON (for intel display)
   Updated wording for enemy fleet damage dealt
   Lightning gun from SWP now has its damage recorded correctly
   Don't process missile damages 2x (was caused by processing explosion damage and then missile damge)
   Don't count launched missiles as a part of a fleet

v5.2.1
 Bugfixes

v5.2.0
 Updated to .95.1a
 Now tracks missiles intercepted
 Now tracks explosion damage in a more robust manner (greatly improved accuracy)
 General improvements to damage detection accuracy

v5.1.2
 Fixed issue with explosions and looking too deeply inside core objects. Thanks Photonsynthesis
 Better explosion damage detection

v5.1.1
 Add caching layer to prevent perf issues if IntelCombatReport.isValid() is called repeatedly (by mods)

v5.1
 Campaign Simulation Battles now create "simulation" combat results that last for a day
 Changed Flux Dmg -> EMP Dmg
 Localization fixes (some strings were omitted)
 Issue with Motes not always being properly identified

v5.0
 Can now be localized, current localization is English
 Uses StarSector .95 damage listener system to give much more accurate values for damage.
 Now properly tracks (didn't before):
 * plasma cannons
 * Flak weapons
 * storyline "things"
 * collision damage
 * on-hit effects
 Beam weapons that use multiple beams (like Seeker's kaleidoscope) now have damage computed correctly
 Renamed mod to show scope and goal (not general analytics, just detailed combat results)

v4.6.1
 Fix issue with failing to parse a saved combat for which there was no damage dealt

v4.6
 Fix all projectile weapons dealing double damage

v4.5
 Another attempt at getting submunition damage correct

v4.4
 Try again to get MIRVs to calculate damage correctly without breaking anything else

v4.3
 Fully compatible with prior version
 MIRV warheads should now calculate their damage properly (Thanks MesoTroniK)
 Defend against NULL in a place it shouldn't be possible but apparently is.

v4.2
 New save data format to enable new functionality
 * this won't break anything
 * but your old combat data will be automatically deleted.
 * Your save game is fine.
 No longer displaying kills based on killing blows, we now display Solo Kills (> 80% of hull damage) and Assists (> 20% hull dmg)
 DP destroyed based on pro-rated armor/hull damage
 More accurate damage calculations.  Not perfect, but better.
 More accurate tracking of what ships were killed.
 Fighters are now treated as a singular weapon system, since that is often the most useful way of thinking about them
 Ships details "kill" grid removed, replaced with sprites:
 * now render sprites for ships that were solo-killed
 * render ship sprites that were kill-assisted (along with a damage overlay)
 New enemy fleet status summary area
 Player ships are shown as Disabled/Destroyed/Retreated in ship list
 Added % hull remaining after combat, color coded

v4.1
 Proper fighter kill count for TOTAL rows
 Denser data format for detailed combat data (to try and avoid 1MB limit)
 No longer using "ship list" functionality, now using rendered sprite images in list
 If SaveDetailedCombatData is set to true, when results from main menu missions are saved
 Images scaled slightly by hull class
 Use 7 zip command line to build archive to hopefully work around archive slash issues

v4.0
 Added new intel window for post combat detailed results (press 'e')
 Weapons can now properly compute how many fighters they've killed (rewrote aggregation)
 Various bug fixes around things done in mods that aren't done in Vanilla (big thanks stormbringer951)
 If SaveDetailedCombatData is on, files will be written after each battle in the folder /saves/common/combatanalytics/

3.3
 Defend against key not being specified in JSON

3.2
 Handle possible exception (should only happen on some mods)
 Sort results by damage dealt desc

3.1
 Updated to starsector .9a
 
3.0
 Updated to Starsector .8.1a
 Fighters are tracked as weapon systems

2.0
 Data is persisted as strings so disabling the mod wont break saves
 Better exception trapping in case something unexpected happens

1.0 - Released


Forum: http://fractalsoftworks.com/forum/index.php?topic=xxx
Source: https://bitbucket.org/NickWWest/starsectorcombatanalytics
License: MIT License (Do what ever you want, creators not liable)



