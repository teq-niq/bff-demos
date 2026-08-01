import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { appConfig } from './app/app.config';
import { MySingleton } from './app/my-singleton';
import {
  provideHttpClient,
  withXsrfConfiguration,
  HTTP_INTERCEPTORS
} from '@angular/common/http';
import { BaseUrlInterceptor } from './app/base-url.interceptor';
import { Provider, EnvironmentProviders } from '@angular/core';

import { withInterceptorsFromDi } from '@angular/common/http';

async function providers(): Promise<(Provider | EnvironmentProviders)[]> {
  const providersList: (Provider | EnvironmentProviders)[] = [
    ...(appConfig.providers ?? []),
    provideHttpClient(
      withXsrfConfiguration({
        cookieName: 'XSRF-TOKEN',
        headerName: 'X-XSRF-TOKEN'
      })
    )
  ];

  try {
    const response = await fetch('/assets/serverenv.json');
    if (response.ok) {
      const json = await response.json();
      if (json.serverbaseurl) {
        MySingleton.getInstance().setServerBaseUrl(json.serverbaseurl);
      }

      // Only add interceptor if serverBaseUrl is present
      providersList.push(
        provideHttpClient(withInterceptorsFromDi()),
        {
          provide: HTTP_INTERCEPTORS,
          useClass: BaseUrlInterceptor,
          multi: true
        }
      );
    }
  } catch {
    // file not found or error, do nothing, providersList stays minimal
  }

  return providersList;
}

(async () => {
  const providerList = await providers();

  bootstrapApplication(App, {
    ...appConfig,
    providers: providerList
  }).catch(err => console.error(err));
})();
