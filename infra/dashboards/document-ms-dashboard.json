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
              { "name": "PartId", "value": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| where cloud_RoleName == \"document-ms\"\n| summarize Count = count() by bin(timestamp, 1h)\n| order by timestamp asc\n",
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
                  "splitBy": [],
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
                "PartTitle": "HTTP Requests (document-ms)"
              }
            }
          }
        },
        "1": {
          "position": {
            "x": 9,
            "y": 0,
            "colSpan": 7,
            "rowSpan": 4
          },
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
              { "name": "PartId", "value": "b2c3d4e5-f6a7-8901-bcde-f12345678901", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| where cloud_RoleName == \"document-ms\"\n| summarize Failures = countif(success == false), Total = count() by bin(timestamp, 1h)\n| extend FailureRate = round(todouble(Failures) / todouble(Total) * 100, 2)\n| project timestamp, FailureRate\n| order by timestamp asc\n",
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
                  "yAxis": [{ "name": "FailureRate", "type": "real" }],
                  "splitBy": [],
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
                "PartTitle": "Failure Rate % (document-ms)"
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
              { "name": "PartId", "value": "c3d4e5f6-a7b8-9012-cdef-123456789012", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| where cloud_RoleName == \"document-ms\"\n| summarize p50 = percentile(duration, 50), p95 = percentile(duration, 95) by bin(timestamp, 1h)\n| order by timestamp asc\n",
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
                  "yAxis": [
                    { "name": "p50", "type": "real" },
                    { "name": "p95", "type": "real" }
                  ],
                  "splitBy": [],
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
                "PartTitle": "Response Time p50 / p95 ms (document-ms)"
              }
            }
          }
        },
        "3": {
          "position": {
            "x": 9,
            "y": 4,
            "colSpan": 7,
            "rowSpan": 4
          },
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
              { "name": "PartId", "value": "d4e5f6a7-b8c9-0123-def0-234567890123", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "exceptions\n| where cloud_RoleName == \"document-ms\"\n| summarize Count = count() by type, outerMessage\n| order by Count desc\n| take 20\n",
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
                "PartTitle": "Top Exceptions (document-ms)"
              }
            }
          }
        },
        "4": {
          "position": {
            "x": 0,
            "y": 8,
            "colSpan": 8,
            "rowSpan": 4
          },
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
              { "name": "PartId", "value": "e5f6a7b8-c9d0-1234-ef01-345678901234", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| where cloud_RoleName == \"document-ms\"\n| where name contains \"document-content\"\n| summarize Count = count() by bin(timestamp, 1h), name\n| order by timestamp asc\n",
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
                  "splitBy": [{ "name": "name", "type": "string" }],
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
                "PartTitle": "PDF Generation Requests - Contract & Attachment (document-ms)"
              }
            }
          }
        },
        "5": {
          "position": {
            "x": 8,
            "y": 8,
            "colSpan": 8,
            "rowSpan": 4
          },
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
              { "name": "PartId", "value": "f6a7b8c9-d0e1-2345-f012-456789012345", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| where cloud_RoleName == \"document-ms\"\n| where name contains \"signature\" or name contains \"upload-signed-contract\"\n| summarize Count = count() by bin(timestamp, 1h), name\n| order by timestamp asc\n",
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
                  "splitBy": [{ "name": "name", "type": "string" }],
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
                "PartTitle": "Signature & Upload Signed Contract Requests (document-ms)"
              }
            }
          }
        },
        "6": {
          "position": {
            "x": 0,
            "y": 12,
            "colSpan": 8,
            "rowSpan": 4
          },
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
              { "name": "PartId", "value": "a7b8c9d0-e1f2-3456-0123-567890123456", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| where cloud_RoleName == \"document-ms\"\n| summarize Count = count(), AvgDuration = round(avg(duration), 2), FailCount = countif(success == false) by name\n| order by Count desc\n| take 15\n",
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
                "PartTitle": "Top Endpoints by Traffic (document-ms)"
              }
            }
          }
        },
        "7": {
          "position": {
            "x": 8,
            "y": 12,
            "colSpan": 8,
            "rowSpan": 4
          },
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
              { "name": "PartId", "value": "b8c9d0e1-f2a3-4567-1234-678901234567", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| where cloud_RoleName == \"document-ms\"\n| where success == false\n| order by timestamp desc\n| project timestamp, name, resultCode, duration\n| take 50\n",
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
                "GridColumnsWidth": {
                  "name": "300px",
                  "resultCode": "80px",
                  "duration": "100px"
                },
                "PartTitle": "Recent Failed Requests (document-ms)"
              }
            }
          }
        },
        "8": {
          "position": {
            "x": 0,
            "y": 16,
            "colSpan": 9,
            "rowSpan": 4
          },
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
              { "name": "PartId", "value": "c9d0e1f2-a3b4-5678-2345-789012345678", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "customEvents\n| where cloud_RoleName == \"document-ms\"\n| where name in (\n    \"DOCUMENT-MS-PDF-CONTRACT-CREATED\",\n    \"DOCUMENT-MS-PDF-ATTACHMENT-CREATED\",\n    \"DOCUMENT-MS-SIGNED-CONTRACT-UPLOADED\",\n    \"DOCUMENT-MS-SIGNATURE-VERIFIED\",\n    \"DOCUMENT-MS-SIGNATURE-FAILED\"\n  )\n| summarize Count = count() by name, bin(timestamp, 1h)\n| order by timestamp asc\n",
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
                  "splitBy": [{ "name": "name", "type": "string" }],
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
                "PartTitle": "Custom Business Events nel tempo (document-ms)"
              }
            }
          }
        },
        "9": {
          "position": {
            "x": 9,
            "y": 16,
            "colSpan": 7,
            "rowSpan": 4
          },
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
              { "name": "PartId", "value": "d0e1f2a3-b4c5-6789-3456-890123456789", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P1D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "customEvents\n| where cloud_RoleName == \"document-ms\"\n| where name in (\n    \"DOCUMENT-MS-PDF-CONTRACT-CREATED\",\n    \"DOCUMENT-MS-PDF-ATTACHMENT-CREATED\",\n    \"DOCUMENT-MS-SIGNED-CONTRACT-UPLOADED\",\n    \"DOCUMENT-MS-SIGNATURE-VERIFIED\",\n    \"DOCUMENT-MS-SIGNATURE-FAILED\"\n  )\n| extend props = parse_json(tostring(customDimensions))\n| extend onboardingId = tostring(props[\"onboardingId\"]), durationMs = todouble(customMeasurements[\"durationMs\"])\n| order by timestamp desc\n| project timestamp, name, onboardingId, durationMs\n| take 50\n",
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
                "GridColumnsWidth": {
                  "name": "280px",
                  "onboardingId": "220px",
                  "durationMs": "100px"
                },
                "PartTitle": "Custom Events recenti con durata (document-ms)"
              }
            }
          }
        },
        "10": {
          "position": {
            "x": 0,
            "y": 20,
            "colSpan": 9,
            "rowSpan": 4
          },
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
              { "name": "PartId", "value": "e1f2a3b4-c5d6-7890-4567-901234567890", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P7D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| where cloud_RoleName == \"document-ms\"\n| summarize RequestsPerHour = count(), P95DurationMs = percentile(duration, 95) by bin(timestamp, 1h)\n| order by timestamp asc\n",
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
                  "yAxis": [
                    { "name": "RequestsPerHour", "type": "long" },
                    { "name": "P95DurationMs", "type": "real" }
                  ],
                  "splitBy": [],
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
                "PartTitle": "Throughput/ora vs P95 — Supporto dimensionamento istanze (document-ms)"
              }
            }
          }
        },
        "11": {
          "position": {
            "x": 9,
            "y": 20,
            "colSpan": 7,
            "rowSpan": 4
          },
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
              { "name": "PartId", "value": "f2a3b4c5-d6e7-8901-5678-012345678901", "isOptional": true },
              { "name": "Version", "value": "2.0", "isOptional": true },
              { "name": "TimeRange", "value": "P7D", "isOptional": true },
              { "name": "DashboardId", "isOptional": true },
              { "name": "DraftRequestParameters", "isOptional": true },
              {
                "name": "Query",
                "value": "requests\n| where cloud_RoleName == \"document-ms\"\n| summarize RequestsPerHour = count() by bin(timestamp, 1h)\n| summarize MaxLoad = max(RequestsPerHour), AvgLoad = avg(RequestsPerHour), P95Load = percentile(RequestsPerHour, 95)\n",
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
                "PartTitle": "Picco / Media / P95 richieste/ora — Dimensionamento (document-ms)"
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
              "StartboardPart-LogsDashboardPart-document-ms-00",
              "StartboardPart-LogsDashboardPart-document-ms-01",
              "StartboardPart-LogsDashboardPart-document-ms-02",
              "StartboardPart-LogsDashboardPart-document-ms-03",
              "StartboardPart-LogsDashboardPart-document-ms-04",
              "StartboardPart-LogsDashboardPart-document-ms-05",
              "StartboardPart-LogsDashboardPart-document-ms-06",
              "StartboardPart-LogsDashboardPart-document-ms-07"
            ]
          }
        }
      }
    }
  }
}

