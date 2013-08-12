
// Get the motd dashlet content
model.launchUrlNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Chat Dashlet/url_launch_chat.txt');

cache.neverCache=false;
cache.isPublic=false;
cache.maxAge=36000; //10 hours
cache.mustRevalidate=false;
cache.ETag = 100;