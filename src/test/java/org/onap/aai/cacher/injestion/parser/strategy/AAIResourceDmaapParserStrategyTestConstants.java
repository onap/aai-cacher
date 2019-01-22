/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.cacher.injestion.parser.strategy;

public class AAIResourceDmaapParserStrategyTestConstants {

	public static final String VSERVER_URI = "/aai/v12/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/AAIAIC25/tenants/tenant/SERVERNAME%3A%3AXXXX/vservers/vserver/afce2113-297a-436c-811a-acf9981fff68";
	public static final String VSERVER_RELATIONSHIP_OBJ = "{" +
			"	'related-to': 'vserver'," +
			"	'relationship-label': 'tosca.relationships.HostedOn'," +
			"	'related-link': '/aai/v12/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/AAIAIC25/tenants/tenant/SERVERNAME%3A%3AXXXX/vservers/vserver/afce2113-297a-436c-811a-acf9981fff68'," +
			"	'relationship-data': [" +
			"		{" +
			"			'relationship-key': 'cloud-region.cloud-owner'," +
			"			'relationship-value': 'onap-cloud-owner'" +
			"		}," +
			"		{" +
			"			'relationship-key': 'cloud-region.cloud-region-id'," +
			"			'relationship-value': 'AAIAIC25'" +
			"		}," +
			"		{" +
			"			'relationship-key': 'tenant.tenant-id'," +
			"			'relationship-value': 'SERVERNAME::XXXX'" +
			"		}," +
			"		{" +
			"			'relationship-key': 'vserver.vserver-id'," +
			"			'relationship-value': 'afce2113-297a-436c-811a-acf9981fff68'" +
			"		}" +
			"	]" +
			"}";

	public static final String FULL_PSERVER_URI = "/aai/v12/cloud-infrastructure/pservers/pserver/SERVERNAME";
	public static final String FULL_PSERVER = "{" +
			"	'hostname': 'SERVERNAME'," +
			"	'relationship-list':" +
			"	{" +
			"		'relationship': [" +
			"			{" +
			"				'related-to': 'generic-vnf'," +
			"				'relationship-label': 'tosca.relationships.HostedOn'," +
			"				'related-link': '/aai/v12/network/generic-vnfs/generic-vnf/205c64eb-88b1-490a-a838-b0080e6902bc'," +
			"				'relationship-data': [" +
			"					{" +
			"						'relationship-key': 'generic-vnf.vnf-id'," +
			"						'relationship-value': '205c64eb-88b1-490a-a838-b0080e6902bc'" +
			"					}" +
			"				]," +
			"				'related-to-property': [" +
			"					{" +
			"						'property-key': 'generic-vnf.vnf-name'," +
			"						'property-value': 'USAUTOUFTIL2001UJDM02'" +
			"					}" +
			"				]" +
			"			}," +
			"			{" +
			"				'related-to': 'vserver'," +
			"				'relationship-label': 'tosca.relationships.HostedOn'," +
			"				'related-link': '/aai/v12/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/AAIAIC25/tenants/tenant/SERVERNAME%3A%3AXXXX/vservers/vserver/74a47c2c-b53f-4264-87fc-bb85c7f49207'," +
			"				'relationship-data': [" +
			"					{" +
			"						'relationship-key': 'cloud-region.cloud-owner'," +
			"						'relationship-value': 'onap-cloud-owner'" +
			"					}," +
			"					{" +
			"						'relationship-key': 'cloud-region.cloud-region-id'," +
			"						'relationship-value': 'AAIAIC25'" +
			"					}," +
			"					{" +
			"						'relationship-key': 'tenant.tenant-id'," +
			"						'relationship-value': 'SERVERNAME::XXXX'" +
			"					}," +
			"					{" +
			"						'relationship-key': 'vserver.vserver-id'," +
			"						'relationship-value': '74a47c2c-b53f-4264-87fc-bb85c7f49207'" +
			"					}" +
			"				]," +
			"				'related-to-property': [" +
			"					{" +
			"						'property-key': 'vserver.vserver-name'," +
			"						'property-value': 'SERVERNAME-USAUTOUFTIL2001UJTE03'" +
			"					}" +
			"				]" +
			"			}," +
			"			{" +
			"				'related-to': 'vserver'," +
			"				'relationship-label': 'tosca.relationships.HostedOn'," +
			"				'related-link': '/aai/v12/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/AAIAIC25/tenants/tenant/SERVERNAME%3A%3AXXXX/vservers/vserver/afce2113-297a-436c-811a-acf9981fff68'," +
			"				'relationship-data': [" +
			"					{" +
			"						'relationship-key': 'cloud-region.cloud-owner'," +
			"						'relationship-value': 'onap-cloud-owner'" +
			"					}," +
			"					{" +
			"						'relationship-key': 'cloud-region.cloud-region-id'," +
			"						'relationship-value': 'AAIAIC25'" +
			"					}," +
			"					{" +
			"						'relationship-key': 'tenant.tenant-id'," +
			"						'relationship-value': 'SERVERNAME::XXXX'" +
			"					}," +
			"					{" +
			"						'relationship-key': 'vserver.vserver-id'," +
			"						'relationship-value': 'afce2113-297a-436c-811a-acf9981fff68'" +
			"					}" +
			"				]," +
			"				'related-to-property': [" +
			"					{" +
			"						'property-key': 'vserver.vserver-name'," +
			"						'property-value': 'SERVERNAME-vjunos0'" +
			"					}" +
			"				]" +
			"			}," +
			"			{" +
			"				'related-to': 'complex'," +
			"				'relationship-label': 'org.onap.relationships.inventory.LocatedIn'," +
			"				'related-link': '/aai/v12/cloud-infrastructure/complexes/complex/STLSMO0914'," +
			"				'relationship-data': [" +
			"					{" +
			"						'relationship-key': 'complex.physical-location-id'," +
			"						'relationship-value': 'STLSMO0914'" +
			"					}" +
			"				]" +
			"			}" +
			"		]" +
			"	}," +
			"	'p-interfaces':" +
			"	{" +
			"		'p-interface': [" +
			"			{" +
			"				'interface-name': 'ge-0/0/10'," +
			"				'relationship-list':" +
			"				{" +
			"					'relationship': [" +
			"						{" +
			"							'related-to': 'physical-link'," +
			"							'relationship-label': 'tosca.relationships.network.LinksTo'," +
			"							'related-link': '/aai/v12/network/physical-links/physical-link/HIS.1702.03053.121'," +
			"							'relationship-data': [" +
			"								{" +
			"									'relationship-key': 'physical-link.link-name'," +
			"									'relationship-value': 'HIS.1702.03053.121'" +
			"								}" +
			"							]" +
			"						}" +
			"					]" +
			"				}" +
			"			}," +
			"			{" +
			"				'interface-name': 'ge-0/0/11'," +
			"				'relationship-list':" +
			"				{" +
			"					'relationship': [" +
			"						{" +
			"							'related-to': 'physical-link'," +
			"							'relationship-label': 'tosca.relationships.network.LinksTo'," +
			"							'related-link': '/aai/v12/network/physical-links/physical-link/HIS.1702.03053.122'," +
			"							'relationship-data': [" +
			"								{" +
			"									'relationship-key': 'physical-link.link-name'," +
			"									'relationship-value': 'HIS.1702.03053.122'" +
			"								}" +
			"							]" +
			"						}" +
			"					]" +
			"				}" +
			"			}" +
			"		]" +
			"	}" +
			"}";


		public final static String GENERIC_VNF_EVENT_WITH_2_RELAT = "{" +
				"	'cambria.partition': 'AAI'," +
				"	'event-header':" +
				"	{" +
				"		'severity': 'NORMAL'," +
				"		'entity-type': 'generic-vnf'," +
				"		'top-entity-type': 'generic-vnf'," +
				"		'entity-link': '/aai/v14/network/generic-vnfs/generic-vnf/cc1703a9-a63f-46c5-a6b1-7ff67f3a9848'," +
				"		'event-type': 'AAI-EVENT'," +
				"		'domain': 'e2e1'," +
				"		'action': 'UPDATE'," +
				"		'sequence-number': '0'," +
				"		'id': '35717064-c145-4172-941a-ae71dced750e'," +
				"		'version': 'v12'," +
				"		'timestamp': '20180523-15:41:19:570'" +
				"	}," +
				"	'entity':" +
				"	{" +
				"		'vnf-id': 'cc1703a9-a63f-46c5-a6b1-7ff67f3a9848'," +
				"		'vf-modules':" +
				"		{" +
				"			'vf-module': [" +
				"				{" +
				"					'vf-module-id': 'eb792c93-d7e6-481c-8a78-e63d39f63e3a'" +
				"				}," +
				"				{" +
				"					'vf-module-id': '43448d88-099f-4a33-8860-889773440675'" +
				"				}" +
				"			]" +
				"		}," +
				"		'relationship-list':" +
				"		{" +
				"			'relationship': [" +
				"				{" +
				"					'related-to': 'service-instance'," +
				"					'relationship-label': 'org.onap.relationships.inventory.ComposedOf'," +
				"					'related-link': '/aai/v14/business/customers/customer/1702_IT3_SubscGblID_20170426162928/service-subscriptions/service-subscription/XXXX-VMS/service-instances/service-instance/SERVERNAME'," +
				"					'relationship-data': [" +
				"						{" +
				"							'relationship-key': 'customer.global-customer-id'," +
				"							'relationship-value': '1702_IT3_SubscGblID_20170426162928'" +
				"						}," +
				"						{" +
				"							'relationship-key': 'service-subscription.service-type'," +
				"							'relationship-value': 'XXXX-VMS'" +
				"						}," +
				"						{" +
				"							'relationship-key': 'service-instance.service-instance-id'," +
				"							'relationship-value': 'SERVERNAME'" +
				"						}" +
				"					]," +
				"					'related-to-property': [" +
				"						{" +
				"							'property-key': 'service-instance.service-instance-name'" +
				"						}" +
				"					]" +
				"				}," +
				"				{" +
				"					'related-to': 'vserver'," +
				"					'relationship-label': 'tosca.relationships.HostedOn'," +
				"					'related-link': '/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/AAIAIC25/tenants/tenant/SERVERNAME%3A%3AXXXX-VMS/vservers/vserver/e77451f2-1c07-4db4-b92b-9907b840fc8f'," +
				"					'relationship-data': [" +
				"						{" +
				"							'relationship-key': 'cloud-region.cloud-owner'," +
				"							'relationship-value': 'onap-cloud-owner'" +
				"						}," +
				"						{" +
				"							'relationship-key': 'cloud-region.cloud-region-id'," +
				"							'relationship-value': 'AAIAIC25'" +
				"						}," +
				"						{" +
				"							'relationship-key': 'tenant.tenant-id'," +
				"							'relationship-value': 'SERVERNAME::XXXX-VMS'" +
				"						}," +
				"						{" +
				"							'relationship-key': 'vserver.vserver-id'," +
				"							'relationship-value': 'e77451f2-1c07-4db4-b92b-9907b840fc8f'" +
				"						}" +
				"					]," +
				"					'related-to-property': [" +
				"						{" +
				"							'property-key': 'vserver.vserver-name'," +
				"							'property-value': 'SERVERNAME-vsrx'" +
				"						}" +
				"					]" +
				"				}" +
				"			]" +
				"		}" +
				"	}," +
				"	'existing-obj':" +
				"	{" +
				"		'vnf-id': 'cc1703a9-a63f-46c5-a6b1-7ff67f3a9848'," +
				"		'vf-modules':" +
				"		{" +
				"			'vf-module': [" +
				"				{" +
				"					'vf-module-id': 'eb792c93-d7e6-481c-8a78-e63d39f63e3a'" +
				"				}," +
				"				{" +
				"					'vf-module-id': '43448d88-099f-4a33-8860-889773440675'," +
				"					'relationship-list':" +
				"					{" +
				"						'relationship': [" +
				"							{" +
				"								'related-to': 'l3-network'," +
				"								'relationship-data': [" +
				"									{" +
				"										'relationship-value': '91eae07d-6f38-4fd8-b929-e7c04614c8c3'," +
				"										'relationship-key': 'l3-network.network-id'" +
				"									}" +
				"								]," +
				"								'related-link': '/aai/v14/network/l3-networks/l3-network/91eae07d-6f38-4fd8-b929-e7c04614c8c3'," +
				"								'relationship-label': 'org.onap.relationships.inventory.Uses'," +
				"								'related-to-property': [" +
				"									{" +
				"										'property-key': 'l3-network.network-name'," +
				"										'property-value': 'ADIODvPE-24388-T-E2E-001_int_AdiodVpeTenantOamNetwork.vpeNodMisOam_net_2'" +
				"									}" +
				"								]" +
				"							}" +
				"						]" +
				"					}" +
				"				}" +
				"			]" +
				"		}," +
				"		'relationship-list':" +
				"		{" +
				"			'relationship': [" +
				"				{" +
				"					'related-to': 'vserver'," +
				"					'relationship-label': 'tosca.relationships.HostedOn'," +
				"					'related-link': '/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/AAIAIC25/tenants/tenant/SERVERNAME%3A%3AXXXX-VMS/vservers/vserver/e77451f2-1c07-4db4-b92b-9907b840fc8f'," +
				"					'relationship-data': [" +
				"						{" +
				"							'relationship-key': 'cloud-region.cloud-owner'," +
				"							'relationship-value': 'onap-cloud-owner'" +
				"						}," +
				"						{" +
				"							'relationship-key': 'cloud-region.cloud-region-id'," +
				"							'relationship-value': 'AAIAIC25'" +
				"						}," +
				"						{" +
				"							'relationship-key': 'tenant.tenant-id'," +
				"							'relationship-value': 'SERVERNAME::XXXX-VMS'" +
				"						}," +
				"						{" +
				"							'relationship-key': 'vserver.vserver-id'," +
				"							'relationship-value': 'e77451f2-1c07-4db4-b92b-9907b840fc8f'" +
				"						}" +
				"					]," +
				"					'related-to-property': [" +
				"						{" +
				"							'property-key': 'vserver.vserver-name'," +
				"							'property-value': 'SERVERNAME-vsrx'" +
				"						}" +
				"					]" +
				"				}" +
				"			]" +
				"		}" +
				"	}" +
				"}";

	public final static String GENERIC_VNF_EVENT = "{" +
			"	'cambria.partition': 'AAI'," +
			"	'event-header':" +
			"	{" +
			"		'severity': 'NORMAL'," +
			"		'entity-type': 'generic-vnf'," +
			"		'top-entity-type': 'generic-vnf'," +
			"		'entity-link': '/aai/v14/network/generic-vnfs/generic-vnf/cc1703a9-a63f-46c5-a6b1-7ff67f3a9848'," +
			"		'event-type': 'AAI-EVENT'," +
			"		'domain': 'e2e1'," +
			"		'action': 'UPDATE'," +
			"		'sequence-number': '0'," +
			"		'id': '35717064-c145-4172-941a-ae71dced750e'," +
			"		'version': 'v12'," +
			"		'timestamp': '20180523-15:41:19:570'" +
			"	}," +
			"	'entity':" +
			"	{" +
			"		'vnf-id': 'cc1703a9-a63f-46c5-a6b1-7ff67f3a9848'" +
			"	}" +
			"}";
}
