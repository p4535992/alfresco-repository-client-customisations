<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/repository/enhanced-security/visibility/lib/visibility.lib.js">

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

//Set up the map of constraint names -> English
model.constraintNameMapping = {
	"es:validOpenMarkings" : "Thematic Groups",
	"es:validClosedMarkings" : "Groups and/or Restrictions",
	"es:validOrganisations" : "Organisations"
};

//Get the marking from the URL
var marking = getMarkingFromArgs(args);

//Get the comparison from the model and store the results in the model
model.machineResults = compareAuthorityGroupsWithMarking(args.userName, marking);

// Store the information about the user we're looking up
model.person = people.getPerson(args.userName);

//Don't do any caching here as people might use this service in conjunction with an admin tool to work out who can see what
