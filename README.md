# Un poema al día

Un poema en dominio público cada día, el mismo para la web y para la app.

```
S3 (poemas .md, privado) → Lambda diaria (00:05 Europe/Madrid) → today.json (público)
                                                                   ├─ web (GitHub Pages)
                                                                   └─ app Android
```

## Ramas

| Rama | Contenido |
|---|---|
| `pages` (por defecto) | `web/`: página estática publicada con GitHub Pages. `infra/`: stack de AWS CDK con la Lambda que elige el poema y publica `today.json`. |
| `app` | App Android (Kotlin). Cada tag `v*` genera una APK firmada en GitHub Releases. |

Las dos ramas no se fusionan: solo comparten el formato de `today.json`
(`{date, id, title, author, text}`) y su URL.

## Licencia

El código está bajo licencia MIT. Los poemas son obras en dominio público. Ver [`LICENSE`](LICENSE).
