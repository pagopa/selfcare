{
  "lenses": {
    "0": {
      "order": 0,
      "parts": {
        "0": {
          "position": { "x": 0, "y": 0, "colSpan": 12, "rowSpan": 4 },
          "metadata": {
            "inputs": [
              { "name": "resourceTypeMode", "isOptional": true },
              { "name": "ComponentId", "isOptional": true },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                  ]
                },
                "isOptional": true
              },
              { "name": "PartId", "value": "a1b2c3d4-0001-4000-8001-000000000001", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| summarize Count=count() by cloud_RoleName, bin(timestamp, 1h)\n| order by timestamp asc\n",
                "isOptional": true
              },
              { "name": "ControlType", "value": "FrameControlChart", "isOptional": true },
              { "name": "SpecificChart", "value": "Line", "isOptional": true },
              { "name": "PartTitle", "value": "Analytics", "isOptional": true },
              { "name": "PartSubTitle", "value": "${prefix}-appinsights", "isOptional": true },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": { "name": "timestamp", "type": "datetime" },
                  "yAxis": [{ "name": "Count", "type": "long" }],
                  "splitBy": [{ "name": "cloud_RoleName", "type": "string" }],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              { "name": "LegendOptions", "value": { "isEnabled": true, "position": "Bottom" }, "isOptional": true },
              { "name": "IsQueryContainTimeRange", "value": false, "isOptional": true }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "PartTitle": "Request Rate per Service"
              }
            }
          }
        },
        "1": {
          "position": { "x": 12, "y": 0, "colSpan": 12, "rowSpan": 4 },
          "metadata": {
            "inputs": [
              { "name": "resourceTypeMode", "isOptional": true },
              { "name": "ComponentId", "isOptional": true },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                  ]
                },
                "isOptional": true
              },
              { "name": "PartId", "value": "a1b2c3d4-0001-4000-8001-000000000002", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| where toint(resultCode) >= 400\n| summarize Count=count() by cloud_RoleName, bin(timestamp, 1h)\n| order by timestamp asc\n",
                "isOptional": true
              },
              { "name": "ControlType", "value": "FrameControlChart", "isOptional": true },
              { "name": "SpecificChart", "value": "Bar", "isOptional": true },
              { "name": "PartTitle", "value": "Analytics", "isOptional": true },
              { "name": "PartSubTitle", "value": "${prefix}-appinsights", "isOptional": true },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": { "name": "timestamp", "type": "datetime" },
                  "yAxis": [{ "name": "Count", "type": "long" }],
                  "splitBy": [{ "name": "cloud_RoleName", "type": "string" }],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              { "name": "LegendOptions", "value": { "isEnabled": true, "position": "Bottom" }, "isOptional": true },
              { "name": "IsQueryContainTimeRange", "value": false, "isOptional": true }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "PartTitle": "Error Rate per Service (HTTP 4xx/5xx)"
              }
            }
          }
        },
        "2": {
          "position": { "x": 0, "y": 4, "colSpan": 12, "rowSpan": 4 },
          "metadata": {
            "inputs": [
              { "name": "resourceTypeMode", "isOptional": true },
              { "name": "ComponentId", "isOptional": true },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                  ]
                },
                "isOptional": true
              },
              { "name": "PartId", "value": "a1b2c3d4-0001-4000-8001-000000000003", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| summarize p95=percentile(duration, 95) by cloud_RoleName, bin(timestamp, 1h)\n| order by timestamp asc\n",
                "isOptional": true
              },
              { "name": "ControlType", "value": "FrameControlChart", "isOptional": true },
              { "name": "SpecificChart", "value": "Line", "isOptional": true },
              { "name": "PartTitle", "value": "Analytics", "isOptional": true },
              { "name": "PartSubTitle", "value": "${prefix}-appinsights", "isOptional": true },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": { "name": "timestamp", "type": "datetime" },
                  "yAxis": [{ "name": "p95", "type": "real" }],
                  "splitBy": [{ "name": "cloud_RoleName", "type": "string" }],
                  "aggregation": "Max"
                },
                "isOptional": true
              },
              { "name": "LegendOptions", "value": { "isEnabled": true, "position": "Bottom" }, "isOptional": true },
              { "name": "IsQueryContainTimeRange", "value": false, "isOptional": true }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "PartTitle": "Latency p95 per Service (ms)"
              }
            }
          }
        },
        "3": {
          "position": { "x": 12, "y": 4, "colSpan": 12, "rowSpan": 4 },
          "metadata": {
            "inputs": [
              { "name": "resourceTypeMode", "isOptional": true },
              { "name": "ComponentId", "isOptional": true },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                  ]
                },
                "isOptional": true
              },
              { "name": "PartId", "value": "a1b2c3d4-0001-4000-8001-000000000004", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| summarize Total=count(), Success=countif(toint(resultCode) < 400) by cloud_RoleName, bin(timestamp, 1h)\n| extend Availability=round(100.0 * Success / Total, 2)\n| project timestamp, cloud_RoleName, Availability\n| order by timestamp asc\n",
                "isOptional": true
              },
              { "name": "ControlType", "value": "FrameControlChart", "isOptional": true },
              { "name": "SpecificChart", "value": "Line", "isOptional": true },
              { "name": "PartTitle", "value": "Analytics", "isOptional": true },
              { "name": "PartSubTitle", "value": "${prefix}-appinsights", "isOptional": true },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": { "name": "timestamp", "type": "datetime" },
                  "yAxis": [{ "name": "Availability", "type": "real" }],
                  "splitBy": [{ "name": "cloud_RoleName", "type": "string" }],
                  "aggregation": "Min"
                },
                "isOptional": true
              },
              { "name": "LegendOptions", "value": { "isEnabled": true, "position": "Bottom" }, "isOptional": true },
              { "name": "IsQueryContainTimeRange", "value": false, "isOptional": true }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "PartTitle": "Availability % per Service"
              }
            }
          }
        },
        "4": {
          "position": { "x": 0, "y": 8, "colSpan": 12, "rowSpan": 4 },
          "metadata": {
            "inputs": [
              { "name": "resourceTypeMode", "isOptional": true },
              { "name": "ComponentId", "isOptional": true },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                  ]
                },
                "isOptional": true
              },
              { "name": "PartId", "value": "a1b2c3d4-0001-4000-8001-000000000005", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "exceptions\n| summarize Count=count() by cloud_RoleName, bin(timestamp, 1h)\n| order by timestamp asc\n",
                "isOptional": true
              },
              { "name": "ControlType", "value": "FrameControlChart", "isOptional": true },
              { "name": "SpecificChart", "value": "Bar", "isOptional": true },
              { "name": "PartTitle", "value": "Analytics", "isOptional": true },
              { "name": "PartSubTitle", "value": "${prefix}-appinsights", "isOptional": true },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": { "name": "timestamp", "type": "datetime" },
                  "yAxis": [{ "name": "Count", "type": "long" }],
                  "splitBy": [{ "name": "cloud_RoleName", "type": "string" }],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              { "name": "LegendOptions", "value": { "isEnabled": true, "position": "Bottom" }, "isOptional": true },
              { "name": "IsQueryContainTimeRange", "value": false, "isOptional": true }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "PartTitle": "Exceptions per Service"
              }
            }
          }
        },
        "5": {
          "position": { "x": 12, "y": 8, "colSpan": 12, "rowSpan": 4 },
          "metadata": {
            "inputs": [
              { "name": "resourceTypeMode", "isOptional": true },
              { "name": "ComponentId", "isOptional": true },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                  ]
                },
                "isOptional": true
              },
              { "name": "PartId", "value": "a1b2c3d4-0001-4000-8001-000000000006", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "dependencies\n| where success == false\n| summarize Count=count() by target, bin(timestamp, 1h)\n| order by timestamp asc\n",
                "isOptional": true
              },
              { "name": "ControlType", "value": "FrameControlChart", "isOptional": true },
              { "name": "SpecificChart", "value": "Bar", "isOptional": true },
              { "name": "PartTitle", "value": "Analytics", "isOptional": true },
              { "name": "PartSubTitle", "value": "${prefix}-appinsights", "isOptional": true },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": { "name": "timestamp", "type": "datetime" },
                  "yAxis": [{ "name": "Count", "type": "long" }],
                  "splitBy": [{ "name": "target", "type": "string" }],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              { "name": "LegendOptions", "value": { "isEnabled": true, "position": "Bottom" }, "isOptional": true },
              { "name": "IsQueryContainTimeRange", "value": false, "isOptional": true }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "PartTitle": "Failed Dependencies (MongoDB, Service Bus)"
              }
            }
          }
        },
        "6": {
          "position": { "x": 0, "y": 12, "colSpan": 12, "rowSpan": 4 },
          "metadata": {
            "inputs": [
              { "name": "resourceTypeMode", "isOptional": true },
              { "name": "ComponentId", "isOptional": true },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                  ]
                },
                "isOptional": true
              },
              { "name": "PartId", "value": "a1b2c3d4-0001-4000-8001-000000000007", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| summarize avg_duration=avg(duration), p95=percentile(duration, 95), calls=count() by name, cloud_RoleName\n| top 10 by p95 desc\n| project cloud_RoleName, name, avg_ms=round(avg_duration, 2), p95_ms=round(p95, 2), calls\n",
                "isOptional": true
              },
              { "name": "ControlType", "value": "AnalyticsGrid", "isOptional": true },
              { "name": "SpecificChart", "isOptional": true },
              { "name": "PartTitle", "value": "Analytics", "isOptional": true },
              { "name": "PartSubTitle", "value": "${prefix}-appinsights", "isOptional": true },
              { "name": "Dimensions", "isOptional": true },
              { "name": "LegendOptions", "isOptional": true },
              { "name": "IsQueryContainTimeRange", "value": false, "isOptional": true }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "PartTitle": "Top 10 Slowest Endpoints (p95)"
              }
            }
          }
        },
        "7": {
          "position": { "x": 12, "y": 12, "colSpan": 12, "rowSpan": 4 },
          "metadata": {
            "inputs": [
              { "name": "resourceTypeMode", "isOptional": true },
              { "name": "ComponentId", "isOptional": true },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                  ]
                },
                "isOptional": true
              },
              { "name": "PartId", "value": "a1b2c3d4-0001-4000-8001-000000000008", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "exceptions\n| order by timestamp desc\n| project timestamp, cloud_RoleName, type, outerMessage\n| take 20\n",
                "isOptional": true
              },
              { "name": "ControlType", "value": "AnalyticsGrid", "isOptional": true },
              { "name": "SpecificChart", "isOptional": true },
              { "name": "PartTitle", "value": "Analytics", "isOptional": true },
              { "name": "PartSubTitle", "value": "${prefix}-appinsights", "isOptional": true },
              { "name": "Dimensions", "isOptional": true },
              { "name": "LegendOptions", "isOptional": true },
              { "name": "IsQueryContainTimeRange", "value": false, "isOptional": true }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "PartTitle": "Recent Exceptions"
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
              "StartboardPart-LogsDashboardPart-overview-a1b2c3d4-0001-4000-8001-000000000001",
              "StartboardPart-LogsDashboardPart-overview-a1b2c3d4-0001-4000-8001-000000000002",
              "StartboardPart-LogsDashboardPart-overview-a1b2c3d4-0001-4000-8001-000000000003",
              "StartboardPart-LogsDashboardPart-overview-a1b2c3d4-0001-4000-8001-000000000004",
              "StartboardPart-LogsDashboardPart-overview-a1b2c3d4-0001-4000-8001-000000000005",
              "StartboardPart-LogsDashboardPart-overview-a1b2c3d4-0001-4000-8001-000000000006",
              "StartboardPart-LogsDashboardPart-overview-a1b2c3d4-0001-4000-8001-000000000007",
              "StartboardPart-LogsDashboardPart-overview-a1b2c3d4-0001-4000-8001-000000000008"
            ]
          }
        }
      }
    }
  }
}
