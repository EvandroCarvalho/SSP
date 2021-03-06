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
Ext.define('Ssp.util.tools.map.MapEventUtils',{
	extend: 'Ext.Component',
	mixins: ['Deft.mixin.Injectable'],
    inject: {
    	appEventsController: 'appEventsController',
		formUtils: 'formRendererUtils',
		person: 'currentPerson',
        personLite: 'personLite',
    	apiProperties: 'apiProperties',
    	planStore: 'planStore',
    	termsStore:'termsStore',
    	currentMapPlan: 'currentMapPlan',
        mapPlanService:'mapPlanService',
		authenticatedPerson: 'authenticatedPerson',
		semesterStores : 'currentSemesterStores',
        electiveStore: 'electivesAllUnpagedStore',
        colorStore: 'colorsAllUnpagedStore',
        textStore: 'sspTextStore'
    },
	initComponent: function() {
		return this.callParent( arguments );
    },
	loadTemplate: function(templateId) {
		var me = this; 
		
		var callbacks = new Object();
		me.appEventsController.getApplication().fireEvent("onBeforePlanLoad");
		callbacks.success = me.onLoadCompleteSuccess;
		callbacks.failure = me.onLoadCompleteFailure;
		callbacks.scope = me;
		callbacks.isTemplate = true;
	   	me.mapPlanService.getTemplate(templateId, callbacks);

	},
	loadPlan: function(planId) {
		var me = this; 
		
		var callbacks = new Object();
		me.appEventsController.getApplication().fireEvent("onBeforePlanLoad");
		callbacks.success = me.onLoadCompleteSuccess;
		callbacks.failure = me.onLoadCompleteFailure;
		callbacks.scope = me;
		callbacks.isTemplate = false;
   	 	me.mapPlanService.getPlan(planId, callbacks);

	},	
	onLoadCompleteSuccess: function(serviceResponses){
        var me = this;
		if(!serviceResponses || !serviceResponses.responseText || serviceResponses.responseText.trim().length == 0) {

       	} else {
			// Not completely sure why, why loadFromServer() doesn't clear out
			// existing 'currentMapPlan' state first, e.g. it preserves template
			// state. Might be other call sites that need that behavior. But
			// we know here that we always want a complete refresh, so wipe out
			// current state first.
			me.scope.currentMapPlan.clearMapPlan();
       		me.scope.currentMapPlan.loadFromServer(Ext.decode(serviceResponses.responseText));
       		if(me.isTemplate)
       		{
       			me.scope.currentMapPlan.setIsTemplate(true);
       		}
       		else
       		{
       			me.scope.currentMapPlan.setIsTemplate(false);
       		}
			me.scope.appEventsController.getApplication().fireEvent('onPlanLoad');
		}
	},
	onLoadCompleteFailure: function(serviceResponses){
		var me = this;
    	me.scope.getView().setLoading(false);
	},	
	loadCurrentMap: function () {
		var me = this;
		var id = me.personLite.get('id');
		me.appEventsController.getApplication().fireEvent("onBeforePlanLoad");
		me.withMapPlanRenderingDependencies({
			fn: function() {
				if (id != "") {
					var serviceResponses = {
							failures: {},
							successes: {},
							responseCnt: 0,
							expectedResponseCnt: 1
					}
					me.mapPlanService.getCurrent(id, {
						success: me.newServiceSuccessHandler('map', me.getMapPlanServiceSuccess, serviceResponses),
						failure: me.newServiceFailureHandler('map', me.getMapPlanServiceFailure, serviceResponses),
						scope: me
					});
				}		
			},
			scope: me // shouldnt be necessary in this particular case, but satisfies ResponseDispatcher expectations
		});
	},
	createNewMapPlan: function () {
		var me = this;
		me.currentMapPlan.dirty = false;
		me.appEventsController.getApplication().fireEvent("onPlanLoad");
		Ext.Msg.alert(
			me.textStore.getValueByCode('ssp.message.map.plan-ready-to-edit', 'The plan is ready for edit')
		);
	},
	createNewTemplate: function () {
		var me = this;
		me.currentMapPlan.dirty = false;
		me.appEventsController.getApplication().fireEvent("onPlanLoad");
		Ext.Msg.alert(
			me.textStore.getValueByCode('ssp.message.map.template-ready-to-edit', 'New template is ready for edit')
		);
	},
	saveTemplate: function(saveAs) {
	    var me = this;
		me.appEventsController.getApplication().fireEvent("onBeforePlanSave");
	    var callbacks = new Object();
	    var serviceResponses = {
	            failures: {},
	            successes: {},
	            responseCnt: 0,
	            expectedResponseCnt: 1
	        }
	    callbacks.success = me.newServiceSuccessHandler('map', me.onSaveTemplateCompleteSuccess, serviceResponses);
	    callbacks.failure = me.newServiceFailureHandler('map', me.onSaveTemplateCompleteFailure, serviceResponses);
	    callbacks.scope = me;
	    me.mapPlanService.saveTemplate(me.semesterStores, callbacks, me.currentMapPlan, saveAs);
	},
	onSaveTemplateCompleteSuccess: function(serviceResponses){
	    var me = this;
	    me.getMapPlanServiceSuccess(serviceResponses, true,true);
		me.appEventsController.getApplication().fireEvent("doAfterSaveSuccess");
	},
	onSaveTemplateCompleteFailure: function(serviceResponses){
		var me = this;
		me.appEventsController.getApplication().fireEvent("onAfterPlanSave");
		me.appEventsController.getApplication().fireEvent("doAfterSaveFailure");
	},
	save: function(saveAs) {
		var me = this;
		var callbacks = new Object();
		var serviceResponses = {
            failures: {},
            successes: {},
            responseCnt: 0,
            expectedResponseCnt: 1
        }
    	callbacks.success = me.newServiceSuccessHandler('map', me.onSaveCompleteSuccess, serviceResponses);
    	callbacks.failure = me.newServiceFailureHandler('map', me.onSaveCompleteFailure, serviceResponses);
    	callbacks.scope = me;
		me.appEventsController.getApplication().fireEvent("onBeforePlanSave");
    	me.mapPlanService.save(me.semesterStores, callbacks, me.currentMapPlan, saveAs);
    },
	onSaveCompleteSuccess: function(serviceResponses){
		var me = this;
		me.getMapPlanServiceSuccess(serviceResponses);
		me.currentMapPlan.setIsTemplate(false);
		me.appEventsController.getApplication().fireEvent("onAfterPlanLoad");
		me.appEventsController.getApplication().fireEvent("doAfterSaveSuccess");
	},
	onSaveCompleteFailure: function(serviceResponses){
		var me = this;
		me.appEventsController.getApplication().fireEvent("doAfterSaveFailure");
	},
  
    getMapPlanServiceSuccess: function(serviceResponses, isTemplate, fromSave) {
        var me = this;
        var mapResponse = serviceResponses.successes.map;
		if(!mapResponse || !mapResponse.responseText || Ext.String.trim(mapResponse.responseText).length == 0) {
			if(me.termsStore.isLoading()) {
				 me.termsStore.on('load', this.getMapPlanServiceFailure, this, {single: true});
				return;
			}
			personId = me.personLite.get('id');
			var successFunc = function(response){
				
		    	var r, records;
		    	var data=[];
		    	r = Ext.decode(response.responseText);
		    	
		    	if (r != null)
		    	{
		    		if(r.results == 0)
		    		{
		    			me.createNewMapPlan();
		    			return;
		    		}
		    		Ext.Object.each(r,function(key,value){
			    		var plans = value;
			    		Ext.Array.each(plans,function(plan,index){
			    			if(plan.name){
								data.push(plan);
							}
			    		},this);
			    	},this);		    		

		    		me.planStore.loadData(data);
		    		me.planStore.sort();
		    		me.allPlansPopUp = Ext.create('Ssp.view.tools.map.LoadPlans',{hidden:true,onInit:true,store:me.planStore,fromMapLoad:true});
		    		me.allPlansPopUp.show();
		    	}
			};
			
			me.personMapPlanUrl = me.apiProperties.getItemUrl('personMapPlan');
			me.personMapPlanUrl = me.personMapPlanUrl.replace('{id}',personId);

			me.apiProperties.makeRequest({
				url: me.apiProperties.createUrl(me.personMapPlanUrl+'/summary'),
				method: 'GET',
				successFunc: successFunc 
			});
       	} else {
       		if(fromSave)
       		{
       			me.appEventsController.getApplication().fireEvent("onBeforePlanLoad");
       		}
       		me.currentMapPlan.clearMapPlan();
       		me.currentMapPlan.loadFromServer(Ext.decode(mapResponse.responseText));
       		if(isTemplate)
       		{
       			me.currentMapPlan.setIsTemplate(true);
       		}
			me.appEventsController.getApplication().fireEvent("onPlanLoad");
		}
    },	
    withMapPlanRenderingDependencies: function(work) {
        var me = this;
        if(me.electiveStore.getCount() < 1 || me.colorStore.getCount() < 1)
        {
	        var responseDispatcher = Ext.create('Ssp.util.ResponseDispatcher', {
	            remainingOpNames: ['electives','colors'],
	            afterLastOp: {
	                callback: work.fn,
	                callbackScope: work.scope
	            }
	        });
	        me.electiveStore.clearFilter(true);
	        me.electiveStore.load(responseDispatcher.setSuccessCallback('electives', me.noOp, me));
	        me.colorStore.clearFilter(true);
	        me.colorStore.load(responseDispatcher.setSuccessCallback('colors', me.noOp, me));
        }
        else
        {
        	work.fn();
        }
    },
    newServiceSuccessHandler: function(name, callback, serviceResponses) {
        var me = this;
        return me.newServiceHandler(name, callback, serviceResponses, function(name, serviceResponses, response) {
            serviceResponses.successes[name] = response;
        });
    },

    newServiceFailureHandler: function(name, callback, serviceResponses) {
        var me = this;
        return me.newServiceHandler(name, callback, serviceResponses, function(name, serviceResponses, response) {
            serviceResponses.failures[name] = response;
        });
    },

    newServiceHandler: function(name, callback, serviceResponses, serviceResponsesCallback) {
        return function(r, scope) {
            var me = scope;
            serviceResponses.responseCnt++;
            if ( serviceResponsesCallback ) {
                serviceResponsesCallback.apply(me, [name, serviceResponses, r]);
            }
            if ( callback ) {
                callback.apply(me, [ serviceResponses ]);
            }
        };
    }
});
