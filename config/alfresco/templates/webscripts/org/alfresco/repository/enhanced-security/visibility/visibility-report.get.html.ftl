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
