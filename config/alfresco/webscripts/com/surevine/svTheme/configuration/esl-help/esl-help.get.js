
// Get the motd dashlet content
model.contentNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Configuration/Enhanced Security/help.html');

cache.neverCache=false;
cache.isPublic=false;
cache.maxAge=3600; //1 hour
cache.mustRevalidate=false;
cache.ETag = 100;