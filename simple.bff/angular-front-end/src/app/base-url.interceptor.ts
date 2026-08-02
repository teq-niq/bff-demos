import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MySingleton } from './my-singleton';


@Injectable()
export class BaseUrlInterceptor implements HttpInterceptor {
	private _serverBaseUrl: string = '';
  constructor() {
	this._serverBaseUrl = MySingleton.getInstance().getServerBaseUrl();
	
  }
  

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const baseUrl = this._serverBaseUrl;
	
    // Only modify relative URLs if serverBaseUrl is set
    if (baseUrl && req.url.startsWith('/')) {
      const cloned = req.clone({
        url: baseUrl.replace(/\/$/, '') + req.url,
        withCredentials: true
      });
      return next.handle(cloned);
    }

    // Leave everything else untouched
    return next.handle(req);
  }
}
