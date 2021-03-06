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
Ext.define('Ssp.controller.tool.map.MapPlanToolViewController', {
    extend: 'Deft.mvc.ViewController',
    mixins: [ 'Deft.mixin.Injectable' ],
    control: {
    	'onPlanField': '#onPlan',
		'onPlanStatusDetails': '#onPlanStatusDetails'
    },
    inject:{
		appEventsController: 'appEventsController',
		formUtils: 'formRendererUtils',
    	currentMapPlan: 'currentMapPlan',
    	mapPlanService:'mapPlanService'
    },
	init: function() {
		var me=this;
        // Cannot do anything in init that depends on the current plan
        // having been loaded because this component potentially (and at this
        // writing does) get initialized before the component that actually
        // loads the current plan (not to mention that that load is async). So
        // we just make sure to clear out any existing view state (paranoia?),
        // set up listeners and wait for a subsequent event to indicate the
        // current plan is loaded. We don't set a spinner here b/c we don't
        // want to accidentally lock the panel forever should we never get
        // the event we expect
        me.resetForm();
        me.appEventsController.getApplication().addListener("onAfterPlanLoad", me.onUpdateCurrentMapPlan, me);
        me.appEventsController.getApplication().addListener("onUpdatePlanStatus", me.updatePlanStatus, me);

        return me.callParent(arguments);
    },
    resetForm: function() {
        var me = this;
       me.getView().getForm().reset();
    },

    updatePlanStatus: function(){
		var me=this;
        // Important to cache this before the async plan status lookup rather
        // than in a success or failure handler because we know the event
        // that triggers this function execution can be fired multiple times
        // before those async calls return.
        me.currentMapPlanId = me.currentMapPlan.getId();

    	if(me.currentMapPlan.get('isTemplate') == true || me.currentMapPlan.get('personId') == ""){
    		me.afterUpdatePlanStatus();
    		return;
    	}
        me.mapPlanService.planStatus(me.currentMapPlan, {
            success: function(response) {
                me.onPlanStatusSuccess(response);
                me.afterUpdatePlanStatus();
            },
            failure: function(response) {
                me.onPlanStatusFailure(response);
                me.afterUpdatePlanStatus();
            },
            scope: me
        });
    },

    afterUpdatePlanStatus: function() {
        var me = this;
        me.onCurrentMapPlanChange();
    },
    
	onUpdateCurrentMapPlan: function(){
		var me = this;
		me.getView().loadRecord(me.currentMapPlan);
        if ( me.guardPlanStatusLookup() ) {
            me.updatePlanStatus();
        }
        else
        {
            me.onCurrentMapPlanChange();
        }
	},

    guardPlanStatusLookup: function() {
        var me = this;
        // Can't tell the difference between two different "new" plans without
        // listening for "onCreateNewMapPlan". But can't hook into that b/c
        // SemesterPanelContainerViewController translates that event into
        // "onAfterPlanLoad", which this component listens
        // for. So we might not receive "onCreateNewMapPlan" until after
        // "onAfterPlanLoad". So this guard ends up being
        // a bit imprecise. But we assume it's probably not necessary to reload
        // the plan status if all you're doing is replacing an unsaved plan with
        // another unsaved plan.
        return me.currentMapPlanId === undefined || // first time load
            me.currentMapPlanId !== me.currentMapPlan.getId(); // obviously changing plans
    },
	
	onPlanStatusSuccess:function(response){
		var me = this;
		if(response.responseText && response.responseText.length > 1)
		    var planStatus = Ext.decode(response.responseText);
		else
		    var planStatus = null;
		if(planStatus && planStatus.status == "ON"){
			me.getOnPlanField().setValue("On Plan");
			me.getOnPlanStatusDetails().setTooltip("Student is currently on plan.");
		}else if(planStatus && planStatus.status == "ON_TRACK_SUBSTITUTION"){
			me.getOnPlanField().setValue("On Track Substitution");
			me.getOnPlanStatusDetails().setTooltip("On Track Substitution");
		}else if(planStatus && planStatus.status == "ON_TRACK_SEQUENCE"){
			me.getOnPlanField().setValue("On Track Sequence");
			me.getOnPlanStatusDetails().setTooltip('On Track Sequence');			
		}else if(planStatus && planStatus.status == "OFF"){
			me.getOnPlanField().setValue("Off Plan");
			me.getOnPlanStatusDetails().setTooltip(planStatus.statusReason);
		}
		else{
			me.getOnPlanField().setValue("No Status");
			me.getOnPlanStatusDetails().setTooltip("Currently, there is no status given for this student.");
		}
		me.currentMapPlan.planStatus = me.getOnPlanField().getValue();
		if(planStatus && planStatus.statusReason)
		{
			me.currentMapPlan.planStatusDetails = planStatus.statusReason;
		}
		else
		{
			me.currentMapPlan.planStatusDetails = '';
		}

	},
	
	onPlanStatusFailure:function(){
		var me = this;
		me.getOnPlanField().setValue("No Plan");

	},
	
	onCurrentMapPlanChange: function(){
		var me = this;
		 if(me.currentMapPlan.get('isTemplate') == true)
		{
			me.getOnPlanField().hide();
			me.getOnPlanStatusDetails().hide();
		}else{
			me.getOnPlanField().show();
			me.getOnPlanStatusDetails().show();
		}
		me.appEventsController.getApplication().fireEvent("onRemoveMask");

	},

	
	destroy: function(){
		var me = this;
		me.appEventsController.getApplication().removeListener("onAfterPlanLoad", me.onUpdateCurrentMapPlan, me);
        me.appEventsController.getApplication().removeListener("onUpdatePlanStatus", me.updatePlanStatus, me);
	    return me.callParent( arguments );
	}
});