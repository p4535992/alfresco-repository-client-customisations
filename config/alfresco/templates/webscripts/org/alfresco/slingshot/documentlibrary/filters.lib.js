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
var Filters =
{
   /**
    * Type map to filter required types.
    * NOTE: "documents" filter also returns folders to show UI hint about hidden folders.
    */
   TYPE_MAP:
   {
      "documents": '+(TYPE:"content" OR TYPE:"app:filelink" OR TYPE:"folder")',
      "folders": '+(TYPE:"folder" OR TYPE:"app:folderlink")',
      "images": '-TYPE:"thumbnail" +@cm\\:content.mimetype:image/*'
   },

   /**
    * Encode a path with ISO9075 encoding
    *
    * @method iso9075EncodePath
    * @param path {string} Path to be encoded
    * @return {string} Encoded path
    */
   iso9075EncodePath: function Filter_iso9075EncodePath(path)
   {
      var parts = path.split("/");
      for (var i = 1, ii = parts.length; i < ii; i++)
      {
         parts[i] = "cm:" + search.ISO9075Encode(parts[i]);
      }
      return parts.join("/");
   },

   /**
    * Create filter parameters based on input parameters
    *
    * @method getFilterParams
    * @param filter {string} Required filter
    * @param parsedArgs {object} Parsed arguments object literal
    * @param optional {object} Optional arguments depending on filter type
    * @return {object} Object literal containing parameters to be used in Lucene search
    */
   getFilterParams: function Filter_getFilterParams(filter, parsedArgs, optional)
   {
	  var sortField = args.sortField ? args.sortField : "@cm:name";
	   
      var filterParams =
      {
         query: "+PATH:\"" + parsedArgs.pathNode.qnamePath + "/*\"",
         limitResults: null,
         sort: [
         {
            column: sortField,
            ascending: (args.sortAsc != "false")
         }],
         language: "lucene",
         templates: null,
         variablePath: true
      };

      optional = optional || {};

      // Max returned results specified?
      var argMax = args.max;
      if ((argMax !== null) && !isNaN(argMax))
      {
         filterParams.limitResults = argMax;
      }

      var favourites = optional.favourites;
      if (typeof favourites == "undefined")
      {
         favourites = [];
      }

      // Create query based on passed-in arguments
      var filterData = String(args.filterData),
         filterQuery = "";

      // Common types and aspects to filter from the UI - known subtypes of cm:content and cm:folder
      var filterQueryDefaults =
         " -TYPE:\"cm:thumbnail\"" +
         " -TYPE:\"cm:failedThumbnail\"" +
         " -TYPE:\"cm:systemfolder\"" +
         " -TYPE:\"fm:forums\"" +
         " -TYPE:\"fm:forum\"" +
         " -TYPE:\"fm:topic\"" +
         " -TYPE:\"fm:post\"";

      switch (String(filter))
      {
         case "all":
            filterQuery = "+PATH:\"" + parsedArgs.rootNode.qnamePath + "//*\"";
            filterQuery += " +TYPE:\"cm:content\"";
            filterParams.query = filterQuery + filterQueryDefaults;
            break;

         case "recentlyAdded":
         case "recentlyModified":
         case "recentlyCreatedByMe":
         case "recentlyModifiedByMe":
            var onlySelf = (filter.indexOf("ByMe")) > 0 ? true : false,
               dateField = (filter.indexOf("Modified") > 0) ? "modified" : "created",
               ownerField = (dateField == "created") ? "creator" : "modifier";

            // Default to 7 days - can be overridden using "days" argument
            var dayCount = 7,
               argDays = args.days;
            if ((argDays !== null) && !isNaN(argDays))
            {
               dayCount = argDays;
            }

            // Default limit to 50 documents - can be overridden using "max" argument
            if (filterParams.limitResults === null)
            {
               filterParams.limitResults = 50;
            }

            var date = new Date();
            var toQuery = date.getFullYear() + "\\-" + (date.getMonth() + 1) + "\\-" + date.getDate();
            date.setDate(date.getDate() - dayCount);
            var fromQuery = date.getFullYear() + "\\-" + (date.getMonth() + 1) + "\\-" + date.getDate();

            filterQuery = this.constructPathQuery(parsedArgs);
            filterQuery += " +@cm\\:" + dateField + ":[" + fromQuery + "T00\\:00\\:00.000 TO " + toQuery + "T23\\:59\\:59.999]";
            if (onlySelf)
            {
               filterQuery += " +@cm\\:" + ownerField + ":\"" + person.properties.userName + '"';
            }
            filterQuery += " +TYPE:\"cm:content\"";

            filterParams.sort = [
            {
               column: "@cm:" + dateField,
               ascending: false
            }];
            filterParams.query = filterQuery + filterQueryDefaults;
            break;

         case "editingMe":
            filterQuery = this.constructPathQuery(parsedArgs);
            filterQuery += " +ASPECT:\"workingcopy\"";
            filterQuery += " +@cm\\:workingCopyOwner:\"" + person.properties.userName + '"';
            filterParams.query = filterQuery;
            break;

         case "editingOthers":
            filterQuery = this.constructPathQuery(parsedArgs);
            filterQuery += " +ASPECT:\"workingcopy\"";
            filterQuery += " -@cm\\:workingCopyOwner:\"" + person.properties.userName + '"';
            filterParams.query = filterQuery;
            break;

         case "favourites":
            var foundOne = false;

            for (var favourite in favourites)
            {
               if (foundOne)
               {
                  filterQuery += " OR ";
               }
               foundOne = true;
               filterQuery += "ID:\"" + favourite + "\"";
            }
            
            if (filterQuery.length > 0)
            {
               filterQuery = "+(" + filterQuery + ") " + this.constructPathQuery(parsedArgs);
            }
            else
            {
               // empty favourites query
               filterQuery = "+ID:\"\"";
            }
            
            filterParams.query = filterQuery;
            break;

         case "node":
            filterParams.variablePath = false;
            filterParams.query = "+ID:\"" + parsedArgs.nodeRef + "\"";
            break;

         case "tag":
            // Remove any trailing "/" character
            if (filterData.charAt(filterData.length - 1) == "/")
            {
               filterData = filterData.slice(0, -1);
            }
            filterQuery = this.constructPathQuery(parsedArgs);
            filterParams.query = filterQuery + " +PATH:\"/cm:taggable/cm:" + search.ISO9075Encode(filterData) + "/member\"";
            break;

         case "category":
            // Remove any trailing "/" character
            if (filterData.charAt(filterData.length - 1) == "/")
            {
               filterData = filterData.slice(0, -1);
            }
            filterParams.query = "+PATH:\"/cm:generalclassifiable" + Filters.iso9075EncodePath(filterData) + "/member\"";
            break;

         default: // "path"
            filterParams.variablePath = false;
            filterQuery = "+PATH:\"" + parsedArgs.pathNode.qnamePath + "/*\"";
            filterParams.query = filterQuery + " AND NOT ASPECT:\"sys:hidden\" " + filterQueryDefaults;
            break;
      }

      // Specialise by passed-in type
      if (filterParams.query !== "")
      {
         filterParams.query += " " + (Filters.TYPE_MAP[parsedArgs.type] || "");
      }

      return filterParams;
   },
   
   constructPathQuery: function constructPathQuery(parsedArgs)
   {
      var pathQuery = "";
      if (parsedArgs.nodeRef != "alfresco://company/home")
      {
         pathQuery = "+PATH:\"" + parsedArgs.rootNode.qnamePath;
         if (parsedArgs.nodeRef == "alfresco://sites/home")
         {
            pathQuery += "/*/cm:documentLibrary";
         }
         pathQuery += "//*\"";
      }
      return pathQuery;
   }
};
