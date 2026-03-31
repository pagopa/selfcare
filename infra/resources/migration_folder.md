# infra/resources — folder map

Tabella delle migrazioni effettuate

```
+----------------+--------+----------+--------+----------+---------+-----------+
| category       | dev-ar | dev-pnpg | uat-ar | uat-pnpg | prod-ar | prod-pnpg |
+----------------+--------+----------+--------+----------+---------+-----------+
| auth           |   ✓    |          |        |          |         |           |
| document-ms    |   ✓    |          |        |          |         |           |
| iam            |   ✓    |    ✓     |        |          |         |           |
| namirial-sign  |        |          |        |          |         |           |
| onboarding-bff |   ✓    |    ✓     |        |          |         |           |
| onboarding-cdc |   ✓    |    ✓     |        |          |         |           |
| onboarding-fn  |   ✓    |    ✓     |        |          |         |           |
| onboarding-ms  |   ✓    |    ✓     |        |          |         |           |
| product        |   ✓    |          |        |          |         |           |
| product-cdc    |   ✓    |          |        |          |         |           |
| registry-proxy |        |          |        |          |         |           |
| spid-login     |   x    |          |   x    |          |    x    |     ◌     |
| webhook        |        |          |        |          |         |           |
+----------------+--------+----------+--------+----------+---------+-----------+
```

Legend:
- ` ` directory with Terraform files
- `◌` directory exists but empty (not yet implemented)
- ` ` not applicable for this category/environment combination
- `x` this category/environment should not be used
