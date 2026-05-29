{
"lenses": {
  "0": {
    "order": 0,
    "parts": {
      "0": {
        "position": {
          "x": 0,
          "y": 0,
          "colSpan": 9,
          "rowSpan": 4
        },
        "metadata": {
          "inputs": [
            {
              "name": "resourceTypeMode",
              "isOptional": true
            },
            {
              "name": "ComponentId",
              "isOptional": true
            },
            {
              "name": "Scope",
              "value": {
                "resourceIds": [
                  "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                ]
              },
              "isOptional": true
            },
            {
              "name": "PartId",
              "value": "132be8c1-1242-42a1-a954-836f8a91ad0d",
              "isOptional": true
            },
            {
              "name": "Version",
              "value": "2.0",
              "isOptional": true
            },
            {
              "name": "TimeRange",
              "value": "P7D",
              "isOptional": true
            },
            {
              "name": "DashboardId",
              "isOptional": true
            },
            {
              "name": "DraftRequestParameters",
              "isOptional": true
            },
            {
              "name": "Query",
              "value": "customEvents\n| where name contains \"ONBOARDING-CDC\"\n| where customMeasurements contains \"OnboardingsUpdate_successes\"\n| where customDimensions contains \"_id\"\n| extend d=parse_json(customDimensions)\n| extend documentKey = d[\"documentKey\"]\n| project-rename onboardingId = documentKey\n| project timestamp, onboardingId\n| summarize Count = count() by bin(timestamp, 6h)\n",
              "isOptional": true
            },
            {
              "name": "ControlType",
              "value": "FrameControlChart",
              "isOptional": true
            },
            {
              "name": "SpecificChart",
              "value": "Line",
              "isOptional": true
            },
            {
              "name": "PartTitle",
              "value": "Analytics",
              "isOptional": true
            },
            {
              "name": "PartSubTitle",
              "value": "${prefix}-appinsights",
              "isOptional": true
            },
            {
              "name": "Dimensions",
              "value": {
                "xAxis": {
                  "name": "timestamp",
                  "type": "datetime"
                },
                "yAxis": [
                  {
                    "name": "Count",
                    "type": "long"
                  }
                ],
                "splitBy": [],
                "aggregation": "Sum"
              },
              "isOptional": true
            },
            {
              "name": "LegendOptions",
              "value": {
                "isEnabled": true,
                "position": "Bottom"
              },
              "isOptional": true
            },
            {
              "name": "IsQueryContainTimeRange",
              "value": false,
              "isOptional": true
            }
          ],
          "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
          "settings": {
            "content": {
              "PartTitle": "Count Notification invocations with success"
            }
          }
        }
      },
      "1": {
        "position": {
          "x": 9,
          "y": 0,
          "colSpan": 6,
          "rowSpan": 4
        },
        "metadata": {
          "inputs": [
            {
              "name": "resourceTypeMode",
              "isOptional": true
            },
            {
              "name": "ComponentId",
              "isOptional": true
            },
            {
              "name": "Scope",
              "value": {
                "resourceIds": [
                  "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                ]
              },
              "isOptional": true
            },
            {
              "name": "PartId",
              "value": "142201b2-14f6-41ea-a3d7-2863c2e13f52",
              "isOptional": true
            },
            {
              "name": "Version",
              "value": "2.0",
              "isOptional": true
            },
            {
              "name": "TimeRange",
              "value": "P7D",
              "isOptional": true
            },
            {
              "name": "DashboardId",
              "isOptional": true
            },
            {
              "name": "DraftRequestParameters",
              "isOptional": true
            },
            {
              "name": "Query",
              "value": "customEvents\n| where name contains \"ONBOARDING-CDC\"\n| where customMeasurements contains \"OnboardingsUpdate_successes\"\n| where customDimensions contains \"_id\"\n| extend d=parse_json(customDimensions)\n| extend documentKey = d[\"documentKey\"]\n| project-rename onboardingId = documentKey\n| project onboardingId\n",
              "isOptional": true
            },
            {
              "name": "ControlType",
              "value": "AnalyticsGrid",
              "isOptional": true
            },
            {
              "name": "SpecificChart",
              "isOptional": true
            },
            {
              "name": "PartTitle",
              "value": "Analytics",
              "isOptional": true
            },
            {
              "name": "PartSubTitle",
              "value": "${prefix}-appinsights",
              "isOptional": true
            },
            {
              "name": "Dimensions",
              "isOptional": true
            },
            {
              "name": "LegendOptions",
              "isOptional": true
            },
            {
              "name": "IsQueryContainTimeRange",
              "value": false,
              "isOptional": true
            }
          ],
          "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
          "settings": {
            "content": {
              "GridColumnsWidth": {
                "onboardingId": "473.993px"
              },
              "PartTitle": "Onboarding id of successfull Notification invocations"
            }
          }
        }
      },
      "2": {
        "position": {
          "x": 0,
          "y": 4,
          "colSpan": 9,
          "rowSpan": 4
        },
        "metadata": {
          "inputs": [
            {
              "name": "resourceTypeMode",
              "isOptional": true
            },
            {
              "name": "ComponentId",
              "isOptional": true
            },
            {
              "name": "Scope",
              "value": {
                "resourceIds": [
                  "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                ]
              },
              "isOptional": true
            },
            {
              "name": "PartId",
              "value": "3cd8d57f-822a-4186-b0e4-490fe4b17a7d",
              "isOptional": true
            },
            {
              "name": "Version",
              "value": "2.0",
              "isOptional": true
            },
            {
              "name": "TimeRange",
              "value": "P7D",
              "isOptional": true
            },
            {
              "name": "DashboardId",
              "isOptional": true
            },
            {
              "name": "DraftRequestParameters",
              "isOptional": true
            },
            {
              "name": "Query",
              "value": "customEvents\n| where name contains \"ONBOARDING-CDC\"\n| where customMeasurements contains \"OnboardingsUpdate_failures\"\n| where customDimensions contains \"_id\"\n| extend d=parse_json(customDimensions)\n| extend documentKey = d[\"documentKey\"]\n| where documentKey contains \"_id\"\n| project-rename onboardingId = documentKey\n| project timestamp, onboardingId\n| summarize Count = count() by bin(timestamp, 6h)\n",
              "isOptional": true
            },
            {
              "name": "ControlType",
              "value": "FrameControlChart",
              "isOptional": true
            },
            {
              "name": "SpecificChart",
              "value": "Line",
              "isOptional": true
            },
            {
              "name": "PartTitle",
              "value": "Analytics",
              "isOptional": true
            },
            {
              "name": "PartSubTitle",
              "value": "${prefix}-appinsights",
              "isOptional": true
            },
            {
              "name": "Dimensions",
              "value": {
                "xAxis": {
                  "name": "timestamp",
                  "type": "datetime"
                },
                "yAxis": [
                  {
                    "name": "Count",
                    "type": "long"
                  }
                ],
                "splitBy": [],
                "aggregation": "Sum"
              },
              "isOptional": true
            },
            {
              "name": "LegendOptions",
              "value": {
                "isEnabled": true,
                "position": "Bottom"
              },
              "isOptional": true
            },
            {
              "name": "IsQueryContainTimeRange",
              "value": false,
              "isOptional": true
            }
          ],
          "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
          "settings": {
            "content": {
              "PartTitle": "Count Notification invocations with failure"
            }
          }
        }
      },
      "3": {
        "position": {
          "x": 9,
          "y": 4,
          "colSpan": 6,
          "rowSpan": 4
        },
        "metadata": {
          "inputs": [
            {
              "name": "resourceTypeMode",
              "isOptional": true
            },
            {
              "name": "ComponentId",
              "isOptional": true
            },
            {
              "name": "Scope",
              "value": {
                "resourceIds": [
                  "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                ]
              },
              "isOptional": true
            },
            {
              "name": "PartId",
              "value": "0720210f-9b58-4ec8-a73e-5847668a9298",
              "isOptional": true
            },
            {
              "name": "Version",
              "value": "2.0",
              "isOptional": true
            },
            {
              "name": "TimeRange",
              "value": "P7D",
              "isOptional": true
            },
            {
              "name": "DashboardId",
              "isOptional": true
            },
            {
              "name": "DraftRequestParameters",
              "isOptional": true
            },
            {
              "name": "Query",
              "value": "customEvents\n| where name contains \"ONBOARDING-CDC\"\n| where customMeasurements contains \"OnboardingsUpdate_failures\"\n| where customDimensions contains \"_id\"\n| extend d=parse_json(customDimensions)\n| extend documentKey = d[\"documentKey\"]\n| where documentKey contains \"_id\"\n| project-rename onboardingId = documentKey\n| project onboardingId\n",
              "isOptional": true
            },
            {
              "name": "ControlType",
              "value": "AnalyticsGrid",
              "isOptional": true
            },
            {
              "name": "SpecificChart",
              "isOptional": true
            },
            {
              "name": "PartTitle",
              "value": "Analytics",
              "isOptional": true
            },
            {
              "name": "PartSubTitle",
              "value": "${prefix}-appinsights",
              "isOptional": true
            },
            {
              "name": "Dimensions",
              "isOptional": true
            },
            {
              "name": "LegendOptions",
              "isOptional": true
            },
            {
              "name": "IsQueryContainTimeRange",
              "value": false,
              "isOptional": true
            }
          ],
          "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
          "settings": {
            "content": {
              "GridColumnsWidth": {
                "onboardingId": "506px"
              },
              "PartTitle": "Onboarding id of failed Notification invocations"
            }
          }
        }
      },
      "4": {
        "position": {
          "x": 0,
          "y": 8,
          "colSpan": 7,
          "rowSpan": 4
        },
        "metadata": {
          "inputs": [
            {
              "name": "resourceTypeMode",
              "isOptional": true
            },
            {
              "name": "ComponentId",
              "isOptional": true
            },
            {
              "name": "Scope",
              "value": {
                "resourceIds": [
                  "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                ]
              },
              "isOptional": true
            },
            {
              "name": "PartId",
              "value": "81d9d129-c43b-4a75-a9a5-0f011489a1d4",
              "isOptional": true
            },
            {
              "name": "Version",
              "value": "2.0",
              "isOptional": true
            },
            {
              "name": "TimeRange",
              "value": "P7D",
              "isOptional": true
            },
            {
              "name": "DashboardId",
              "isOptional": true
            },
            {
              "name": "DraftRequestParameters",
              "isOptional": true
            },
            {
              "name": "Query",
              "value": "customEvents\n| where name contains \"ONBOARDING-FN\"\n| extend d=parse_json(customDimensions)\n| where d[\"topic\"] == \"SC-Contracts\"\n| order by timestamp desc\n| project timestamp\n| summarize Count = count() by bin(timestamp, 6h)\n",
              "isOptional": true
            },
            {
              "name": "ControlType",
              "value": "FrameControlChart",
              "isOptional": true
            },
            {
              "name": "SpecificChart",
              "value": "Line",
              "isOptional": true
            },
            {
              "name": "PartTitle",
              "value": "Analytics",
              "isOptional": true
            },
            {
              "name": "PartSubTitle",
              "value": "${prefix}-appinsights",
              "isOptional": true
            },
            {
              "name": "Dimensions",
              "value": {
                "xAxis": {
                  "name": "timestamp",
                  "type": "datetime"
                },
                "yAxis": [
                  {
                    "name": "Count",
                    "type": "long"
                  }
                ],
                "splitBy": [],
                "aggregation": "Sum"
              },
              "isOptional": true
            },
            {
              "name": "LegendOptions",
              "value": {
                "isEnabled": true,
                "position": "Bottom"
              },
              "isOptional": true
            },
            {
              "name": "IsQueryContainTimeRange",
              "value": false,
              "isOptional": true
            }
          ],
          "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
          "settings": {
            "content": {
              "PartTitle": "Count Notifications sent on SC-Contracts"
            }
          }
        }
      },
      "5": {
        "position": {
          "x": 7,
          "y": 8,
          "colSpan": 9,
          "rowSpan": 4
        },
        "metadata": {
          "inputs": [
            {
              "name": "resourceTypeMode",
              "isOptional": true
            },
            {
              "name": "ComponentId",
              "isOptional": true
            },
            {
              "name": "Scope",
              "value": {
                "resourceIds": [
                  "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                ]
              },
              "isOptional": true
            },
            {
              "name": "PartId",
              "value": "11ff3635-154f-4581-8e01-363be8dc88ad",
              "isOptional": true
            },
            {
              "name": "Version",
              "value": "2.0",
              "isOptional": true
            },
            {
              "name": "TimeRange",
              "value": "P7D",
              "isOptional": true
            },
            {
              "name": "DashboardId",
              "isOptional": true
            },
            {
              "name": "DraftRequestParameters",
              "isOptional": true
            },
            {
              "name": "Query",
              "value": "customEvents\n| where name contains \"ONBOARDING-FN\"\n| extend d=parse_json(customDimensions)\n| where d[\"topic\"] == \"SC-Contracts\"\n| order by timestamp desc\n| project timestamp, d\n",
              "isOptional": true
            },
            {
              "name": "ControlType",
              "value": "AnalyticsGrid",
              "isOptional": true
            },
            {
              "name": "SpecificChart",
              "isOptional": true
            },
            {
              "name": "PartTitle",
              "value": "Analytics",
              "isOptional": true
            },
            {
              "name": "PartSubTitle",
              "value": "${prefix}-appinsights",
              "isOptional": true
            },
            {
              "name": "Dimensions",
              "isOptional": true
            },
            {
              "name": "LegendOptions",
              "isOptional": true
            },
            {
              "name": "IsQueryContainTimeRange",
              "value": false,
              "isOptional": true
            }
          ],
          "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
          "settings": {
            "content": {
              "GridColumnsWidth": {
                "d": "595.998px"
              },
              "PartTitle": "Notifications sent on SC-Contracts"
            }
          }
        }
      },
      "6": {
        "position": {
          "x": 0,
          "y": 12,
          "colSpan": 7,
          "rowSpan": 4
        },
        "metadata": {
          "inputs": [
            {
              "name": "resourceTypeMode",
              "isOptional": true
            },
            {
              "name": "ComponentId",
              "isOptional": true
            },
            {
              "name": "Scope",
              "value": {
                "resourceIds": [
                  "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                ]
              },
              "isOptional": true
            },
            {
              "name": "PartId",
              "value": "410407fc-0aa7-4ef1-9998-0136a1110502",
              "isOptional": true
            },
            {
              "name": "Version",
              "value": "2.0",
              "isOptional": true
            },
            {
              "name": "TimeRange",
              "value": "2024-09-10T06:50:21.000Z/2024-10-04T06:50:21.381Z",
              "isOptional": true
            },
            {
              "name": "DashboardId",
              "isOptional": true
            },
            {
              "name": "DraftRequestParameters",
              "isOptional": true
            },
            {
              "name": "Query",
              "value": "customEvents\n| where name contains \"ONBOARDING-FN\"\n| extend d=parse_json(customDimensions)\n| where d[\"topic\"] == \"SC-Contracts-SAP\"\n| order by timestamp desc\n| project timestamp\n| summarize Count = count() by bin(timestamp, 6h)\n",
              "isOptional": true
            },
            {
              "name": "ControlType",
              "value": "FrameControlChart",
              "isOptional": true
            },
            {
              "name": "SpecificChart",
              "value": "Line",
              "isOptional": true
            },
            {
              "name": "PartTitle",
              "value": "Analytics",
              "isOptional": true
            },
            {
              "name": "PartSubTitle",
              "value": "${prefix}-appinsights",
              "isOptional": true
            },
            {
              "name": "Dimensions",
              "value": {
                "xAxis": {
                  "name": "timestamp",
                  "type": "datetime"
                },
                "yAxis": [
                  {
                    "name": "Count",
                    "type": "long"
                  }
                ],
                "splitBy": [],
                "aggregation": "Sum"
              },
              "isOptional": true
            },
            {
              "name": "LegendOptions",
              "value": {
                "isEnabled": true,
                "position": "Bottom"
              },
              "isOptional": true
            },
            {
              "name": "IsQueryContainTimeRange",
              "value": false,
              "isOptional": true
            }
          ],
          "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
          "settings": {
            "content": {
              "PartTitle": "Count Notifications sent on SC-Contracts-SAP"
            }
          }
        }
      },
      "7": {
        "position": {
          "x": 7,
          "y": 12,
          "colSpan": 9,
          "rowSpan": 4
        },
        "metadata": {
          "inputs": [
            {
              "name": "resourceTypeMode",
              "isOptional": true
            },
            {
              "name": "ComponentId",
              "isOptional": true
            },
            {
              "name": "Scope",
              "value": {
                "resourceIds": [
                  "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                ]
              },
              "isOptional": true
            },
            {
              "name": "PartId",
              "value": "12f6ef02-2b87-4163-af10-695d5fc7d51f",
              "isOptional": true
            },
            {
              "name": "Version",
              "value": "2.0",
              "isOptional": true
            },
            {
              "name": "TimeRange",
              "value": "2024-09-10T06:50:21.000Z/2024-10-04T06:50:21.381Z",
              "isOptional": true
            },
            {
              "name": "DashboardId",
              "isOptional": true
            },
            {
              "name": "DraftRequestParameters",
              "isOptional": true
            },
            {
              "name": "Query",
              "value": "customEvents\n| where name contains \"ONBOARDING-FN\"\n| extend d=parse_json(customDimensions)\n| where d[\"topic\"] == \"SC-Contracts-SAP\"\n| order by timestamp desc\n| project timestamp, d\n",
              "isOptional": true
            },
            {
              "name": "ControlType",
              "value": "AnalyticsGrid",
              "isOptional": true
            },
            {
              "name": "SpecificChart",
              "isOptional": true
            },
            {
              "name": "PartTitle",
              "value": "Analytics",
              "isOptional": true
            },
            {
              "name": "PartSubTitle",
              "value": "${prefix}-appinsights",
              "isOptional": true
            },
            {
              "name": "Dimensions",
              "isOptional": true
            },
            {
              "name": "LegendOptions",
              "isOptional": true
            },
            {
              "name": "IsQueryContainTimeRange",
              "value": false,
              "isOptional": true
            }
          ],
          "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
          "settings": {
            "content": {
              "PartTitle": "Notifications sent on SC-Contracts-SAP"
            }
          }
        }
      }
    }
  }
},
"metadata": {
  "model": {
    "timeRange": {
      "value": {
        "relative": {
          "duration": 24,
          "timeUnit": 1
        }
      },
      "type": "MsPortalFx.Composition.Configuration.ValueTypes.TimeRange"
    },
    "filterLocale": {
      "value": "en-us"
    },
    "filters": {
      "value": {
        "MsPortalFx_TimeRange": {
          "model": {
            "format": "utc",
            "granularity": "auto",
            "relative": "24h"
          },
          "displayCache": {
            "name": "UTC Time",
            "value": "Past 24 hours"
          },
          "filteredPartIds": [
            "StartboardPart-LogsDashboardPart-5b5cf61c-c51a-40ae-944f-f80f33cc812f",
            "StartboardPart-LogsDashboardPart-5b5cf61c-c51a-40ae-944f-f80f33cc817c",
            "StartboardPart-LogsDashboardPart-5b5cf61c-c51a-40ae-944f-f80f33cc81a7",
            "StartboardPart-LogsDashboardPart-5b5cf61c-c51a-40ae-944f-f80f33cc81dc",
            "StartboardPart-LogsDashboardPart-5b5cf61c-c51a-40ae-944f-f80f33cc8219",
            "StartboardPart-LogsDashboardPart-5b5cf61c-c51a-40ae-944f-f80f33cc82ae",
            "StartboardPart-LogsDashboardPart-5b5cf61c-c51a-40ae-944f-f80f33cc82fd",
            "StartboardPart-LogsDashboardPart-5b5cf61c-c51a-40ae-944f-f80f33cc8355"
          ]
        }
      }
    }
  }
}
}