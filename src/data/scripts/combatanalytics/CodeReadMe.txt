/damagedetection/ objects related to the gathering of damage data from combat
/data/ contains persistence objects, should not contain any functionality that isn't necessary for data serialization & deserialization
/function/ contains objects for performing analytics on data
/util/

The IntelCombatReport is serialized on save so it's essential that it not depend upon any objects other than java objects.
Failing to do that will create big problems with upgradability