# Triage Labels

The skills speak in terms of five canonical triage roles. This file maps those roles to the actual label strings used in this repo's issue tracker.

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| -------------------------- | -------------------- | ---------------------------------------- |
| `needs-triage`             | `bug:triage`         | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`         | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`    | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `dev:todo`           | Requires human implementation            |
| `wontfix`                  | `wontfix`            | Will not be actioned                     |
| `closed`                   | `closed`             | 工单已完成并通过全量单测与审查（终态）   |
| `resolved`                 | `resolved`           | 探索/研究类子工单已决已闭环（终态）     |

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), use the corresponding label string from this table.

Edit the right-hand column to match whatever vocabulary you actually use.
