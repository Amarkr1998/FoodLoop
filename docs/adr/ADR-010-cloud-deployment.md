# ADR-010: Azure/AKS as primary deployment target, portable adapters

## Status
Accepted

## Context
§49 designates Azure as the primary cloud but requires portability to AWS/GCP later; §51 requires a
clean repository/infrastructure structure supporting this without a rewrite.

## Decision
Deploy on AKS with Azure-managed data services (Postgres Flexible Server, Cache for Redis, Blob
Storage, Key Vault, ACR) provisioned via Terraform and Helm. Cloud-specific SDK calls are isolated
to adapter classes (blob storage adapter, secrets adapter, AI provider adapter) behind interfaces
domain/application code depends on — no Azure SDK type ever appears in a domain or application-
service class.

## Consequences
- A future AWS/GCP target requires new adapter implementations (S3/GCS, Secrets Manager/Secret
  Manager, Bedrock/Vertex) and new Terraform modules, not application rewrites.
- Slight abstraction overhead now for adapters that currently have only one implementation; accepted
  because the spec explicitly requires portability as a design constraint, not a hypothetical.
