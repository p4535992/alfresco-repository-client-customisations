
// Get the security label header
model.userDashboardTitleNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Configuration/User Dashboard/title.html');

//Get the background image node
model.backgroundImageNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Configuration/User Dashboard/background');

cache.neverCache=false;
cache.isPublic=false;
cache.maxAge=36000; //10 hours
cache.mustRevalidate=false;
cache.ETag = 100;