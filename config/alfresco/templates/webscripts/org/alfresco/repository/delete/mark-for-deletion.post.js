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
try {
  var action = actions.create("com.surevine.alfresco.repo.action.delete.MarkForDeleteAction");


  if (args.path)
  {
    for each (path in argsM['path'])
    {
      //change:  /app:company_home/st:sites/cm:sandbox/cm:wiki/cm:1a
      // into: /app:company_home/st:sites/cm:sandbox/cm:wiki/cm:_x0031_a (or whatever ISO 9075 is for a '1'
      var indexOfLeafPart = path.lastIndexOf(':') + 1;
      var partPart = path.substring( 0, indexOfLeafPart);
      var leafPart = path.substring( indexOfLeafPart);
      var nodePath = partPart + search.ISO9075Encode(leafPart);
      action.execute(search.luceneSearch('+PATH:"' + nodePath + '"')[0]);
    }
  }
  else
  {
    for each (ref in argsM['nodeRef'])
    {
      action.execute(search.findNode(ref));
    }
  }

  model.success=true;
}
catch (err)
{
  model.success=false;
  status.code=500;
  logger.log(err);
}
