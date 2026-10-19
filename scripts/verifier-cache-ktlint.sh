#!/usr/bin/env bash
# Vérifie que la sortie en cache de ktlint ne passe pas d'une copie du dépôt à une autre (P5.31).
#
# Deux clones A et B du dépôt partagent un cache de build local (isolé dans le dossier de travail,
# le cache de ~/.gradle n'est pas touché) :
#   1. A lance ktlintCheck sur core:game : le cache se remplit ;
#   2. B lance la même analyse : sans la parade de build-logic, B reprend du cache une sortie qui
#      cite les fichiers de A par leur chemin absolu ;
#   3. A introduit une violation de style, sans rien lancer (état intermédiaire d'un autre agent) ;
#   4. B modifie un autre fichier, sans violation, et relance ktlintCheck : le plugin réanalyse les
#      fichiers cités dans la sortie précédente. Sans la parade, B échoue sur les fichiers de A.
#
# Le dépôt testé est HEAD plus les modifications non commitées (git diff HEAD) du dépôt source.
# Usage : scripts/verifier-cache-ktlint.sh [dépôt source (défaut .)] [dossier de travail (défaut mktemp)]
# Code retour : 0 si B reste vert et ne cite que ses propres fichiers, 1 si le défaut est reproduit,
# 2 si le scénario n'a pas pu se dérouler. Durée : ~1 min.
# Le dossier de travail est effacé en cas de succès s'il a été créé par le script (mktemp) ; il est
# gardé en cas d'échec, ou s'il a été passé en argument.
set -uo pipefail

src="$(cd "${1:-.}" && pwd)"
if [ $# -ge 2 ]; then
    work="$2"
    created=false
else
    work="$(mktemp -d)"
    created=true
fi
mkdir -p "$work"
work="$(cd "$work" && pwd)"

cleanup() {
    local status=$?
    if [ "$status" -eq 0 ] && [ "$created" = true ]; then
        rm -rf "${work:?}"
    elif [ "$status" -ne 0 ]; then
        echo "Dossier de travail conservé : $work" >&2
    fi
}
trap cleanup EXIT

fail() {
    echo "$1" >&2
    exit 2
}

for c in A B; do
    [ -e "$work/$c" ] && fail "Le dossier de travail contient déjà $c : en choisir un vide."
done

export BLOX_KTLINT_CACHE_DIR="$work/build-cache"
init="$work/cache-isole.init.gradle.kts"
cat > "$init" <<'EOF'
beforeSettings {
    buildCache { local { directory = File(System.getenv("BLOX_KTLINT_CACHE_DIR")) } }
}
EOF

for c in A B; do
    git clone -q "$src" "$work/$c" || fail "Clonage de $src impossible."
    (cd "$src" && git diff --binary HEAD) | (cd "$work/$c" && git apply --allow-empty) ||
        fail "Les modifications non commitées de $src ne s'appliquent pas au clone $c."
    if [ -f "$src/local.properties" ]; then
        cp "$src/local.properties" "$work/$c/" || fail "Copie de local.properties impossible."
    fi
done

gradle_args=(--init-script "$init" --build-cache --console=plain)
model=core/game/src/main/kotlin/ch/magori/blox/core/game/model
errors=core/game/build/intermediates/ktLint/runKtlintCheckOverMainSourceSet_errors.bin

# Sortie de B présente : à vérifier dans le shell principal, `fail` ne sortirait que d'un `$(…)`.
require_errors() {
    [ -f "$work/B/$errors" ] || fail "Sortie ktlint absente : $work/B/$errors"
}

# Nombre de fichiers de la copie $1 (A ou B) cités dans la sortie de B (chemins, pas lignes).
cited_in_b() {
    grep -aoE "[\\\\/]$1[\\\\/]core[\\\\/]game" "$work/B/$errors" | wc -l
}

echo "== 1. A : ktlintCheck (remplit le cache)"
(cd "$work/A" && ./gradlew "${gradle_args[@]}" -q :core:game:ktlintCheck) || fail "A échoue déjà."

echo "== 2. B : même analyse, autre chemin"
(cd "$work/B" && ./gradlew "${gradle_args[@]}" :core:game:ktlintCheck > "$work/b2.log" 2>&1) ||
    fail "B échoue déjà à l'étape 2 (journal : $work/b2.log)."
grep "runKtlintCheckOverMainSourceSet" "$work/b2.log"
require_errors
echo "   fichiers cités dans la sortie de B : $(cited_in_b A) de A, $(cited_in_b B) de B"

echo "== 3. A : violation de style, sans build"
printf 'val   fautif = 1\n' >> "$work/A/$model/Lines.kt"

echo "== 4. B : modification valide d'un autre fichier, puis ktlintCheck"
printf '\n// modification valide\n' >> "$work/B/$model/Points.kt"
(cd "$work/B" && ./gradlew "${gradle_args[@]}" -q :core:game:ktlintCheck > "$work/b.log" 2>&1)
rc=$?
require_errors
leaked=$(cited_in_b A)

if [ "$rc" -eq 0 ] && [ "$leaked" -eq 0 ]; then
    echo "OK : B est vert et ne cite que ses propres fichiers."
    exit 0
fi
echo "ÉCHEC : code retour de B = $rc, fichiers de A cités par B = $leaked (journal : $work/b.log)"
exit 1
