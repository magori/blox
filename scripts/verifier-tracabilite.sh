#!/usr/bin/env bash
# Vérifie que chaque référence « règle → test » de la documentation désigne un test qui existe.
#
# Lecture des docs (docs/**/*.md, blocs ``` ignorés) :
#   - chaque span de code `…` qui contient « › » est une référence candidate ; un span peut être
#     coupé en fin de ligne (la suite est lue sur la ligne d'après, espaces de tête retirés, avec
#     une espace à la jointure) et garde le numéro de sa première ligne ;
#   - un span qui contient « › » et reste ouvert sur une ligne vide ou en fin de fichier est
#     signalé, puis abandonné (un span ouvert sans « › » est abandonné sans message) ;
#   - une candidate doit avoir la forme `Classe › nom` : Classe en [A-Za-z0-9_], ou
#     Englobante.Interne pour une classe imbriquée (@Nested), puis « › » entouré d'une espace
#     de chaque côté, puis un nom non vide ; toute autre forme (Foo-Test › x, Foo›x…) est signalée ;
#   - la classe générique ClasseDeTest (exemple de syntaxe dans les encadrés) est ignorée.
#
# Règle d'appariement :
#   - la classe (ou, pour Englobante.Interne, la classe englobante) désigne un fichier Classe.kt
#     sous src/test*, src/androidTest ou src/screenshotTest d'un module (build/ exclu) ; si
#     plusieurs fichiers portent ce nom, un seul suffit ; pour Englobante.Interne, le fichier doit
#     aussi déclarer « class Interne » (le nom du test est ensuite cherché dans tout le fichier,
#     pas seulement dans le corps de la classe imbriquée) ;
#   - le nom doit y être déclaré sous l'une de ces formes (les trois premières sont celles du
#     dépôt, @DisplayName est prévu pour plus tard) :
#       fun `nom`               nom de fonction de test
#       name = "nom"            @ParameterizedTest(name = …)
#       argumentSet("nom"       nom d'un jeu d'arguments
#       @DisplayName("nom"
#     Un nom qui n'apparaît que dans une assertion, un commentaire ou une autre chaîne ne compte pas ;
#   - le nom doit y figurer en entier, délimiteur fermant compris, sauf s'il se termine par « … »,
#     « ... » ou « * » : ce qui précède ce marqueur est alors un préfixe, qui doit rester non vide.
#
# Sortie : chaque référence introuvable ou mal formée (fichier:ligne, classe, nom), puis un résumé.
# Code de retour : 1 si une référence est introuvable ou mal formée, ou si aucune n'est trouvée ;
# 0 sinon.
# Usage : scripts/verifier-tracabilite.sh [racine du dépôt]   (défaut : dossier courant)
set -euo pipefail
export LC_ALL=C

root="${1:-.}"
cd "$root"

refs="$(mktemp)"
tests="$(mktemp)"
trap 'rm -f "$refs" "$tests"' EXIT

# Fichiers de test : une ligne par chemin.
find . -path '*/build' -prune -o -type f -name '*.kt' -print \
    | grep -E '/src/(test[^/]*|androidTest|screenshotTest)/' > "$tests" || true

# Spans contenant « › », une ligne chacun :
#   R<TAB>fichier<TAB>ligne<TAB>classe<TAB>nom   référence bien formée ;
#   M<TAB>fichier<TAB>ligne<TAB>span             forme non reconnue ;
#   U<TAB>fichier<TAB>ligne<TAB>span             span resté ouvert.
find docs -type f -name '*.md' | sort | while IFS= read -r doc; do
    awk -v doc="$doc" '
        BEGIN { arrow = "\342\200\272"; sep = " " arrow " " }   # « › » en UTF-8
        function unclosed() {
            if (index(pending, arrow) > 0) printf "U\t%s\t%d\t%s\n", doc, start, pending
            pending = ""; open = 0
        }
        function span(text, at,    i) {
            if (index(text, arrow) == 0) return
            if (text ~ /^ClasseDeTest /) return
            if (text ~ /^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)? \342\200\272 [^ ]/) {
                i = index(text, sep)
                printf "R\t%s\t%d\t%s\t%s\n", doc, at, substr(text, 1, i - 1), substr(text, i + length(sep))
            } else printf "M\t%s\t%d\t%s\n", doc, at, text
        }
        FNR == 1 { pending = ""; open = 0; fence = 0 }
        {
            sub(/\r$/, "")
            line = $0
            if (line ~ /^[ \t]*```/) { if (open) unclosed(); fence = !fence; next }
            if (fence) next
            if (line ~ /^[ \t]*$/) { if (open) unclosed(); next }
            if (open) { sub(/^[ \t]+/, "", line); text = pending " "; at = start }
            # Parcours des backticks : chaque paire délimite un span.
            while (1) {
                p = index(line, "`")
                if (!open) {
                    if (p == 0) break
                    line = substr(line, p + 1); open = 1; text = ""; at = FNR
                } else if (p == 0) {
                    pending = text line; start = at; break
                } else {
                    span(text substr(line, 1, p - 1), at)
                    line = substr(line, p + 1); open = 0
                }
            }
            if (!open) pending = ""
        }
        END { if (open) unclosed() }
    ' "$doc"
done > "$refs"

total=0
bad=0
while IFS=$'\t' read -r kind doc line class name; do
    case "$kind" in
        M) bad=$((bad + 1)); printf '%s:%s : forme non reconnue › %s\n' "$doc" "$line" "$class"; continue ;;
        U) bad=$((bad + 1)); printf '%s:%s : span non refermé › %s\n' "$doc" "$line" "$class"; continue ;;
    esac
    total=$((total + 1))
    exact=1
    case "$name" in
        *…) name="${name%…}"; exact=0 ;;
        *...) name="${name%...}"; exact=0 ;;
        *\*) name="${name%\*}"; exact=0 ;;
    esac
    name="${name%"${name##*[! ]}"}"   # espaces de fin
    if [ -z "$name" ]; then
        bad=$((bad + 1)); printf '%s:%s : nom vide (%s)\n' "$doc" "$line" "$class"; continue
    fi
    outer="${class%%.*}"
    inner=""
    [ "$outer" != "$class" ] && inner="${class#*.}"
    if [ "$exact" = 1 ]; then b='`'; q='"'; else b=''; q=''; fi
    found=0
    reason="classe introuvable"
    files="$(grep -E "/${outer}\.kt\$" "$tests" || true)"
    if [ -n "$files" ]; then
        while IFS= read -r file; do
            if [ -n "$inner" ] && ! grep -qE "class ${inner}([^A-Za-z0-9_]|\$)" "$file"; then
                reason="classe imbriquée introuvable"; continue
            fi
            reason="test introuvable"
            if grep -qF -e "fun \`$name$b" -e "name = \"$name$q" -e "argumentSet(\"$name$q" \
                -e "@DisplayName(\"$name$q" "$file"; then
                found=1; break
            fi
        done <<< "$files"
    fi
    if [ "$found" = 0 ]; then
        bad=$((bad + 1))
        printf '%s:%s : %s (%s) › %s\n' "$doc" "$line" "$reason" "$class" "$name"
    fi
done < "$refs"

if [ "$bad" -gt 0 ]; then
    echo "$bad référence(s) introuvable(s) ou mal formée(s) ; $total référence(s) bien formée(s) lues."
    exit 1
fi
if [ "$total" = 0 ]; then
    echo "Aucune référence trouvée dans docs/ : vérifier le dossier ou le format des références."
    exit 1
fi
echo "$total références vérifiées, toutes trouvées."
