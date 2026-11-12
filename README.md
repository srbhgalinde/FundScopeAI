# FundScope AI — Android prototype

India-first, offline-first mutual fund research prototype.

## What is included
- Native Kotlin + Jetpack Compose Android app.
- Discover screen with sample Indian mutual fund profiles.
- Fund detail screen with a nine-factor scorecard and explanatory annotations.
- Risk-meter visual component (explicitly labelled as a prototype estimate, not official SEBI data).
- SIP future-value calculator with annual step-up support.
- Methodology and privacy screens.
- Room database schema starter for local fund metadata.
- No Android `INTERNET` permission. The prototype cannot call a cloud API or transmit user data.

## Important limitations
This is a UI and calculation prototype, not a live investment research product:
- Fund names are illustrative sample records and the scores/returns/expense ratios are not verified current data.
- No live AMFI/AMC NAV or portfolio connector is implemented.
- No official SEBI riskometer feed is implemented.
- SIP calculator is a hypothetical future-value projection, not historical NAV backtesting or XIRR.
- On-device LLM runtime is not yet bundled. Explanations are deterministic local educational text.
- Room schema is supplied as a persistence foundation; sample UI data is currently bundled in code.
- Global fund coverage is intentionally deferred until data licensing and provider contracts are resolved.

Do not use sample scores or sample metrics to make investment decisions.

## Build
1. Install Android Studio (Ladybug or newer) with Android SDK Platform 35.
2. Open this folder in Android Studio.
3. Allow Gradle sync to download the declared Android dependencies.
4. Select the `app` run configuration and run on an Android 10+ device or emulator.
5. To create an APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. Debug APK output is normally under `app/build/outputs/apk/debug/app-debug.apk`.

A command-line build can be run after installing Gradle:
`gradle :app:assembleDebug`

## Privacy
The manifest intentionally omits the `INTERNET` permission. The app is offline-only in this prototype. Do not add cloud AI, analytics, advertising or network permissions without an explicit privacy review and user-facing disclosure.

## Next implementation milestones
1. Import AMFI scheme master and daily NAV data with source and timestamp metadata.
2. Implement licensed AMC portfolio holdings and issuer fundamentals ingestion.
3. Add category-aware scoring rules and confidence/data-completeness penalties.
4. Verify and store official AMC/SEBI riskometer disclosures with effective dates.
5. Add historical SIP cash-flow simulation and XIRR using dated NAVs.
6. Integrate a quantized on-device LLM model with no network access.
7. Add Room repositories, encrypted personal records and backup/export controls.
8. Add automated tests and independent validation of every financial formula.
