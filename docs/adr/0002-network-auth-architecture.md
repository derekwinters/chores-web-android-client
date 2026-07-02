# Network and auth architecture for the chores-web API client

The app talks to a self-hosted chores-web backend (no fixed public URL, see docker-compose.yml in that repo), so the server address is user-entered at login rather than baked in at build time. We use Retrofit + OkHttp + kotlinx.serialization for the API layer, with Hilt for DI (introduced now, at the same time as the first ViewModels/repositories, rather than deferred). The auth token (365-day JWT) is persisted in EncryptedSharedPreferences. Since Retrofit needs a baseUrl at construction but the server address can change per-install, an OkHttp interceptor rewrites each request's scheme/host/port from the stored preference rather than rebuilding the Retrofit instance. A second OkHttp interceptor/authenticator globally clears the stored token and routes to the login screen on any 401, matching the web client's behavior (frontend/src/api/client.js in chores-web).

## Considered Options

- **DI**: manual `AppContainer` (matches the "no framework yet" posture from issue #2) vs. Hilt. Chose Hilt since this issue introduces the first real dependency graph (ViewModels, repositories, OkHttp/Retrofit singletons) and manual wiring would likely be replaced by Hilt soon after anyway.
- **JSON**: kotlinx.serialization vs. Moshi. Chose kotlinx.serialization for no-reflection, Kotlin-first fit with the rest of the stack.
- **Base URL**: build-time `BuildConfig` constant per product flavor vs. runtime user input. Rejected build-time constant — this is a self-hosted app with no single known deployment target, so the address must be user-entered and persisted, not compiled in.
- **Base URL wiring**: interceptor-based rewrite (chosen) vs. rebuilding the Hilt-provided Retrofit singleton when the URL changes. Chose the interceptor to keep Retrofit/OkHttp as simple long-lived Hilt singletons with no invalidation logic.

## Consequences

- `AndroidManifest.xml` sets `android:usesCleartextTraffic="true"`: self-hosted chores-web deployments are frequently reached over plain HTTP on a LAN (see docker-compose.yml in chores-web), with no fixed domain to scope a network security config to. This is revisited if/when self-hosted HTTPS becomes the recommended deployment.
- "Navigate to Login" on a global 401 is implemented reactively rather than as an imperative call from the interceptor: `UnauthorizedInterceptor` (running on an OkHttp background thread) only clears the token via `SessionManager.clearToken()`; the root composable (`ChoresRoot`) observes `SessionManager.authState` as a `StateFlow` and swaps to the Login screen when it flips to `LOGGED_OUT`. This avoids threading a `NavController` (a Compose-scoped, main-thread object) into the network layer.
- The backend mounts its versioned API under a `/v1` prefix (`backend/app/main.py` in chores-web: `V1_PREFIX = "/v1"`); Retrofit's placeholder base URL and endpoint paths are written against `/v1/...` accordingly (e.g. `POST /v1/auth/login`, `GET /v1/chores`).
- The Completer-picker dialog's people list comes from `GET /v1/people` (chores-web's `backend/app/routers/people.py`), the same endpoint chores-web's frontend uses to populate `CompleteWithActorModal.jsx`.
