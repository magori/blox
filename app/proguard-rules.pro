# Règles R8 propres à l'application.
# Les clés de navigation sont des classes @Serializable. navigation3-runtime n'embarque aucune
# règle : la restauration de la pile passe par Class.forName(...) puis serializer(), que les
# règles fournies par kotlinx-serialization conservent. À vérifier en build minifié (P5.6).
