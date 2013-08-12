var securityModel = ldap.getSecurityModel();
if (securityModel) {
	model.openMarkings=securityModel.getOpenMarkings().getGroups();
	model.closedMarkings=securityModel.getClosedMarkings().getGroups();
	model.organisations=securityModel.getOrganisations().getGroups();
} else {
	status.code = 304;
	status.redirect = true;
}