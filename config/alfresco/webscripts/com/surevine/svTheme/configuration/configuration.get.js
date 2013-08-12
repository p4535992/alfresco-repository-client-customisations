
// Get the change password url
model.changePasswordUrlNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Configuration/Header/url_change_password.txt');

//Get the security label header
model.securityLabelHeaderNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Configuration/Header/security_label.txt');

//Get the help url
model.helpLinkNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Configuration/Header/url_help.txt');

model.launchChatUrlNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Configuration/Chat Dashlet/url_launch_chat.txt');

//Get the logo node
model.appLogoNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Configuration/Header/app_logo');

model.surevineLinkUrlNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Configuration/Footer/url_surevine_link.txt');

cache.neverCache=false;
cache.isPublic=false;
cache.maxAge=36000; //10 hours
cache.mustRevalidate=false;
cache.ETag = 100;
