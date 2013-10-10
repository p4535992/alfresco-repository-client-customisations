<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/enhanced-security/lib/enhanced-security.lib.js">

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
      var filename = null,
         content = null,
         mimetype = null,
         siteId = null, site = null,
         containerId = null, container = null,
         destination = null,
         destNode = null,
         thumbnailNames = null,
         i;

      // Upload specific
      var uploadDirectory = null,
         contentType = null,
         aspects = [],
         overwrite = true,  // If a filename clashes for a versionable file
         nod = null,
         pm=null,
         freeformcaveats=null,
         closedmarkings = null,
         organisations = null,
         eyes = null,
         atomal = null,
         tags = [],
         perishableReason = null
         ;

      // Update specific
      var updateNodeRef = null,
         majorVersion = false,
         description = "";
      
      // Prevents Flash- and IE8-sourced "null" values being set for those parameters where they are invalid.
      // Note: DON'T use a "!==" comparison for "null" here.
      var fnFieldValue = function(p_field)
      {
         return field.value.length() > 0 && field.value != "null" ? field.value : null;
      };

      // allow the locale to be set via an argument
      if (args["lang"] != null)
      {
         utils.setLocale(args["lang"]);
      }

      // Parse file attributes
      for each (field in formdata.fields)
      {
         switch (String(field.name).toLowerCase())
         {
         
            case "eslnationalowner":
            	nod=field.value;
            break;
            
            case "eslprotectivemarking":
            	pm=field.value;
            break;
            
            case "eslcaveats":
            	freeformcaveats=field.value;
            	//Note that this is less logic than is in the client-side javascript
				//We're not trying to replicate the formatting here, just protect against XSS
				//and the formatting rules happen to make it easy
                freeformcaveats=freeformcaveats.replaceAll('[^a-zA-Z ]+','');
            break;
            
            case "eslclosedgroupshidden":
            	closedmarkings=field.value;
            break;
            
            case "eslorganisationshidden":
            	organisations=field.value;
            break;
            
            case "esleyes":
            	eyes=field.value;
            break;
            
            case "eslnationalcaveats":
			    eyes=field.value;
            break;
         
            case "eslatomal":
                atomal=field.value;
            break;
         
            case "filedata":
               if (field.isFile)
               {
                  filename = fileNameNormaliser.normalise(field.filename);
                  content = field.content;
                  mimetype = field.mimetype;
               }
               break;

            case "siteid":
               siteId = fnFieldValue(field);
               break;

            case "containerid":
               containerId = fnFieldValue(field);
               break;

            case "destination":
               destination = fnFieldValue(field);
               break;

            case "uploaddirectory":
               uploadDirectory = fnFieldValue(field);
               if (uploadDirectory !== null)
               {
                  // Remove any leading "/" from the uploadDirectory
                  if (uploadDirectory.substr(0, 1) == "/")
                  {
                     uploadDirectory = uploadDirectory.substr(1);
                  }
                  // Ensure uploadDirectory ends with "/" if not the root folder
                  if ((uploadDirectory.length > 0) && (uploadDirectory.substring(uploadDirectory.length - 1) != "/"))
                  {
                     uploadDirectory = uploadDirectory + "/";
                  }
               }
               break;

            case "updatenoderef":
               updateNodeRef = fnFieldValue(field);
               break;

            case "description":
               description = field.value;
               break;

            case "contenttype":
               contentType = field.value;
               break;

            case "aspects":
               aspects = field.value != "-" ? field.value.split(",") : [];
               break;

            case "majorversion":
               majorVersion = field.value == "true";
               break;

            case "overwrite":
               overwrite = field.value == "true";
               break;

            case "thumbnails":
               thumbnailNames = field.value;
               break;
            
            case "tags":
            	tags = field.value.split(" ");
            	var tempTags=[];
            	for (var i=0; i < tags.length; i++)
            	{
            		if (tags[i].trim() != "")
            		{
            			tempTags.push(tags[i].replaceAll("[^\u0000-\u007F]+","_"));
            		}
            	}
            	tags=tempTags;
            	break;
            	
            case "perishable":
                perishableReason = field.value ? field.value : null;
                break;
         }
      }      
      
      //The Flash uploaded in Share may have translated the empty string to "null", 
      //so if we have a null (never a valid value), translate it back to the empty string
      if (eyes==null)
      {
        eyes="";
      }

      if (closedmarkings!=null)
      {
          closedmarkings=closedmarkings+atomalToGroups(atomal, closedmarkings.length()>0);
      }
      else
      {
          closedmarkings=atomalToGroups(atomal, false);
      }

      // Ensure mandatory file attributes have been located. Need either destination, or site + container or updateNodeRef
      if ((filename === null || content === null) || (destination === null && (siteId === null || containerId === null) && updateNodeRef === null))
      {
         status.code = 400;
         status.message = "Required parameters are missing";
         status.redirect = true;
         return;
      }

      /**
       * Site or Non-site?
       */
      if (siteId !== null)
      {
         /**
          * Site mode.
          * Need valid site and container. Try to create container if it doesn't exist.
          */
         site = siteService.getSite(siteId);
         if (site === null)
         {
            status.code = 404;
            status.message = "Site (" + siteId + ") not found.";
            status.redirect = true;
            return;
         }

         container = site.getContainer(containerId);
         if (container === null)
         {
            try
            {
               // Create container since it didn't exist
               container = site.createContainer(containerId);
            }
            catch(e)
            {
               // Error could be that it already exists (was created exactly after our previous check) but also something else
               container = site.getContainer(containerId);
               if (container === null)
               {
                  // Container still doesn't exist, then re-throw error
                  throw e;
               }
               // Since the container now exists we can proceed as usual
            }
         }

         if (container === null)
         {
            status.code = 404;
            status.message = "Component container (" + containerId + ") not found.";
            status.redirect = true;
            return;
         }
         
         destNode = container;
      }
      else if (destination !== null)
      {
         /**
          * Non-Site mode.
          * Need valid destination nodeRef.
          */
         destNode = search.findNode(destination);
         if (destNode === null)
         {
            status.code = 404;
            status.message = "Destination (" + destination + ") not found.";
            status.redirect = true;
            return;
         }
      }

      /**
       * Update existing or Upload new?
       */
      if (updateNodeRef !== null)
      {
         /**
          * Update existing file specified in updateNodeRef
          */
         var updateNode = search.findNode(updateNodeRef);
         if (updateNode === null)
         {
            status.code = 404;
            status.message = "Node specified by updateNodeRef (" + updateNodeRef + ") not found.";
            status.redirect = true;
            return;
         }
         
         if (updateNode.isLocked)
         {
            // We cannot update a locked document
            status.code = 404;
            status.message = "Cannot update locked document '" + updateNodeRef + "', supply a reference to its working copy instead.";
            status.redirect = true;
            return;
         }

         if (!updateNode.hasAspect("cm:workingcopy"))
         {
            // Ensure the file is versionable (autoVersion = true, autoVersionProps = false)
            updateNode.ensureVersioningEnabled(true, false);

            // It's not a working copy, do a check out to get the actual working copy
            updateNode = updateNode.checkoutForUpload();
         }

         // Update the working copy content
         updateNode.properties.content.write(content);
         // Reset working copy mimetype and encoding
         updateNode.properties.content.guessMimetype(filename);
         updateNode.properties.content.guessEncoding();
         // check it in again, with supplied version history note
         updateNode = updateNode.checkin(description, majorVersion);
         
         //Iff we've got a PM (mandatory property of an ESL) then populate the whole ESL
	 	 if (pm != null && pm != "") {
	 	 	if (nod==null)
	 	 	{
	 	 		nod="";
	 	 	}
	 	 	
	 	 	updateNode.properties["es:nod"] = nod;
	 	 	updateNode.properties["es:pm"] = pm;
	 	 	updateNode.properties["es:freeFormCaveats"] = freeformcaveats;
	 	 	updateNode.properties["es:nationalityCaveats"] = eyes;
	 	 	if (closedmarkings != null && closedmarkings != "") {
	 	 		updateNode.properties["es:closedMarkings"]=closedmarkings.split(",");
	 	 	}
	 	 	else {
	 	 		updateNode.properties["es:closedMarkings"] = null;
	 	 	}
	 	 	if (organisations != null && organisations != "") {
	 	 		updateNode.properties["es:organisations"] = organisations.split(",");
	 	 	}
	 	 	else {
	 	 		updateNode.properties["es:organisations"] = null;
	 	 	}
	 	 	updateNode.save();
	 	}

         model.document = updateNode;
      }
      else
      {
         /**
          * Upload new file to destNode (calculated earlier) + optional subdirectory
          */
         if (uploadDirectory !== null && uploadDirectory.length > 0)
         {
            destNode = destNode.childByNamePath(uploadDirectory);
            if (destNode === null)
            {
               status.code = 404;
               status.message = "Cannot upload file since upload directory '" + uploadDirectory + "' does not exist.";
               status.redirect = true;
               return;
            }
         }

         /**
          * Exitsing file handling.
          */
         var existingFile = destNode.childByNamePath(filename);
         if (existingFile !== null)
         {
            // File already exists, decide what to do
            if (existingFile.hasAspect("cm:versionable") && overwrite)
            {
               // Upload component was configured to overwrite files if name clashes
               existingFile.properties.content.write(content);

               // Reapply mimetype as upload may have been via Flash - which always sends binary mimetype
               existingFile.properties.content.guessMimetype(filename);
               existingFile.properties.content.guessEncoding();
               existingFile.save();

               model.document = existingFile;
               // We're finished - bail out here
               return;
            }
            else
            {
               // Upload component was configured to find a new unique name for clashing filenames
               var counter = 1,
                  tmpFilename,
                  dotIndex;

               while (existingFile !== null)
               {
                  dotIndex = filename.lastIndexOf(".");
                  if (dotIndex == 0)
                  {
                     // File didn't have a proper 'name' instead it had just a suffix and started with a ".", create "1.txt"
                     tmpFilename = counter + filename;
                  }
                  else if (dotIndex > 0)
                  {
                     // Filename contained ".", create "filename-1.txt"
                     tmpFilename = filename.substring(0, dotIndex) + "-" + counter + filename.substring(dotIndex);
                  }
                  else
                  {
                     // Filename didn't contain a dot at all, create "filename-1"
                     tmpFilename = filename + "-" + counter;
                  }
                  existingFile = destNode.childByNamePath(tmpFilename);
                  counter++;
               }
               filename = tmpFilename;
            }
         }

         /**
          * Create a new file.
          */
         var newFile = destNode.createFile(filename);
         if (contentType !== null)
         {
            newFile.specializeType(contentType);
         }
         newFile.properties.content.write(content);

         // Reapply mimetype as upload may have been via Flash - which always sends binary mimetype
         newFile.properties.content.guessMimetype(filename);
         newFile.properties.content.guessEncoding();
         newFile.save();         

         // Create thumbnail?
         if (thumbnailNames != null)
         {
            var thumbnails = thumbnailNames.split(","),
               thumbnailName = "";

            for (i = 0; i < thumbnails.length; i++)
            {
               thumbnailName = thumbnails[i];
               if (thumbnailName != "" && thumbnailService.isThumbnailNameRegistered(thumbnailName))
               {
                  newFile.createThumbnail(thumbnailName, true);
               }
            }
         }

         //If we've got a PM (mandatory property of an ESL) then populate the whole ESL
         if (pm != null && pm != "") {
	         if (nod==null)
 		     {
 	           nod="";
 	         }
        	 newFile.properties["es:nod"] = nod;
        	 newFile.properties["es:pm"] = pm;
        	 newFile.properties["es:freeformcaveats"] = freeformcaveats;
        	 newFile.properties["es:nationalityCaveats"] = eyes;
        	 if (closedmarkings != null && closedmarkings != "") {
			newFile.properties["es:closedMarkings"]=closedmarkings.split(",");
        	 }
        	 else {
        	 	newFile.properties["es:closedMarkings"] = null;
        	 }
        	 if (organisations != null && organisations != "") {
        		newFile.properties["es:organisations"] = organisations.split(",");
        	 }
        	 else {
        	 	newFile.properties["es:organisations"] = null;
        	 }
        	 newFile.save();
         }         
         // Additional aspects?
         if (aspects.length > 0)
         {
            for (i = 0; i < aspects.length; i++)
            {
               newFile.addAspect(aspects[i]);
            }
         }

         // Extract metadata - via repository action for now.
         // This should use the MetadataExtracter API to fetch properties, allowing for possible failures.
         var emAction = actions.create("extract-metadata");
         if (emAction != null)
         {
            // Call using readOnly = false, newTransaction = false
            emAction.execute(newFile, false, false);
         }
         
         model.document = newFile;
      }
      
      // Perishability
      if(perishableReason) {
     	 managedDeletion.setPerishable(model.document, perishableReason);
      }
      
      //We've uploaded and saved the file, so now we can add the specified tags
      //We start by removing all the existing tags
      model.document.removeTags(model.document.getTags());
      if (tags!=null && tags.length > 0)
      {
          model.document.addTags(tags);    	  
      }
   }
   catch (e)
   {
	      var x = e;

	      status.code = 500;
	      status.message = "Unexpected error occured during upload of new content.";

	      if (x.message)
	      {
			 if (x.message.indexOf("org.alfresco.service.cmr.usage.ContentQuotaException") == 0)
	         {
	            status.code = 413;
	         }
	         else if (x.message.indexOf("org.alfresco.repo.security.permissions.AccessDeniedException") == 0)
	         {
	            status.code = 409;
	         }
	      }

	      status.redirect = true;
	      throw e;
   }
}

main();
