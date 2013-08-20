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
/**
 * Parse some page arguments and return the parameters specified therein as an object
 * Markings live in the following format  es_validOpenMarkings,OG01,OG02,OG03,OG04;es_validClosedMarkings,CG01....
 * @param args HTTP get arguments formatted in the usual SURF way, with the string to parse contained in a parameter 
 * called "marking".  If the parameter is not specified correctly, behaviour is undefined.
 * @return For each constraint specified in the input parameter, an Obejct containing a name and an array of values, all 
 * wrapped up in an array called "markings" as a property of the return value
 */

function getMarkingFromArgs(args)
{
	var retVal = new Object();
	retVal.markings = [];
	var markingString = args.marking;
	var eachMarking = markingString.split(";");
	for (var i=0; i < eachMarking.length; i++)
	{
		var markingComponents = eachMarking[i].split(",");
		retVal.markings[i] = new Object();
		retVal.markings[i].name=markingComponents[0];
		retVal.markings[i].values=markingComponents.slice(1);
	}
	return retVal;
}

/**
 * Find out who can see items marked with a specified list of constraints and values
 * @TODO If a user doesn't have any values for any of the constraints passed into this function, this function
 * will not include the user in it's results.  This isn't likely to be a problem given how we intend to call this 
 * function, but we may need to revisit it in future.
 * @param markingDetails Array of constraints and constraint values as per the output of a call to getMarkingFromArgs
 * @return An associative array of all the people evaluated (keys) and a boolean value indicating who can see the item
 * (values)
 */
function whoCanSeeMarking(markingDetails)
{
	// For each marking, build an associative array based on who could see the given marking if the operating constraint
	// was the only constraint in the marking, and put each of these into an array
	var constraints=markingDetails.markings;
	var individualMarkingResults=[];
	var numberOfConstraints = constraints.length;
	
	for (var i=0; i < numberOfConstraints; i++)
	{
		individualMarkingResults.push(whoCanSeeMarkingForConstraint(constraints[i]));
	}
	
	//We now merge these individual return values into an aggregate associative array.  If a single constraint
	//denies access for a user (username->false) then access is denied in the aggregate.
	
	var totalMarkingResults =[];
	var authorityCount=[]; // associative array mapping users to the number of times they are mentioned in a constraint
	
	//For each constraint...
	for (var j=0; j < individualMarkingResults.length; j++)
	{
		//...iterate through all the authorities and override whatever the current value for that user is, unless it's
		//false
		for (authority in individualMarkingResults[j])
		{
			//Count the number of times we have seen this user
			if (authorityCount[authority]==null)
			{
				authorityCount[authority]=1;
			}
			else {
				authorityCount[authority]++;
			}
			//If we need to optimise, consider only setting the value if it isn't already true
			if (totalMarkingResults[authority]==null || totalMarkingResults[authority]==true)
			{
				totalMarkingResults[authority]=individualMarkingResults[j][authority];
			}
		}
	}
	
	//We've now got a set of marking results, but we need to filter this to remove users who have only passed some
	//of the constraints.  This happens when they didn't have _any_ values in the missing constraints
	for (authority in authorityCount)
	{
		if (authorityCount[authority]<numberOfConstraints) //We're missing values for at least one constraint, 
			                                               //so override the return value for that user with "false"
		{
			totalMarkingResults[authority]=false;
		}
	}
	
	return totalMarkingResults;
	
}

/**
 * Based on a specific constraint, who could see an item if it was created using the specified list of values
 * @param constraintSpec Contains a value called "name", identifying the constraint, and a value called values, which
 * is an array of constraint values to test
 * @return An associative array of all the authorities who have at least one value of the specified constraint (keys), 
 * and a boolean value indicating who can see the item (values)
 */
function whoCanSeeMarkingForConstraint(constraintSpec)
{
	var retVal = new Object();
	//Use the Javascript interface to the CaveatConfigService to get the constraint, and the list of authorities that
	//constraint applies to
	var constraint = caveatConfig.getConstraint(constraintSpec.name);
	var authorities = constraint.getAuthorities();
	var andLogic = isAndLogic(constraint);
	
	//For each authority...
	for (var j=0; j < authorities.length; j++)
	{
		var authority = authorities[j];
		var authorityName=authority.getAuthorityName();
		var hasAccess=false;

		logger.log("Working out if "+authorityName+" can see constraint");
		//...work out if the authority can see the marking...
		hasAccess=processLogic(constraintSpec, constraint, authorityName,andLogic);
		logger.log("Can "+authorityName+" see"+constraintSpec.name+"?  "+hasAccess);
		//...and record the result
		retVal[authorityName]=hasAccess;
	}
	return retVal;
}

/**
 * Given a list of values, a constraint and an authority name, work out if the given user can see a theoretical item
 * created with that list of values, given the logic of the constraint.  
 * @param constraintSpec Specification of the constraint and list of values as per the return value of getMarkingFromArgs/1
 * @param constraint The constraint itself
 * @param authorityName Name of the authority on whose behalf to evaluate access
 * @param andLogic If true, process as "AND" logic.  If false, use "OR" logic. For readability, all the below comments 
 * assume andLogic==true.  The reader is encouraged to turn the boolean around for OR logic.
 * @return Boolean value indicating if the authority can see the given action
 */
function processLogic(constraintSpec, constraint, authorityName, andLogic)
{
	var realName = constraintSpec.name.replace(/_/,':');
    var authorityValues = caveatConfig.getRmCaveatConfigService().getListDetails(realName)[authorityName].toArray();
   
    logger.log("AuthorityValues: "+authorityValues)
    
    //Special case: If no groups specified, then allow access
    if (constraintSpec.values.length==0)
    {
    	logger.log("Special Case");
    	return true;
    }

    //For each marking specified...
	for (var specified =0; specified < constraintSpec.values.length; specified++)
	{
		logger.log("For "+constraintSpec.values[specified]);
		var accessGranted=false;
		//...compare with each marking the user has access to...
		for (var allowed=0; allowed < authorityValues.length; allowed++)
		{
			logger.log("User"+authorityName+" has: ")+authorityValues[allowed];
			if (constraintSpec.values[specified]==authorityValues[allowed])
			{
				logger.log("Access granted");
				accessGranted=true;
				break;
			}
		}
		//...if we haven't matched yet, then the user doesn't have the specified marking, so return false
		if (accessGranted!=andLogic)
		{
			logger.log("Returning "+!andLogic+" at point A");
			return !andLogic;
		}
	}
	//The user has all the specified markings
	logger.log("Returning "+andLogic+" at point B");
	return andLogic;
}

/**
 * Runs through the security model for the given authority and marking details, returning an object describing the
 * results of the comparison
 * @param authorityName name of the authority to run the comparison "as" 
 * @param markingDetails An object of the form returned by getMarkingFromArgs
 * @return An object describing the results of the comparison:
 * 			authorityName:	[Input parameter supplied]
 * 			result: 		[true|false]
 * 			constraints: 	{collection of...}
 * 							result:		[true|false]
 * 							andLogic:   [true|false] (whether AND or OR logic was used to calculate this constraint)
 * 							values:		{collection of...}
 * 										value:	[Name of the constraint value specified]
 * 										result: [true|false]
 */
function compareAuthorityGroupsWithMarking(authorityName, markingDetails)
{
	//Set up the top level of the results object
	var result = new Object();
	result.authorityName=authorityName;
	result.constraints=[];
	
	var constraints=markingDetails.markings;
	
	//Start by assuming we can see every constraint - this will become the top level "result" value
	var canSeeEveryConstraint=true;
	//Go through each constraint, record whether we can see the object based on this constraint, and why
	for (var i=0; i < constraints.length; i++)
	{
		var resultsPart = new Object();
		var constraintSpec=constraints[i];
		var realName = constraintSpec.name.replace(/_/,':');
		resultsPart.constraintName=realName;
		resultsPart.values=[];
		
		//Shortcut if we have no values
		if (constraintSpec.values.length==0)
		{
			resultsPart.result=true;
			result.constraints.push(resultsPart);
			continue;
		}
		
		//For this constraint, which values does the authority have?
		var authorityValues;
		var allAuthorityValues = caveatConfig.getRmCaveatConfigService().getListDetails(realName)[authorityName];
		
		if (allAuthorityValues) {
			authorityValues = allAuthorityValues.toArray();
		} else {
			authorityValues = [];
		}
		
		//Work out if the constraint uses AND or OR logic*
		var andLogic = isAndLogic(caveatConfig.getConstraint(constraintSpec.name));
		resultsPart.andLogic=andLogic;
		var canSeeConstraint=andLogic;
		
		// Go through every value specified in the constraint and work out if the user can see each value,
		// 
		for (var j=0; j<constraintSpec.values.length; j++)
		{
			var value = constraintSpec.values[j];
			var canSeeValue = valueIsInList(value, authorityValues);
			var valueObj = new Object();
			valueObj.value=value;
			valueObj.result=canSeeValue;
			resultsPart.values.push(valueObj);
			if (andLogic && !canSeeValue)
			{
				canSeeConstraint=false;
			}
			if (!andLogic && canSeeValue)
			{
				canSeeConstraint=true;
			}
		}
		resultsPart.result=canSeeConstraint;
		
		result.constraints.push(resultsPart);
		if (!canSeeConstraint)
		{
			canSeeEveryConstraint=false;
		}
	}
	result.overallResult=canSeeEveryConstraint;
	return result;
}

function valueIsInList(value, list)
{
	for (var i=0; i < list.length; i++)
	{
		if (list[i]==value)
		{
			return true;
		}
	}
	return false;
}

/**
 * TODO:  Should use the ScriptConstraint logic to get the matchLogic when we can change the java source but have to  
 * put this horrible hack in for now
 */
function isAndLogic(constraint)
{
	if (constraint.getName()=="es_validClosedMarkings")
	{
		return true;
	}
	return false;
}
