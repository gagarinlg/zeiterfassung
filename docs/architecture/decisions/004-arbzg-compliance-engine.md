# ADR 004: ArbZG Compliance Engine Design

## Status

Accepted

## Context

German labor law (Arbeitszeitgesetz — ArbZG) imposes strict rules on working hours, mandatory breaks, and rest periods. The system must enforce these rules and flag violations.

## Decision

We implement an **ArbZGComplianceService** that validates daily summaries against the following rules:

1. **Maximum daily work**: 10 hours (§3 ArbZG). Average must not exceed 8 hours over 6 months.
2. **Mandatory breaks** (§4 ArbZG):
   - 30 minutes after 6 hours of work.
   - 45 minutes after 9 hours of work.
   - Only breaks ≥ 15 minutes count as qualifying breaks.
3. **Rest period**: Minimum 11 hours between work days (§5 ArbZG).
4. **Auto-deduction**: If qualifying breaks were not taken, the missing time is automatically deducted from work time, respecting threshold-aware rules to avoid contradictions.
5. **Compliance flag**: Each daily summary carries `is_compliant` and `compliance_notes` fields.

### Break gap detection

Gaps of ≥ 15 minutes between a CLOCK_OUT and the next CLOCK_IN on the same day are treated as qualifying breaks.

## Consequences

- Every clock-out triggers a daily summary recalculation with compliance checks.
- Non-compliant days are flagged in the UI with specific ArbZG section references.
- Managers and admins can review compliance reports.
- The threshold-aware deduction algorithm prevents edge-case contradictions.
