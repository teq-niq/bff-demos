window.onload = function() {
  //<editor-fold desc="Changeable Configuration Block">

  function getCsrfTokenFromCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
  }

  // Initialize Swagger UI
  window.ui = SwaggerUIBundle({
    url: "/v3/api-docs",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
    SwaggerUIBundle.presets.apis
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "BaseLayout",
    requestInterceptor: (req) => {
      const stateChangingMethods = ['POST', 'PUT', 'PATCH', 'DELETE'];
      if (req.method && stateChangingMethods.includes(req.method.toUpperCase())) {
        const csrfToken = getCsrfTokenFromCookie('XSRF-TOKEN'); 
        if (csrfToken) {
          req.headers['X-XSRF-TOKEN'] = csrfToken;
        }
      }
      return req;
    }
  });

  //</editor-fold>
};
