
// Get the motd dashlet content
model.contentNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Pages/motd.html');

cache.neverCache = false;
cache.isPublic = false;
cache.maxAge = 3600; //5 minutes
cache.mustRevalidate=false;
cache.ETag = 100;