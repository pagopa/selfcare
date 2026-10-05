# Product Module

## Overview

## Key Features

## Architecture

## OpenAPI response headers

The `HEAD /product/{tenantId}/{productId}/required-documents/enabled` response
declares `X-Required-Documents-Enabled` as a boolean header in the source
annotation. Keep the header schema in generated JSON/YAML contracts: OpenAPI
clients reject response headers that only provide a description without a
schema.

Regenerate the Product MS contract through the Maven reactor and refresh its
versioned consumer copies after changing the endpoint contract.

## Data Model
