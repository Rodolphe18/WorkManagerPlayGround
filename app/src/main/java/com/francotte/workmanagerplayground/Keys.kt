package com.francotte.workmanagerplayground


object Keys {
    const val KEY_IMAGE_URL = "image_url"
    const val KEY_DOWNLOADED_PATH = "downloaded_path" // absolute path in internal storage
    const val KEY_FILTERED_PATH = "filtered_path"
}

enum class UiStatus(val label: String) {
    Idle("En attente de partage…"),
    Downloading("Chargement en cours…"),
    Downloaded("Chargement réussi"),
    Filtering("Filtre en cours d'application…"),
    Filtered("Filtre appliqué"),
    Error("Erreur")
}
