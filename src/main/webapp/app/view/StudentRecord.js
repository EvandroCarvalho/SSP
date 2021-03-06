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
Ext.define('Ssp.view.StudentRecord', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.studentrecord',
    mixins: ['Deft.mixin.Injectable', 'Deft.mixin.Controllable'],
    inject: {
        authenticatedPerson: 'authenticatedPerson',
        appEventsController: 'appEventsController',
        textStore: 'sspTextStore'
    },    
    controller: 'Ssp.controller.StudentRecordViewController',
    width: '100%',
    height: '100%',
    initComponent: function(){
        var me = this;
        Ext.apply(this, {
            title: '',
            collapsible: true,
            collapseDirection: 'left',
            listeners: {
                beforecollapse: function() {
                    me.appEventsController.loadMaskOn();
                },
                collapse: function() {
                    me.appEventsController.loadMaskOff();
                },
                beforeexpand: function() {
                    me.appEventsController.loadMaskOn();
                },
                expand: function() {
                    me.appEventsController.loadMaskOff();
                },
                afterrender: {
                    fn: function(c){
                        c.collapseTool.el.dom.firstChild.setAttribute('alt', "Expand / Collapse");
                    }
                }
            },
            cls: 'studentpanel',
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            tools: [
        	{
                tooltip: me.textStore.getValueByCode('ssp.tooltip.watch-student', 'Watch / Un-Watch Student'),
                text: '',
                width: 105,
                height: 20,
        		hidden: true,
                xtype: 'button',
                cls: "makeTransparent x-btn-link",
                itemId: 'watchStudentButton'
            },                    
			
			{
                tooltip: me.textStore.getValueByCode('ssp.tooltip.email-coach', 'Email Coach'),
                text: '',
                width: 170,
                height: 20,
                xtype: 'button',
                itemId: 'emailCoachButton',
				cls: "makeTransparent x-btn-link"
            },

			{
                tooltip: me.textStore.getValueByCode('ssp.tooltip.coaching-history', 'Coaching History'),
                text: me.textStore.getValueByCode('ssp.label.coaching-history', 'Coaching History'),
                width: 105,
                height: 20,
                xtype: 'button',
				cls: "makeTransparent x-btn-link",
				hidden: !me.authenticatedPerson.hasAccess('PRINT_HISTORY_BUTTON'),
                itemId: 'viewCoachingHistoryButton'
            },
			{
                xtype: 'button',
                itemId: 'studentRecordEditButton',
                text: '',
                tooltip: me.textStore.getValueByCode('ssp.tooltip.edit-student', 'Edit Student'),
				cls: "editPerson20Icon",
				width: 23
            },
			{
                xtype: 'tbspacer',
                width: 1
            }],
            items: [{
                xtype: 'toolsmenu',
                flex: 0.60
            }, {
                xtype: 'tools',
                flex: 4.40
            }]
        });
        return this.callParent(arguments);
    }
});
