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
Ext.define('Ssp.view.tools.map.PlanNotes', {
    extend: 'Ext.window.Window',
    alias: 'widget.plannotes',
    mixins: ['Deft.mixin.Injectable', 'Deft.mixin.Controllable'],
    inject: {
		textStore: 'sspTextStore'
    },
    height: 500,
    width: 500,
    resizable: true,
    modal: true,
    initComponent: function() {
		var me=this;
		Ext.apply(me, 
				{
					layout: {
                align: 'stretch',
                type: 'vbox'
            },
            title: me.textStore.getValueByCode('ssp.label.map.plan-notes.title','Plan Notes'),
            items:[{
                xtype: 'form',
                flex: 1,
                border: 0,
                frame: false,
                layout: {
                    align: 'stretch',
                    type: 'vbox'
                },
                width: '100%',
                height: '100%',
                bodyPadding: 0,
                autoScroll: true,
                itemId: 'notesForm',
                fieldDefaults: {
                        msgTarget: 'side',
                        labelAlign: 'left',
                        labelWidth: 100
                    },
               
				    items: [
				   {
				        fieldLabel: me.textStore.getValueByCode('ssp.label.map.plan-notes.contactNotes','Advisor/Coach Notes'),
				        name: 'contactNotes',
				        allowBlank:true,
				        xtype: 'textareafield',
				        autoscroll: true,
				        flex:1,
				        maxLength: 4000,
				        enforceMaxLength: true
				    },{
				        fieldLabel: me.textStore.getValueByCode('ssp.label.map.plan-notes.studentNotes','Student Notes'),
				        name: 'studentNotes',
				        allowBlank:true,
				        xtype: 'textareafield',
				         flex:1,
				        autoscroll: true,
				        maxLength: 4000,
				        enforceMaxLength: true
				    },{
				        fieldLabel: me.textStore.getValueByCode('ssp.label.map.plan-notes.academicGoals','Academic Goals'),
				        name: 'academicGoals',
				        allowBlank:true,
				        xtype: 'textareafield',
				        flex:1,
				        autoscroll: true
				    }],
				    dockedItems: [{
		                xtype: 'toolbar',
		                dock: 'top',
		                items: [{
		                    xtype: 'button',
		                    name: 'saveButton',
		                    text: me.textStore.getValueByCode('ssp.label.save-button','Save')
		                    
		                }, '-', {
		                    xtype: 'button',
		                    name: 'cancelButton',
		                    text: me.textStore.getValueByCode('ssp.label.cancel-button','Cancel'),
							listeners:{
								click:function(){
									this.close();
								},
								scope: me
							}
		                }]
		            
		              }]
		            }],
                    listeners: {
                          afterrender: function(c){
                              c.el.dom.setAttribute('role', 'dialog');
                        }
                    }
				});
		
		return me.callParent(arguments);
	}
});
