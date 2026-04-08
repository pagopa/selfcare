# infra/resources — folder map

Tabella delle migrazioni effettuate

```
+----------------+--------+--------+--------+--------+--------+--------+
| category       | dev-ar | dev-pg | uat-ar | uat-pg | prd-ar | prd-pg |
+----------------+--------+--------+--------+--------+--------+--------+
| auth           |   ✓    |   ◌    |   ✓    |   ◌    |   ✓    |   ◌    |
| document-ms    |   ✓    |   x    |   ◌    |   x    |   ◌    |   x    |
| iam            |   ✓    |   ✓    |   ✓    |   ✓    |   ✓    |   ✓    |
| namirial-sign  |   ✓    |   x    |   ✓    |   x    |   ✓    |   x    |
| onboarding-bff |   ✓    |   ✓    |   ✓    |   ✓    |   ✓    |   ✓    |
| onboarding-cdc |   ✓    |   ✓    |   ✓    |   ✓    |   ✓    |   x    |
| onboarding-fn  |   ✓    |   ✓    |   ✓    |   ✓    |   ✓    |   ✓    |
| onboarding-ms  |   ✓    |   ✓    |   ✓    |   ✓    |   ✓    |   ✓    |
| product        |   ✓    |   ✓    |   ✓    |   ✓    |   ✓    |   ✓    |
| product-cdc    |   ✓    |   x    |   ✓    |   x    |   ✓    |   x    |
| registry-proxy |   ✓    |   ✓    |   ✓    |   ✓    |        |        |
| spid-login     |   x    |   ✓    |   x    |   ✓    |   x    |   ✓    |
| webhook        |   ✓    |   x    |   ✓    |   x    |        |   x    |
+----------------+--------+--------+--------+--------+--------+--------+
```

Legend:
- ` ` directory with Terraform files
- `◌` directory exists but empty (not yet implemented)
- ` ` not applicable for this category/environment combination
- `x` this category/environment should not be used
