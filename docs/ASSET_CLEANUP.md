# Public asset cleanup — 2026-10-19

Scope: third-party static assets and identity/contact presentation only.
No business logic, TypeScript-error fixes, dependency changes or existing code-license
removals. Team publication authorization is still pending the user's own confirmation.

## Usage review and changes

| Asset | Reference check | Action |
| --- | --- | --- |
| `frontend/public/robot.png` | No source reference found | Removed |
| `frontend/public/placeholder.png` | No source reference found | Removed |
| `frontend/public/shadcn-vue-admin.png` | No source reference found | Removed, including embedded sample names/emails/avatars |
| `frontend/src/assets/icons/arrow-dark.svg` | No source reference found | Removed |
| `frontend/src/assets/icons/arrow-light.svg` | No source reference found | Removed |
| `frontend/public/logo.svg`, `logo-black.svg` | `frontend/index.html` and `frontend/src/pages/auth/components/auth-title.vue` | Replaced with official Lucide 0.577.0 satellite SVG, color-only adaptation |

No source references to the removed files required deletion. Filename, path and
asset-loading searches were checked across the candidate source. The existing logo
paths and 24×24 dimensions remain valid for the favicon and authentication header.
The prior logos were not an exact match for the checked upstream version, so their
provenance was not assumed. The replacements and complete license are traceable in
`THIRD_PARTY_NOTICES.md` and `frontend/licenses/lucide-LICENSE`.

Removed the Bilibili account link and empty Discord invitation from the help page.
The remaining template GitHub link is explicitly labeled as a third-party resource,
not a team contact. The existing frontend MIT license and Maven Wrapper headers remain.

## Verification

Static checks only: remaining logo XML and upstream geometry comparison, live source
references, staged-file enumeration and secret/identity pattern scan. No build/test
rerun, TypeScript cleanup, commit, remote creation or push was performed.

The scope does not establish authorization to publish the team's code or audit every
transitive dependency's license. Existing frontend build limitations are unchanged.
