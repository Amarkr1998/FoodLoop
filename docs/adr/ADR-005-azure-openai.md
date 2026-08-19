# ADR-005: Azure OpenAI as primary LLM provider

## Status
Accepted

## Context
The platform's primary cloud is Azure (§49); the AI layer needs enterprise-grade LLM inference with
data residency, private networking, and content-filtering controls appropriate for a platform
handling food-safety-adjacent decisions.

## Decision
Azure OpenAI is the primary chat/vision model provider, accessed via private endpoint, integrated
through Spring AI's `ChatModel`/`ImageModel` abstractions so the rest of the codebase depends on the
abstraction, not the Azure SDK directly (see ADR-008 for the fallback chain this enables).

## Consequences
- Consistent cloud vendor for networking/identity/monitoring (Azure AD-integrated access, Key
  Vault-managed keys, Azure Monitor tracing).
- Coupling to Azure OpenAI's model catalog and pricing; mitigated by the provider abstraction
  (ADR-008) that allows Azure AI Foundry or another provider to be substituted without touching
  agent/domain code.
