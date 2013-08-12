<div class="markingComparison">
	<div class="overallResult">
		<#-- Write some information out about the overall result of the comparison -->
		<h3>
		<#if machineResults.overallResult>
			${msg("can-view-items", person.properties.firstName?html + " " + person.properties.lastName?html)}
		<#else>
			${msg("cant-view-items", person.properties.firstName?html + " " + person.properties.lastName?html)}
		</#if>
		</h3>
	</div>
	<#list machineResults.constraints as constraint>
		<#-- Then, write some details out about each constraint -->
		<div class="constraintResults">
			<#if constraint.values?size == 0>
				<#-- Shortcut if no marking values were specified for this constraint -->
				${msg("no-constraint", constraintNameMapping[constraint.constraintName]?html)}
			<#else>
				<#if constraint.andLogic>
					<#-- AND logic... -->
					<#if constraint.result>
						<#-- AND Logic, constraint allows access -->
						<div class="detail has">
						${msg("has-all-specified", person.properties.firstName?html, constraintNameMapping[constraint.constraintName]?html)}
						</div>
					<#else>
						<#-- AND Logic, constraint denies access, enumerate which values the user doesn't have -->
						<div class="detail has-not">
						${msg("doesnt-have", person.properties.firstName?html)}
						<#list constraint.values as value>
							${value.value?html}<#if value_has_next><#if value_index == constraint.values?size - 1> ${msg("or")?html}<#else>,</#if></#if>
						</#list>
						</div>
					</#if>
				<#else>
					<#-- OR logic... -->
					<#if constraint.result>
						<#-- OR Logic, constraint allows access, enumerate which values the user has -->
						<div class="detail has">
						${msg("has", person.properties.firstName?html)}
						<#list constraint.values as value>
							${value.value?html}<#if value_has_next><#if value_index == constraint.values?size - 1> ${msg("and")?html}<#else>,</#if></#if> 
						</#list>
						</div>
					<#else>
						<#-- OR Logic, constraint denies access -->
						<div class="detail has-not">
						${msg("has-none-specified", person.properties.firstName?html, constraintNameMapping[constraint.constraintName]?html)}
						</div>
					</#if>			
				</#if>
			</#if>
		</div>
	</#list>
</div>