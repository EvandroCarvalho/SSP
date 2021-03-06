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
Ext.define('Ssp.view.tools.map.SavePlan', {
    extend: 'Ext.window.Window',
    alias: 'widget.saveplan',
    mixins: ['Deft.mixin.Injectable', 'Deft.mixin.Controllable'],
    controller: 'Ssp.controller.tool.map.SavePlanViewController',
    inject: {
        columnRendererUtils: 'columnRendererUtils',
		appEventsController: 'appEventsController',
        programsStore: 'programsStore',
        catalogYearsStore: 'catalogYearsStore',
		currentMapPlan: 'currentMapPlan',
		textStore: 'sspTextStore',
		transferGoalsStore: 'transferGoalsActiveUnpagedStore'
    },
    height: 554,
    width: 684,
    saveAs: null,
    resizable: true,
    modal: true,
    initComponent: function(){
        var me = this;
        Ext.apply(me, {
            layout: {
                align: 'stretch',
                type: 'vbox'
            },
            title: me.textStore.getValueByCode('ssp.label.map.save-plan.title','Save Plan'),
			
            items: [{
                xtype: 'form',
                flex: 1,
                border: 0,
                frame: false,
                layout: {
                    align: 'stretch',
                    type: 'vbox'
                },
                 width: '80%',
                height: '100%',
                bodyPadding: 5,
                autoScroll: true,
                itemId: 'faSavePlan',
                items: [
                        {
                            xtype: 'container',
                            defaultType: 'checkbox',
                            border: 0,
                            title: '',
                            layout: 'hbox',
                            align: 'stretch',
                            padding: '0 0 15 0',
        					margin: '0 0 0 5',
                            width: '100%',
                            height: 100,
                            items: [{
                				    
                			    	fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.object-status','Active Plan'),
                			    	name: 'objectStatus',
									itemId:'objectStatus',
                			    	labelWidth: 65,
                			    	checked: false,
                			    	labelAlign: 'before',
                			    	inputValue: 'objectStatus'
                			    },
                			    {
                                    xtype: 'tbspacer',
                                    width: 20
                                },
                			    {
                			    	
                			    	boxLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.is-important','Important / Caution'),
                			    	name: 'isImportant',
                			    	labelWidth: 130,
                			    	boxLabelAlign: 'before',
                			    	inputValue: 'isImportant',
									itemId:'isImportant'
                			    
                			    },
                			    {
                                    xtype: 'tbspacer',
                                    width: 20
                                },
                			    {
                			    	
                			    	boxLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.is-financial-aid','Required For Financial Aid(SAP)'),
                			    	name: 'isFinancialAid',
                			    	labelWidth: 200,
                			    	boxLabelAlign: 'before',
                			    	inputValue: 'isFinancialAid',
									itemId:'isFinancialAid'
                			    },
                			    {
                                    xtype: 'tbspacer',
                                    width: 20
                                },
                			    {
                			    	
                			    	boxLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.is-f1-visa','F1 visa'),
                			    	name: 'isF1Visa',
                			    	labelWidth: 70,
                			    	boxLabelAlign: 'before',
                			    	inputValue: 'isF1Visa',
									itemId:'isF1Visa'
                                },
                			    {
                                    xtype: 'tbspacer',
                                    width: 20
                                },
                			    {
                			    	boxLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.is-partial','Partial'),
                			    	name: 'isPartial',
                			    	labelWidth: 70,
                			    	boxLabelAlign: 'before',
                			    	inputValue: 'isPartial',
									itemId:'isPartial'
                			    }
                			    ]},
        			    	{
                            xtype: 'container',
                            defaultType: 'textfield',
        			    	border: 1,
                            margin: '0 0 0 2',
                            padding: '0 0 0 5',
							width: '80%',
                            //flex: 1,
                            layout: {
                                align: 'stretch',
                                type: 'vbox'
                            },
                            items: [
				               {
            				        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.plan-title','Plan Title'),
            				        name: 'name',
            				        itemId: 'name',
            				        maxLength: 50,
									width: '80%',
            				        allowBlank:false
            				    },{
        		                    xtype: 'combobox',
        	                        name: 'programCode',
            				        itemId: 'programCode',
									store: me.programsStore,
        	                        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.program-code','Program'),
        	                        emptyText: me.textStore.getValueByCode('ssp.empty-text.map.save-plan.program-code','Specific Program'),
        	                        valueField: 'code',
        	                        displayField: 'name',
        	                        mode: 'local',
        	                        typeAhead: true,
        	                        queryMode: 'local',
									width: '80%',
        	                        allowBlank: true
        	                    },{        	                    
        	                        xtype: 'combobox',
        	                        name: 'catalogYearCode',
									store: me.catalogYearsStore,
        	                        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.catalog-year-code','Catalog Year'),
        	                        emptyText: me.textStore.getValueByCode('ssp.empty-text.map.save-plan.catalog-year-code','Specific Cat Year'),
        	                        valueField: 'code',
        	                        displayField: 'name',
        	                        mode: 'local',
        	                        typeAhead: true,
									width: '80%',
        	                        allowBlank: true
        	                    },{
        	                        xtype: 'combobox',
        	                        name: 'transferGoalId',
									store: me.transferGoalsStore,
        	                        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.transfer-goal','Transfer Goal'),
        	                        emptyText: me.textStore.getValueByCode('ssp.empty-text.map.save-plan.transfer-goal','None Specified'),
        	                        valueField: 'id',
        	                        displayField: 'name',
        	                        mode: 'local',
									queryMode: 'local',
        	                        typeAhead: false,
                                    editable: false,
									width: '80%',
        	                        allowBlank: true
        	                    } ,{
            				        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.contact-name','Contact Name'),
            				        name: 'contactName',
            				        itemId: 'contactName',
            				        maxLength: 50,
									width: '80%',
            				        allowBlank:true
            				        
            				    },{
            				        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.contact-title','Contact Title'),
            				        name: 'contactTitle',
            				        itemId: 'contactTitle',
            				        maxLength: 50,
									width: '80%',
            				        allowBlank:true
            				    },{
	            				    fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.contact-email','Contact Email'),
	            				    name: 'contactEmail',
	            				    itemId: 'contactEmail',
									width: '80%',
	            				   	allowBlank:true,
									maxLength: 200
	            				 },{
            				        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.contact-phone','Contact Phone'),
            				        name: 'contactPhone',
            				        itemId: 'contactPhone',
									width: '80%',
            				        allowBlank:true,
									maxLength: 200
            				    },
            				   {
            				        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.academic-link','Academic'),
            				        name: 'academicLink',
            				        allowBlank:true,
									width: '80%',
            				        itemId: 'academicLink',
									maxLength: 2000
            				    },{
            				        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.career-link','Career Data'),
            				        name: 'careerLink',
            				        allowBlank:true,
									width: '80%',
            				        itemId: 'careerLink',
									maxLength: 2000
            				    },{
            				        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.contact-notes','Advisor/Coach Notes'),
            				        name: 'contactNotes',
            				        allowBlank:true,
            				        itemId: 'contactNotes',
									width: '80%',
            				        xtype: 'textareafield',
									maxLength: 4000
            				    },{
            				        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.student-notes','Student Notes'),
            				    	name: 'studentNotes',
            			        	allowBlank:true,
            			        	itemId: 'studentNotes',
            			        	xtype: 'textareafield',
									width: '80%',
									maxLength: 4000
                			    },{
                			        fieldLabel: me.textStore.getValueByCode('ssp.label.map.save-plan.academic-goals','Academic Goals'),
                			        name: 'academicGoals',
                			        allowBlank: true,
                			        itemId: 'academicGoals',
                			        xtype: 'textareafield',
									width: '80%',
									maxLength: 2000
                			    }
            			    ]
                    
                    }
                    ],
                        dockedItems: [{
                        xtype: 'toolbar',
                        dock: 'top',
                        items: [{
                            xtype: 'button',
                            itemId: 'saveButton',
                            text: me.textStore.getValueByCode('ssp.label.save-button','Save')

                            
                        }, '-', {
                            xtype: 'button',
                            itemId: 'cancelButton',
                            text: me.textStore.getValueByCode('ssp.label.cancel-button','Cancel')
                        }]
                    
                    }]
            }
            
            ],
            listeners: {
                 afterrender: function(c){
                     c.el.dom.setAttribute('role', 'dialog');
                 }
            }
            
        });
        return me.callParent(arguments);
    }
    
});