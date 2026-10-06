# Product Module

## Overview

## Key Features

## Architecture


## Optional tenant storage bindings

Product MS uses tenant SDK `0.4.0` together with tenant-mongodb `0.3.0`. The
tenant SDK models `tenant.storage.mandatory-keys` as an optional property, so an
environment can explicitly clear `TENANT_STORAGE_MANDATORY_KEYS` when that
tenant does not use blob storage. PNPG relies on this behavior because it does
not use contract templates; AR keeps `contracts` mandatory. Every PNPG
environment keeps `TENANT_STORAGE_MANDATORY_KEYS` explicitly empty to clear the
application default and does not configure contract storage, container or
credentials.

## Data Model
