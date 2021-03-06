/*
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
Ext.define('Ssp.view.tools.profile.ServiceReasons', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.profileservicereasons',
    mixins: ['Deft.mixin.Injectable'],
    inject: {
        store: 'profileServiceReasonsStore',
        textStore: 'sspTextStore'
    },
    width: '100%',
    height: '100%',
    initComponent: function(){
        var me = this;
        Ext.apply(me, {
            title: me.textStore.getValueByCode('ssp.label.main.dashboard.service-reasons', 'Service Reasons'),
            hideHeaders: true,
            autoScroll: true,
            queryMode: 'local',
            store: me.store,
            tools: [{
                xtype: 'button',
                itemId: 'serviceReasonEdit',
                width: 20,
                height: 20,
				cls: 'editPencilIcon',
				text:'',
				tooltip: me.textStore.getValueByCode('ssp.tooltip.edit-pencil-icon','Edit')
            }],
            columns: [{
                header: 'Reason',
                dataIndex: 'name',
                flex: 1
            
            }]
        });
        
        return me.callParent(arguments);
    }
});
