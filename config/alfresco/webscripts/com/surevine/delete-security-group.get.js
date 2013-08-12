var securityModel = ldap.getSecurityModel(false);

if (securityModel) {
	model.openMarkings=securityModel.getOpenMarkings().getGroups();
	model.closedMarkings=securityModel.getClosedMarkings().getGroups();
	model.organisations=securityModel.getOrganisations().getGroups();
}