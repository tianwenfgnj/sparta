(function () {
  'use strict';

  angular
    .module('webApp')
    .service('PolicyStaticDataFactory', PolicyStaticDataFactory);

  function PolicyStaticDataFactory() {

    return {
      steps: [
        {name: "_POLICY_._STEPS_._DESCRIPTION_", icon: "icon-tag_left"},
        {name: "_POLICY_._STEPS_._INPUT_", icon: "icon-import"},
        {name: "_POLICY_._STEPS_._MODEL_", icon: "icon-content-left"},
        {name: "_POLICY_._STEPS_._CUBES_", icon: "icon-box"},
        {name: "_POLICY_._STEPS_._OUTPUTS_", icon: "icon-export"},
        {name: "_POLICY_._STEPS_._FINISH_", icon: "icon-paper"}
      ],
      sparkStreamingWindow: {min: 0, max: 10000},
      checkpointInterval: {
        min: 0, max: 10000
      }
      ,
      checkpointAvailability: {
        min: 0, max: 10000
      },
      partitionFormat: {
        "values": [
          {
            "label": "year",
            "value": "year"
          },
          {
            "label": "month",
            "value": "month"
          },
          {
            "label": "day",
            "value": "day"
          },
          {
            "label": "hour",
            "value": "hour"
          },
          {
            "label": "minute",
            "value": "minute"
          }
        ],
      },
      helpLinks: {
        description: 'http://docs.stratio.com/modules/sparkta/development/policy.html#general-configuration',
        inputs: 'http://docs.stratio.com/modules/sparkta/development/policy.html#inputs',
        models: 'http://docs.stratio.com/modules/sparkta/development/policy.html#transformations',
        cubes: 'http://docs.stratio.com/modules/sparkta/development/policy.html#cubes',
        outputs: 'http://docs.stratio.com/modules/sparkta/development/policy.html#outputs'
      }
    }
  }
})
();
