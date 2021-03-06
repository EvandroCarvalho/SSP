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
Ext.define('Ssp.view.tools.profile.RecentSSPActivity', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.recentsspactivity',
    mixins: ['Deft.mixin.Injectable', 'Deft.mixin.Controllable'],
    inject: {
        textStore: 'sspTextStore'
    },
    controller: 'Ssp.controller.tool.profile.ProfileRecentStudentActivityViewController',
    width: '100%',
    height: '100%',
    autoScroll: true,
	sortableColumns: true,
    initComponent: function(){
        var me = this;
        Ext.applyIf(me, {
			queryMode:'local',
            xtype: 'gridcolumn',
			title: me.textStore.getValueByCode('ssp.label.recent-activity.title','Recent SSP Activity for this Student'),
            columns: [{
                dataIndex: 'coachNameLastFirst',
                text: me.textStore.getValueByCode('ssp.label.recent-activity.coach','Coach'),
				flex: 1
            }, {
                dataIndex: 'activity',
                text: me.textStore.getValueByCode('ssp.label.recent-activity.activity','Service'),
				flex: 1
            }, {
            
                dataIndex: 'activityDate',
                text: me.textStore.getValueByCode('ssp.label.recent-activity.activity-date','Date'),
                renderer: Ext.util.Format.dateRenderer('m/d/Y'),
				flex: 1
            }]
        });
        
        me.callParent(arguments);
    }
});