# Nyavscrn

Application Android d accessibilite pour ecrans partiellement casses.  
Palette cosmos/nebula/cyan, typographie VT323, 100% offline, compilee via GitHub Actions.

## Build

Pousser sur `main` -> workflow Actions -> APK debug telechargeable.

## Design System

| Token | Fichier | Usage |
|-------|---------|-------|
| Couleurs | `colors.xml` | Cosmos, Nebula, Cyan, Star, Void |
| Espacements | `dimens.xml` | Grille 4dp |
| Typographie | `styles.xml` | VT323 sur tout l UI |
| Theme | `themes.xml` | Material3 sombre |

## Conventions

- Packages : `com.nyavo.nrscreen.<feature>`
- Activites : `<Feature>Activity.kt`
- Layouts : `activity_<feature>.xml`
