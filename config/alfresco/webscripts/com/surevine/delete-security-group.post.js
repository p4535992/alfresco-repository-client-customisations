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