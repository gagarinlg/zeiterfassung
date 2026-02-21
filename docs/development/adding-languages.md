# Adding a New Language

Zeiterfassung supports multiple languages. The primary language is German (`de`) and the secondary language is English (`en`). Follow this guide to add a new language.

## Frontend (React + react-i18next)

1. **Create translation file:**
   ```bash
   cp frontend/src/locales/en/translation.json frontend/src/locales/<lang>/translation.json
   ```

2. **Translate all strings** in the new `translation.json` file.

3. **Register the language** in `frontend/src/i18n.ts`:
   ```typescript
   import fr from './locales/fr/translation.json'  // Add import
   
   i18n.use(initReactI18next).init({
     resources: {
       de: { translation: de },
       en: { translation: en },
       fr: { translation: fr },  // Add new language
     },
     // ...
   })
   ```

4. **Add language switcher** (optional): Add the language code to the switcher component.

## Backend (Spring MessageSource)

1. **Create messages file:**
   ```bash
   cp backend/src/main/resources/messages_en.properties \
      backend/src/main/resources/messages_<lang>.properties
   ```

2. **Translate all strings** in the new properties file.

3. Spring MessageSource automatically detects new `messages_<lang>.properties` files based on the request's `Accept-Language` header.

## Android (string resources)

1. **Create values directory:**
   ```bash
   mkdir -p mobile/android/app/src/main/res/values-<lang>
   cp mobile/android/app/src/main/res/values/strings.xml \
      mobile/android/app/src/main/res/values-<lang>/strings.xml
   ```

2. **Translate all strings** in the new `strings.xml` file.

3. Android automatically uses the correct language based on device locale.

## iOS (Localizable.strings)

1. **Create lproj directory:**
   ```bash
   mkdir -p mobile/ios/ZeiterfassungApp/Resources/<lang>.lproj
   cp mobile/ios/ZeiterfassungApp/Resources/en.lproj/Localizable.strings \
      mobile/ios/ZeiterfassungApp/Resources/<lang>.lproj/Localizable.strings
   ```

2. **Translate all strings** in the new `Localizable.strings` file.

3. **Add language in Xcode:** Open the project in Xcode → Project settings → Info → Localizations → Add language.

## Terminal (Fluent .ftl files)

1. **Create locale directory:**
   ```bash
   mkdir -p terminal/locales/<lang>
   cp terminal/locales/en/main.ftl terminal/locales/<lang>/main.ftl
   ```

2. **Translate all strings** in the new `main.ftl` file.

3. **Register in config:** Set `language = "<lang>"` in `terminal.toml`.

## Language Code Reference

| Code | Language |
|------|----------|
| `de` | German (Deutsch) — primary |
| `en` | English — secondary |
| `fr` | French (Français) |
| `es` | Spanish (Español) |
| `tr` | Turkish (Türkçe) |
| `pl` | Polish (Polski) |

## Testing Translations

- **Frontend:** Change `lng` in `i18n.ts` to test, or use browser language settings
- **Backend:** Set `Accept-Language: <lang>` header in API requests
- **Android:** Change device language in Settings → General → Language
- **iOS:** Change device language in Settings → General → Language & Region
- **Terminal:** Change `language` in `terminal.toml`

## Translation Template

When adding new translatable strings, always add them to **all existing language files** to avoid missing translations falling back to the key name.
