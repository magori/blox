# Skills Android officiels

Copiés tels quels depuis le dépôt Google [android/skills](https://github.com/android/skills)
(licence Apache 2.0, voir `LICENSE-android-skills.txt`), révision `5bda39f` du 2026-10-18.
Ils suivent le standard ouvert des agent skills ; Claude Code les lit dans ce dossier.

| Skill | Origine | Tâche qu'il sert |
|---|---|---|
| `navigation-3` | `navigation/navigation-3` | P0.13 (ADR Navigation 3), P2, P3 |
| `edge-to-edge` | `system/edge-to-edge` | P2.3, P5.3 |
| `testing-setup` | `testing/testing-setup` | P5.5, P5.7 |
| `r8-analyzer` | `performance/r8-analyzer` | P5.6 |
| `jetpack-compose-performance` | `jetpack-compose/jetpack-compose-performance` | P2.3 (plateau en Canvas à 60 Hz) |

**Ne pas modifier ces dossiers** : la mise à jour consiste à recopier le dossier depuis le dépôt
(`android skills add <nom> --project=.` avec le CLI Android, ou copie manuelle) et à mettre à
jour la révision ci-dessus. Un skill adapté à BloX doit être renommé, sinon la mise à jour
l'écrase.

Les autres skills du dépôt (`navigation-event`, `adaptive`, `android-cli`…) s'ajoutent quand une
tâche les demande.
