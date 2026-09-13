/* global SwaggerUIBundle, SwaggerUIStandalonePreset */
"use strict";

// Resolve from this page so context paths work and tokens stay on the API's origin.
const bookstoreBase = new URL("../", window.location.href);

async function bookstoreRequestInterceptor(request) {
    const target = new URL(request.url, window.location.href);
    const method = (request.method || "GET").toUpperCase();
    if (target.origin !== bookstoreBase.origin || !target.pathname.startsWith(bookstoreBase.pathname)) {
        throw new Error("Use the Bookstore API on the same origin as this documentation page.");
    }
    request.credentials = "same-origin";
    if (["POST", "PUT", "PATCH", "DELETE"].includes(method)
            && target.pathname.startsWith(new URL("api/v1/", bookstoreBase).pathname)) {
        // A fresh fetch also handles token invalidation after successful login/logout.
        const response = await fetch(new URL("api/v1/authentication/csrf", bookstoreBase), {
            credentials: "same-origin",
            cache: "no-store",
            headers: { Accept: "application/json" }
        });
        if (!response.ok) throw new Error("Could not obtain a CSRF token. Check that the API is running.");
        const csrf = await response.json();
        if (csrf.headerName !== "X-CSRF-TOKEN" || typeof csrf.token !== "string" || !csrf.token) {
            throw new Error("The API returned an invalid CSRF response.");
        }
        request.headers = request.headers || {};
        request.headers[csrf.headerName] = csrf.token;
    }
    return request;
}

window.ui = SwaggerUIBundle({
    url: new URL("v3/api-docs", bookstoreBase).href,
    dom_id: "#swagger-ui",
    deepLinking: true,
    displayRequestDuration: true,
    filter: true,
    tagsSorter: "alpha",
    operationsSorter: "method",
    validatorUrl: null,
    persistAuthorization: false,
    requestInterceptor: bookstoreRequestInterceptor,
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    layout: "StandaloneLayout"
});
