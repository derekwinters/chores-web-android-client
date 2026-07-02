# Network and auth architecture for the chores-web API client

The app talks to a self-hosted chores-web backend (no fixed public URL, see docker-compose.yml in that repo), so the server address is user-entered at login rather than baked in at build time. We use Retrofit + OkHttp + kotlinx.serialization for the API layer, with Hilt for DI (introduced now, at the same time as the first ViewModels/repositories, rather than deferred). The auth token (365-day JWT) is persisted in EncryptedSharedPreferences. Since Retrofit needs a baseUrl at construction but the server address can change per-install, an OkHttp interceptor rewrites each request's scheme/host/port from the stored preference rather than rebuilding the Retrofit instance. A second OkHttp interceptor/authenticator globally clears the stored token and routes to the login screen on any 401, matching the web client's behavior (frontend/src/api/client.js in chores-web).

## Considered Options

- **DI**: manual `AppContainer` (matches the "no framework yet" posture from issue #2) vs. Hilt. Chose Hilt since this issue introduces the first real dependency graph (ViewModels, repositories, OkHttp/Retrofit singletons) and manual wiring would likely be replaced by Hilt soon after anyway.
- **JSON**: kotlinx.serialization vs. Moshi. Chose kotlinx.serialization for no-reflection, Kotlin-first fit with the rest of the stack.
- **Base URL**: build-time `BuildConfig` constant per product flavor vs. runtime user input. Rejected build-time constant — this is a self-hosted app with no single known deployment target, so the address must be user-entered and persisted, not compiled in.
- **Base URL wiring**: interceptor-based rewrite (chosen) vs. rebuilding the Hilt-provided Retrofit singleton when the URL changes. Chose the interceptor to keep Retrofit/OkHttp as simple long-lived Hilt singletons with no invalidation logic.
