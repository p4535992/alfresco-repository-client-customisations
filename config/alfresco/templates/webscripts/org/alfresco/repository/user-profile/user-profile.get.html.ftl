<!--
    Copyright (C) 2008-2010 Surevine Limited.
      
    Although intended for deployment and use alongside Alfresco this module should
    be considered 'Not a Contribution' as defined in Alfresco'sstandard contribution agreement, see
    http://www.alfresco.org/resource/AlfrescoContributionAgreementv2.pdf
    
    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<script language='javascript'>

//Record whether the user has modified any fields on the form.  This will sometimes record a modification when the data
//hasn't changed (for instance, when the user changes a field then changes it back, or when the only key press they've
//made is forbidden by one of the keydown handlers), which we accept

var fieldChanged=false; 

  function controlChange()
  {
      if (!fieldChanged)
      {
          fieldChanged=true;

          var saveButton = document.getElementById("save");
          saveButton.disabled=false;
          saveButton.innerHTML="Save";
      }
  }

  //Remind the user, in various browsers, to save the page before exiting
  window.onbeforeunload = function() { if (fieldChanged) { return "You have unsaved changes.  Navigating away from this page will abandon your changes.  Are you sure?";} }
  document.onbeforeunload = function() { if (fieldChanged) { return "You have unsaved changes.  Navigating away from this page will abandon your changes.  Are you sure?";} }
  
  function createNewTelephoneRow(controlLabel, element)
  {
      var list=document.getElementById("profile-edit-"+controlLabel+"-list");
      var children = getChildren(list);
      
      var inputs = getChildren(getLastChild(children));
      var newIndex=children.length; 
      if (inputs==null || inputs[0]==element || inputs[1]==element || inputs[2]==element)
      {
        var newRow= "<input type='text' id='profile-edit-"+controlLabel+"-"+newIndex+"-network' class='editTextInput network  input-blurred' onchange='inputToUpper(this)' onkeydown='controlChange(); createNewTelephoneRow(\""+controlLabel+"\", this); return lettersNumbersDashOnly(event)' value='Network' onfocus='tooltipFocus(\"Network\",this)' onblur='tooltipBlur(\"Network\",this)'/>"
                  +" <input type='text' id='profile-edit-"+controlLabel+"-"+newIndex+"-number' class='editTextInput number  input-blurred' onchange='telephonePatternChange(this)' onkeydown='controlChange(); createNewTelephoneRow(\""+controlLabel+"\", this); return telephonePatternKD(event)' value='Number' onfocus='tooltipFocus(\"Number\",this)' onblur='tooltipBlur(\"Number\",this)'/> <span class='value'>Ext. </span>"
                  +" <input type='text' id='profile-edit-"+controlLabel+"-"+newIndex+"-extension' class='editTextInput extension  input-blurred' onchange='telephonePatternChange(this)' onkeydown='controlChange(); createNewTelephoneRow(\""+controlLabel+"\", this); return telephonePatternKD(event)' value='Extension' onfocus='tooltipFocus(\"Extension\",this)' onblur='tooltipBlur(\"Extension\",this)'/>";
        var newLi = document.createElement("li");
        list.appendChild(newLi);
        newLi.innerHTML=newRow;
      }
  }

  function createNewListRow(controlLabel, element)
  {
      var list=document.getElementById("profile-edit-"+controlLabel.replace(/ /g, "_")+"-list");
      var children = getChildren(list);
            
      var inputs = getChildren(getLastChild(children)); 
      var newIndex=children.length;

      if (inputs==null || inputs[0]==element)
      {
        var newRow ="<input type='text' id='profile-edit-"+controlLabel.replace(/ /g, "_")+"-"+newIndex+"' class='editTextInput' onkeydown='controlChange();' maxlength='" + element.getAttribute('maxlength') + "' onfocus='createNewListRow(\""+controlLabel+"\", this)'></input>";               
        var newLi = document.createElement("li");
        list.appendChild(newLi);
        newLi.innerHTML=newRow;
      }
  }

  /**
  * Included for firefox 2.0.0.1 compatability purposes.  Get the last child in the input array that is an element
  **/
  function getLastChild(array)
  {
      if (array==null || array.length==0)
      {
          return null;
      }
      var index = array.length-1;
      while (index>=0)
      {
          if (array[index].nodeType==1)
          {
              return array[index];
          }
          else
          {
              index--;
          }
      }
      throw "Array "+array+" contains no element nodes";
  }

  /**
  * Included for firefox 2.0.0.1 compatability purposes.  Get the element children of the given element
  **/
  function getChildren(element)
  {
      if (element==null)
      {
          return null;
      }
      //If we support the simple way of doing this, just do that
      var rV = element.children;
      if (rV!=null)
      {
          return rV;
      }
      //Oh dear, we're using an antique version of Firefox, so we have to filter out everything that isn't nodetype==1
      //(ie. an element) from our return value
      var nodes = element.childNodes;
      rV=[];
          
      for (var i=0; i < nodes.length; i++)
      {
          if (nodes[i].nodeType==1)
          {
              rV.push(nodes[i]);
          }
      }
      return rV;
  }

  function lettersNumbersDashOnly(e)
  {
      var key = e.keyCode;
      
      // spaces and all control codes are allowed
      if (key <= 46)
      {
          return true;
      }
      
      // letters are allowed
      if (key >= 65 && key <= 90)
      {
          return true;
      }

      //0-9
      if ((key>=48 && key<=57) || (key>=96 && key <=105))
      {
          return true;
      }

      //both - (numpad & keyboard)
      if (key==109 || key==189)
      {
          return true;
      }
      
      return false;
      
  }

  function telephonePatternKD(e)
  {
      var key = e.keyCode;

      // spaces and all control codes are allowed
      if (key <= 46)
      {
          return true;
      }
      
      //0-9
      if (key>=48 && key<=57)
      {
          return true;
      }

      //+, * or #
      if (key==107 || key == 106 || key == 222)
      {
          return true;
      }
      return false;
  }

  /**
   * Only allow the characters specified in the english schema document - we're already restricted by telephonePatternKD
   * so all we need to worry about here is that someone held down the shift key while entering the number. Note that
   * the regex also trims leading and trailing spaces.
   **/
  function telephonePatternChange(element)
  {
      element.value = element.value.replace(/^\s+|[^0-9()+*# ]+|\s+$/g,""); // Strip out invalid characters
  } 

  /**
   * Convert the input value to upper case. Note that the last regex also trims leading and trailing spaces.
   **/
  function inputToUpper(element)
  {
      element.value = element.value.toUpperCase().replace(/\s+/g, " ").replace(/^\s+|[^A-Z\- ]|\s+$/g, "");
  }

  /**
  * Assemble some JSON from the values present in the form and then delegate to the embedding application
  * the responsiblity for submitting the form.  This is to allow the embedding application to take full advantage
  * of whatever AJAX framework it is (probably) using.  The delegation is achieved by the simple expediant of assuming
  * that the embedding application has already defined a global function named com_surevine_sendProfilePostRequest
  **/
  function saveProfile()
  {
      var json = assembleJSON();
      var serviceURL="sv-theme/user-profile/profile";
      indicateProfileSaveInProgress();
      com_surevine_sendProfilePostRequest(json, serviceURL);
  }

  function assembleJSON()
  {
      var out ='{"userName":"${profile.userName}","biography":"'+getBiography()+'","askMeAbouts":[';
      var amas = getAskMeAbouts();
      for (var i=0; i < amas.length; i++)
      {
          out+='"'+amas[i]+'"';
          if (i < amas.length-1)
          {
              out+=',';
          }
      }
      out+='],"telephones":[';
      var telephones = getTelephones();
      for (var i=0; i < telephones.length; i++)
      {
          out += '{"number":"'+telephones[i].number+'","network":"'+telephones[i].network+'","extension":"'+telephones[i].extension+'"}';
          if (i < telephones.length-1)
          {
              out+=',';
          }
      }
      out+=']}';
      return out;
  }
  
  function setAvatarImage(frame)
  {
  
    if (frame == null) {
        return;
    }
    var content = frame.contentWindow.document.body.innerHTML;
    if (content == null) {
        content = frame.contentWindow.document.body.firstChild.innerHTML;
    }
    if (content == null) {
        return;
    }
    var matches=content.match('nodeRef.?\\s*:\\s*["\']([^"\']+)["\']');
    if (matches && matches[1]) {
        var nodeRef=matches[1];
        document.getElementById("avatar-image").src="/share/proxy/alfresco/api/node/"+nodeRef.replace('://','/')+"/content/thumbnails/avatar?c=force";
        setAvatarVersionCookie();
    }
    
  }
  
  // Set version cookie for avatar anti-cache
  function setAvatarVersionCookie()
  {
	var avatarVersion = YAHOO.util.Cookie.get("alfresco-avatarVersion", Number);
	if(avatarVersion != undefined && avatarVersion != null) {
		YAHOO.util.Cookie.set("alfresco-avatarVersion", avatarVersion+1, {
			path: "/share/"
		});
	}
	else {
		YAHOO.util.Cookie.set("alfresco-avatarVersion", 1, {
			path: "/share/"
		});
	}
  }

  function getBiography()
  {
      var element = document.getElementById('profile-edit-Personal_Information');
      var text=sanitise(element.value);
      //The keydown handler on the text box should have kept the limit < 2000 charecters, but just in case some 
      //trickery has been going on...
      if (text.length>1900)
      {
          text=text.substr(0, 1899);
      }
      return text.replace(/\r/g,"").replace(/\n/g,"\\n");
  }

  function getAskMeAbouts()
  {
      var amas=[];
      var listItems = getChildren(document.getElementById('profile-edit-Ask_Me_About-list'));
      for (var i=0; i<listItems.length; i++)
      {
            //The text box with the value in is the first(only) child of the list item it's in
          var ama = sanitise(getChildren(listItems[i])[0].value);
          if (ama!=null && ama.length>0) //Don't push blank data
          {
              amas.push(ama);
          }
      }
      return amas;     
  }
  
  function displayProfileLoading() 
  {
        document.getElementById("avatar-image").src="/share/images/loading.gif";
  }

  function getTelephones()
  {
      var telephones=[];
      var listItems = getChildren(document.getElementById('profile-edit-Telephone-list'));
      for (var i=0; i<listItems.length; i++)
      {
          var telephone=new Object();
          telephone.network=sanitise(getChildren(listItems[i])[0].value);
          if (telephone.network=="Network")
          {
              telephone.network="";
          }
          telephone.number=sanitise(getChildren(listItems[i])[1].value);
          if (telephone.number=="Number")
          {
              telephone.number="";
          }
          telephone.extension=sanitise(getChildren(listItems[i])[3].value);
          if (telephone.extension=="Extension")
          {
              telephone.extension="";
          }
          
          //Only push telephone numbers with a number, convert blank networks into "Unknown" (to be LDAP-schema-compliant)
          if (telephone.network==null || telephone.network.length==0)
          {
              telephone.network="UNKNOWN";
          }
          if (telephone.number!=null && telephone.number.length>0)
          {
                telephones.push(telephone);
          }
      }
      return telephones;   
  }

  /**
  * Javascript sanitisation function.  We still need to protect against XSS at the back end because it's
  * trivial for the user to bypass this function. This function is just to help prevent us from creating broken JSON
  **/
  function sanitise(text)
  {
      if (text==null)
      {
          return null;
      }
      return text.replace(/\"/g, "'");
  }

  /**
  * Namespaced function forming part of the interface betweeen this form and the embedding application.  The embedding
  * application can, if it wishes, call this method to indicate that the user's profile has been saved succesfully.  
  * Calling this method is optional, the embedding application may wish to do it's own thing.
  */
  function com_surevine_indicateProfileSaveSuccess()
  {
      var saveButton = document.getElementById("save");
      saveButton.disabled=false;
      saveButton.innerHTML="Profile Saved";
      saveButton.disabled=true;
      fieldChanged=false;
  }

  /**
  * Function that the embedding application can use to indicate to the user that an error has occured while saving their
  * profile.  The embedding application is not required to use this method. 
  * @param error Object describing an error, containing a "serverResponse->status" object describing the HTTP response
  * code generated by the underlying service
  */
  function com_surevine_indicateProfileSaveFailure(error)
  {
      var saveButton = document.getElementById("save");
      saveButton.disabled=false;
      
      saveButton.innerHTML="Save";
      var message='Response code: '+error.serverResponse.status;
      alert('Profile Save Failed.  '+message+'.  Please try again later, and contact support if this issue persists');
  }

  /**
  * Similar to the above two functions, but note that this function is called from within this form and does _not_ form
  * part of the interface between the form and the embedding application
  */
  function indicateProfileSaveInProgress()
  {
      var saveButton = document.getElementById("save");
      saveButton.innerHTML="Saving...";
      saveButton.disabled=true;   
  }

  function trimToLength(length, control)
  {
       if (control.value.length>length-4)
       {
           control.value=control.value.substr(0, length-5);
       }
  }

  function useCSS(stylesheetLocation)
  {
      var headID = document.getElementsByTagName("head")[0];         
      var cssNode = document.createElement('link');
      cssNode.type = 'text/css';
      cssNode.rel = 'stylesheet';
      cssNode.href = stylesheetLocation;
      cssNode.media = 'screen';
      headID.appendChild(cssNode);
  }

  function tooltipFocus(tooltip, control)
  {
      if(control.value == tooltip) {
          control.value = "";
          removeClass(control, "input-blurred");
      }
  }

  function tooltipBlur(tooltip, control) {
      if(control.value == "") {
          control.value = tooltip;
          addClass(control, "input-blurred");
      }
  };

  /**
   * Utility functions imported from our CAS front end to add/remove CSS classes from elements 
   */
  function removeClass(target, classValue)
  {
      var removedClass = target.className;
      var pattern = new RegExp("(^| )" + classValue + "( |$)");

      removedClass = removedClass.replace(pattern, "$1");
      removedClass = removedClass.replace(/ $/, "");
      target.className = removedClass;

      return true;
  }

  function addClass(target, classValue)
  {
    
      var pattern = new RegExp("(^| )" + classValue + "( |$)");

      if (!pattern.test(target.className))
      {
          if (target.className == "")
          {
              target.className = classValue;
          }
          else
          {
              target.className += " " + classValue;
          }
      }

      return true;
  }
  
  
</script>

    <#macro textControl controlLabel controlValue>
    <div class='row'>
        <#if editMode==true>
            <label class='label' for='profile-edit-${controlLabel}'>${controlLabel}</label>
            <input type='text' id='profile-edit-${controlLabel}' class='editTextInput' value="${controlValue}"></input>
        <#else>
            <span class='label'>${controlLabel}:</span>
            <span class='value'>${controlValue}</span>
        </#if>
    </div>
    </#macro>
    
    <#macro richTextControl controlLabel controlValue>
    <div class='row scroll'>
        <#if editMode==true>
            <label class='label' for='profile-edit-${controlLabel?replace(" ","_")}'>${controlLabel}:</label>
            <textarea rows='6' cols='30' maxLength="1900" onkeydown='controlChange();' id='profile-edit-${controlLabel?replace(" ","_")}' class='value edit'>${controlValue}</textarea>
        <#else>
            <span class='label'>${controlLabel}:</span>
            <div class='value'>${controlValue?replace("\n","<br/>")}</div>
        </#if>
    </div>
    </#macro>
    
    <#macro readOnlyTextControl controlLabel controlValue>
    <div class='row'>
          <span class='label'>${controlLabel}:</span>
          <span class='value'>${controlValue}</span>
    </div>
    </#macro>
    
    <#macro listControl controlLabel controlValue>
        <div class='row'>
            <#if editMode==true>
                <label class='label' for='profile-edit-${controlLabel}'>${controlLabel}:</label>
                <ul id='profile-edit-${controlLabel?replace(" ","_")}-list'>
                    <#list controlValue as value>
                        <li><input type='text' onkeydown='controlChange();' maxlength='1024' id='profile-edit-${controlLabel?replace(" ","_")}-${value_index}' class='editTextInput' value="${value}"></input></li>
                    </#list>
                    <li>
                       <input type='text' onkeydown='controlChange();' maxlength='1024' id='profile-edit-${controlLabel?replace(" ","_")}-${controlValue?size}' class='editTextInput' onfocus='createNewListRow("${controlLabel}", this)'/>
                    </li>
                </ul>
            <#else>
                <span class='label'>${controlLabel}:</span>
                <span class='listContainer'>
                    <#list controlValue as value>
                        <span class='value list'><a href="${searchBase}${value}">${value}</a></span>
                    </#list>
                </span>
            </#if>
        </div>
    </#macro>
    
    <#macro telephoneControl controlLabel controlValue>
        <div class='row telephone'>
            <#if editMode==true>
                <label class='label' for='profile-edit-${controlLabel}'>${controlLabel}:</label>
                <ul id="profile-edit-${controlLabel}-list">
                    <#list controlValue as phone>
                        <li>
                            <input type='text' id='profile-edit-${controlLabel}-${phone_index}-network' class='editTextInput network' value="${phone.network}" onchange='inputToUpper(this)' onkeydown="controlChange(); return lettersNumbersDashOnly(event);" value="Network" onfocus="tooltipFocus('Network',this)" onblur="tooltipBlur('Network',this)"/>
                            <input type='text' id='profile-edit-${controlLabel}-${phone_index}-number' class='editTextInput number' value="${phone.number}" onkeydown="controlChange(); return telephonePatternKD(event)" onchange="telephonePatternChange(this)" value="Number" onfocus="tooltipFocus('Number',this)" onblur="tooltipBlur('Number',this)"/>
                            <span class='value'>Ext.</span>
                            <input type='text' id='profile-edit-${controlLabel}-${phone_index}-extension' class='editTextInput extension' value="${phone.extension}" onkeydown="controlChange(); return telephonePatternKD(event)" onchange="telephonePatternChange(this)" value="Extension" onfocus="tooltipFocus('Extension',this)" onblur="tooltipBlur('Extension',this)"/>
                        </li>
                    </#list>
                    <li>
                       <input type='text' id='profile-edit-${controlLabel}-${controlValue?size}-network' class='editTextInput network input-blurred' onchange='inputToUpper(this)' onkeydown="controlChange(); createNewTelephoneRow('${controlLabel}', this); return lettersNumbersDashOnly(event);" value="Network" onfocus="tooltipFocus('Network',this)" onblur="tooltipBlur('Network',this)"/>
                       <input type='text' id='profile-edit-${controlValue?size}-number' class='editTextInput number input-blurred' onchange='telephonePatternChange(this)' onkeydown="controlChange(); createNewTelephoneRow('${controlLabel}', this); return telephonePatternKD(event)" value="Number" onfocus="tooltipFocus('Number',this)" onblur="tooltipBlur('Number',this)"/>
                       <span class='value'>Ext.</span> 
                       <input type='text' id='profile-edit-${controlValue?size}-extension' class='editTextInput extension input-blurred' onchange='telephonePatternChange(this)' onkeydown="controlChange(); createNewTelephoneRow('${controlLabel}', this); return telephonePatternKD(event)" value="Extension" onfocus="tooltipFocus('Extension',this)" onblur="tooltipBlur('Extension',this)"/>
                    </li>
                </ul>
            <#else>
                <span class='label'>${controlLabel}:</span>
                <ul >
                    <#list controlValue as phone>
                        <li>
                            <span class='value network'>${phone.network}</span>:
                            <span class='value number'>${phone.number}</span>
                            <#if phone.extension?length &gt; 1> <span class='value'>Ext.</span> </#if> <span class='value extension'>${phone.extension}</span>
                        </li>
                    </#list>
                </ul>
            </#if>
        </div>
    </#macro>
    
   <#macro presenceIndicator userName userDisplayName presence>
    <#switch presence.availability>
        <#case "ONLINE">
            <#assign displayAvl = "online">
            <#break>
        <#case "BUSY">
            <#assign displayAvl = "busy">
            <#break>
        <#case "AWAY">
            <#assign displayAvl = "away">
            <#break>
        <#case "OFFLINE">
            <#assign displayAvl = "offline">
            <#break>
        <#default>
            <#assign displayAvl = "unknown">
    </#switch>

    <#if presence.availability == "UNKNOWN">
            <#assign btnTitle = "Unable to retrieve status for ${userDisplayName?html}.">
    <#else>
            <#assign btnTitle = "${userDisplayName?html} is ${displayAvl?html} in chat.">
    </#if>          

    
    <div class="presence">
            <#if presence.availability == "UNKNOWN" || presence.availability == "OFFLINE" || presence.serviceEnabled?string == "false">
                <button class="presence-indicator ${displayAvl}" type="button"  title="${btnTitle}" disabled="disabled">&nbsp;</button>
            <#else>
                <button class="presence-indicator ${displayAvl}" type="button"  title="${btnTitle}" onclick="Alfresco.thirdparty.presence.launchChat('${userName}','${presence.host}')">&nbsp;</button>
            </#if>
            
        <div class="presence-username">
            <span class="theme-color-1" >${userDisplayName?html}</span>
         </div>
        
    </div>
</#macro>

    <script language='javascript'>
       useCSS("${style}");
    </script>
    
        <div class="profileForm">
            <div class='pageTitle title'>User Profile for ${profile.firstName} ${profile.lastName}</div>
            <div class='title'>My Avatar</div>
            <div class='properties'>
                <div class="row photoimg">
                    <img id="avatar-image" class="photoimg" src="/share/proxy/alfresco/sv-theme/user-profile/avatar?user=${profile.userName}" alt="" /> 
                </div>
                <#if editMode==true>
                    <div class="row">
                        <form id="avatar-upload-form" enctype="multipart/form-data" action="/share/proxy/alfresco/sv-theme/user-profile/avatar.html?user=${profile.userName}" method="post" target="avatar-upload-target">
                            <label class='label' for='profile-edit-avatar'>Change Image:</label> <input id="profile-edit-avatar" type="file" name="avatar" size="40" accept="image/*" onchange="displayProfileLoading(); submit();"/>
                            <iframe id="avatar-upload-target" name="avatar-upload-target" src="" style="width:0;height:0;border:0px solid #fff;" onload="setAvatarImage(document.getElementById('avatar-upload-target'));"></iframe>
                        </form>
                    </div> 
                </#if>
            </div>
            
            <#if editMode==true>
                <div class='properties'>Changes to your avatar take effect immediately - there's no need to push 'Save'</div>
            </#if>
            
            
            <div class='title'>My Details</div>
            <div class='properties'>
                  <div class='row'>
                    <span class='label'>Name:</span>
                    <span class='value'><@presenceIndicator userName=profile.userName userDisplayName=profile.firstName+" "+profile.lastName presence=profile.presence/></span>
                    </div>           
              <@readOnlyTextControl controlLabel='Organisation' controlValue=profile.organisation/>
            </div>
            <div class='properties'>
               <@readOnlyTextControl controlLabel='E-mail' controlValue=profile.email/>
               <@telephoneControl controlLabel='Telephone' controlValue=profile.telephones/>
            </div>
            <div class='title'>About Me</div>
            <div class='properties'>
               <@richTextControl controlLabel='Personal Information' controlValue=profile.biography/>
               <@listControl controlLabel='Ask Me About' controlValue=profile.askMeAbouts/>
            </div>
            <#if editMode==true>
                <div class="buttons">
                    <button id="save" type="button" disabled="true" onclick="saveProfile()">Save</button>
                </div>
            </#if>
        </div>
