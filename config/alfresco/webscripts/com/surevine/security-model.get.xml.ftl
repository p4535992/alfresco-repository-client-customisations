<#--
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
<#macro renderGroupList list>
    <#list list as group>
        <value>
            systemName=${group.name}
            humanName=${group.humanReadableName}
            type=${group.displayType!"none"}
            description=${group.description}
            permissionAuthorities=${group.permissionAuthoritySpecification}
            <#if group.deprecated>deprecated=true</#if>
        </value>
    </#list>
</#macro>
<?xml version="1.0" encoding="UTF-8"?>

<model name="es:escCustom" xmlns="http://www.alfresco.org/model/dictionary/1.0">
    <description>Model definitions for Enhanced Security metadata</description>
    <author>alfresco@surevine.com, simon.white@surevine.com</author>
    <version>0.3</version>

    <imports>
        <import uri="http://www.alfresco.org/model/dictionary/1.0" prefix="d"/>
        <import uri="http://www.alfresco.org/model/system/1.0" prefix="sys"/>
    </imports>

    <namespaces>
        <namespace uri="http://www.alfresco.org/model/enhancedSecurity/0.3" prefix="es"/>
    </namespaces>
    
    <!-- 
    The constraints are declared out-of-line to make it easier for support staff to edit at a later date
     -->
    <constraints>
    
        <constraint name="es:validOpenMarkings" type="com.surevine.alfresco.esl.impl.EnhancedSecurityConstraint">
            <title>Open Markings</title>
            <parameter name="groupDetailsSpecification">
                <list>
                    <@renderGroupList openMarkings/>
                </list>
            </parameter>
            <parameter name="caseSensitive"><value>true</value></parameter>
            <parameter name="matchLogic"><value>OR</value></parameter>
            <parameter name="description"><value>Deprecated Open Markings</value></parameter>
            <parameter name="deprecated"><value>true</value></parameter>
        </constraint>
        
        <constraint name="es:validOrganisations" type="com.surevine.alfresco.esl.impl.EnhancedSecurityConstraint">
            <title>Organisations</title>
            <parameter name="groupDetailsSpecification">
                <list>
                    <@renderGroupList organisations/>
                </list>
            </parameter>
            <parameter name="caseSensitive"><value>true</value></parameter>
            <parameter name="matchLogic"><value>OR</value></parameter>
            <parameter name="description"><value>Organisation Markings</value></parameter>
            <parameter name="displayPriority"><value>Low</value></parameter>
        </constraint>
        
        <constraint name="es:validClosedMarkings" type="com.surevine.alfresco.esl.impl.EnhancedSecurityConstraint">
            <title>Closed Markings</title>
            <parameter name="groupDetailsSpecification">
                <list>
                    <@renderGroupList closedMarkings/>
                </list>
            </parameter>
            <parameter name="caseSensitive"><value>true</value></parameter>
            <parameter name="matchLogic"><value>AND</value></parameter>
            <parameter name="description"><value>Closed Markings, comprising groups and restrictions</value></parameter>
            <parameter name="filterDisplay"><value>true</value></parameter>
            <parameter name="displayPriority"><value>High</value></parameter>
        </constraint>
    
        <!-- Although we'll only be using the very bottom of this list, it
             was felt that we should include all the values in the enumeration
             in order to be more compliant with COSP02
        --> 
        <constraint name="es:validPMs" type="LIST">
            <parameter name="allowedValues">
                <list>
                    <value>NATO UNCLASSIFIED</value>
                    <value>NATO RESTRICTED</value>
                    <value>NATO CONFIDENTIAL</value>
                    <value>CONFIDENTIAL</value>
                    <value>NATO SECRET</value>
                    <value>COSMIC TOP SECRET</value>
                </list>
            </parameter>
            <parameter name="caseSensitive"><value>true</value></parameter>
        </constraint>
        
        <constraint name="es:validNODs" type="LIST">
            <parameter name="allowedValues">
                <list>
                    <value>UK</value>
                    <value></value>
                    <value>ME</value>
                </list>
            </parameter>
            <parameter name="caseSensitive"><value>true</value></parameter>
        </constraint>
        
        <constraint name="es:validNationalCaveats" type="LIST">
            <parameter name="allowedValues">
                <list>
                    <value></value>
                    <value>UK EYES ONLY</value>
                    <value>AUS/UK EYES ONLY</value>
                    <value>CAN/UK EYES ONLY</value>
                    <value>FRA/UK EYES ONLY</value>
                    <value>US/UK EYES ONLY</value>
                    <value>AUS/CAN/UK EYES ONLY</value>
                    <value>AUS/FRA/UK EYES ONLY</value>
                    <value>AUS/US/UK EYES ONLY</value>
                    <value>CAN/FRA/UK EYES ONLY</value>
                    <value>CAN/UK/US EYES ONLY</value>
                    <value>FRA/UK/US EYES ONLY</value>
                    <value>AUS/CAN/UK EYES ONLY</value>
                    <value>CAN/FRA/UK/US EYES ONLY</value>
                    <value>AUS/FRA/UK/US EYES ONLY</value>
                    <value>AUS/CAN/UK/US EYES ONLY</value>
                    <value>AUS/CAN/FRA/UK EYES ONLY</value>
                    <value>AUS/CAN/FRA/UK/US EYES ONLY</value>
                </list>
            </parameter>
            <parameter name="caseSensitive"><value>true</value></parameter>
        </constraint>
        
    </constraints>

    <aspects>
        <aspect name="es:enhancedSecurityLabel">
            <title>Security Marking</title>
            
            <properties>
            
                <property name="es:openMarkings">
                    <title>Open Markings</title>
                    <type>d:text</type>
                    <multiple>true</multiple>
                    <constraints>
                        <constraint ref="es:validOpenMarkings"/>
                    </constraints>
                </property>
                
                <property name="es:organisations">
                    <title>Organisations</title>
                    <type>d:text</type>
                    <multiple>true</multiple>
                    <constraints>
                        <constraint ref="es:validOrganisations"/>
                    </constraints>
                </property>
                
                <property name="es:closedMarkings">
                    <title>Closed Markings</title>
                    <type>d:text</type>
                    <multiple>true</multiple>
                    <constraints>
                        <constraint ref="es:validClosedMarkings"/>
                    </constraints>
                </property>
                
                <property name="es:pm">
                    <title>Protective Marking</title>
                    <type>d:text</type>
                    <mandatory enforced='true'>true</mandatory>
                    <default>NOT PROTECTIVELY MARKED</default>
                    <constraints>
                        <constraint ref="es:validPMs"/>
                    </constraints>
                </property>
                
               <!--  
               This is a bit awkward.  I'm trying to say that the nod can be either "UK" or nothing.
               So I've gone with "You don't _have_ to have this parameter, but if you do it's got to 
               be "UK".  I've used a LOV constraint to make it trivial to extend into a multinational
               or officer-exchange type domain in the future
                -->
                <property name="es:nod">
                    <title>National Ownership Designator</title>
                    <type>d:text</type>
                    <default>UK</default>
                    <constraints>
                        <constraint ref="es:validNODs"/>
                    </constraints>
                </property>
                
                <property name="es:nationalityCaveats">
                    <title>Nationality Caveats</title>
                    <type>d:text</type>
                    <mandatory enforced='true'>true</mandatory>
                    <default>UK EYES ONLY</default>
                    <constraints>
                        <constraint ref="es:validNationalCaveats"/>
                    </constraints>
                </property>
                <!-- 
                Was thinking about making this multiple, but then you get into horrible issues about the difference 
                between {"FOO","BAR"} and "FOO BAR" so suggest a single piece of text is simplest ergo best
                -->
                <property name="es:freeFormCaveats">
                    <title>Free Form Caveats</title>
                    <type>d:text</type>
                </property>
            </properties>
        </aspect>
    </aspects>
</model>
