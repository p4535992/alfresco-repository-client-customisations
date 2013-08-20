/*
 * Copyright (C) 2008-2010 Surevine Limited.
 *   
 * Although intended for deployment and use alongside Alfresco this module should
 * be considered 'Not a Contribution' as defined in Alfresco'sstandard contribution agreement, see
 * http://www.alfresco.org/resource/AlfrescoContributionAgreementv2.pdf
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
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
