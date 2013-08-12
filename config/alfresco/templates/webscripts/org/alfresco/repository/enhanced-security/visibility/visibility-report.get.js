<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/repository/enhanced-security/visibility/lib/visibility.lib.js">

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