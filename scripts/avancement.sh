#!/usr/bin/env bash
# Recalcule les pourcentages d'avancement de SUIVI.md et les écrit entre les marqueurs
# <!-- avancement:debut --> et <!-- avancement:fin -->.
#   - général : tâches cochées / toutes les tâches identifiées (P0.1, P1.12, P4.19a…) ; une tâche
#     découpée en tranches (sous-liste indentée) ne compte que par ses tranches ;
#   - MVP audio : tâches cochées / tâches listées dans le commentaire <!-- mvp-audio: … -->.
# Usage : scripts/avancement.sh [chemin de SUIVI.md]   (défaut : SUIVI.md)
set -euo pipefail

suivi="${1:-SUIVI.md}"
tmp="$(mktemp)"

awk '
    function pct(done, total) { return total == 0 ? 0 : int(done * 100 / total + 0.5) }
    function bar(p,    n, s, i) {
        n = int(p / 5 + 0.5); s = ""
        for (i = 0; i < 20; i++) s = s (i < n ? "█" : "░")
        return s
    }
    # Premier passage : relever les tâches, leurs tranches et le périmètre du MVP audio.
    NR == FNR {
        sub(/\r$/, "")
        if (match($0, /<!-- mvp-audio:[^>]*-->/)) {
            list = substr($0, RSTART + 15, RLENGTH - 18)
            n = split(list, ids, /[ ,]+/)
            for (i = 1; i <= n; i++) if (ids[i] != "") mvp[ids[i]] = 1
        }
        if (match($0, /^ *- \[[x ~]\] \*\*P[0-9]+\.[0-9]+[a-z]?\*\*/)) {
            id = $0; sub(/^ *- \[[x ~]\] \*\*/, "", id); sub(/\*\*.*/, "", id)
            state[id] = ($0 ~ /^ *- \[x\]/)
            if ($0 ~ /^ /) hasSlices[top] = 1; else top = id
            order[++count] = id
        }
        next
    }
    # Second passage : réécrire le bloc entre les marqueurs.
    {
        line = $0; sub(/\r$/, "", line)
        if (line ~ /<!-- avancement:debut -->/) {
            print line
            for (i = 1; i <= count; i++) {
                id = order[i]
                if (id in hasSlices) continue
                total++; done += state[id]
            }
            for (id in mvp) { mvpTotal++; if (id in state) mvpDone += state[id]; else missing = missing " " id }
            g = pct(done, total); m = pct(mvpDone, mvpTotal)
            printf "| Avancement | Tâches | %% |\n|---|---|---|\n"
            printf "| **Général** | %d / %d | `%s` **%d %%** |\n", done, total, bar(g), g
            printf "| **MVP audio** | %d / %d | `%s` **%d %%** |\n", mvpDone, mvpTotal, bar(m), m
            if (missing != "") printf "\nTâches du MVP audio introuvables dans le suivi :%s\n", missing
            skip = 1; next
        }
        if (line ~ /<!-- avancement:fin -->/) skip = 0
        if (!skip) print line
    }
' "$suivi" "$suivi" > "$tmp"

mv "$tmp" "$suivi"
grep -A 5 "<!-- avancement:debut -->" "$suivi" | sed -n '4,5p'
