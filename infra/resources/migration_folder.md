# infra/resources — folder map

```
+----------------+--------+----------+--------+----------+---------+-----------+
| category       | dev-ar | dev-pnpg | uat-ar | uat-pnpg | prod-ar | prod-pnpg |
+----------------+--------+----------+--------+----------+---------+-----------+
| auth           |   ✓    |          |        |          |         |           |
| document-ms    |        |          |        |          |         |           |
| iam            |        |          |        |          |         |           |
| onboarding     |        |          |        |          |         |           |
| product        |        |          |        |          |         |           |
| registry-proxy |        |          |        |          |         |           |
| search         |        |          |        |          |         |           |
| spid-login     |        |          |        |    ◌     |         |     ◌     |
| webhook        |        |          |        |          |         |           |
+----------------+--------+----------+--------+----------+---------+-----------+
```

Legend:
- ` ` directory with Terraform files
- `◌` directory exists but empty (not yet implemented)
- ` ` not applicable for this category/environment combination
