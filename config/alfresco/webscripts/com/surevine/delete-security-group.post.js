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
function main() 
{
	try 
	{
		
		var srcNodes = search.query(
	               {
	                  query: "PATH:\"/app:company_home/app:dictionary/cm:records_management/cm:enhancedSecurityCustomModel.xml\"",
	                  language: "lucene",
	                  page:
	                  {
	                     maxItems: 1
	                  }
	               });
		var model=srcNodes[0];
		var toDelete=model.properties["psg:deleteAuthorised"];
		if (!toDelete || toDelete==null)
		{
			toDelete = new Array();
		}

		
		for each (field in formdata.fields)
		{
			switch (String(field.name).toLowerCase())
			{
				case "group" :
					toDelete.push(field.value.trim());
					model.properties["psg:deleteAuthorised"]=toDelete;
					model.save();
				break;
				
				default:
					logger.warn("Non-form field - "+field.name +":" +field.value);
				break;
			}
		}
	} catch (e) {
		var x = e;

	    status.code = 500;
	    status.message = "Unexpected error occured during deletion of security group.";

	    status.redirect = true;
	    throw e;
	}
}

main();
