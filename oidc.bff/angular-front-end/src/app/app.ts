import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { JsonViewerComponent } from './json-viewer/json-viewer.component';

import { MySingleton } from './my-singleton';


@Component({
	selector: 'fe-root',
	imports: [RouterOutlet, JsonViewerComponent],
	templateUrl: './app.html',
	styleUrl: './app.css'
})
export class App {
	
	protected readonly showInaccessible = signal(false);
	
	protected readonly title = signal('loading...');
	protected readonly loggedOn = signal(false);
	protected readonly shortProfileResult = signal<any>(null);  // Holds result/error
	protected readonly roles = signal<string[]>([]);
	
	protected readonly scopes = signal<string[]>([]);
	
	
	protected readonly userResult = signal('');  // Holds result/error
	protected readonly adminResult = signal('');  // Holds result/error
	protected readonly fooResult   = signal('');  // Holds result/error
	protected readonly barResult   = signal('');  // Holds result/error
	
	
	
	protected readonly checkPostResult = signal('');
	constructor(private http: HttpClient) {
		this.loadTitle();
	}
	

	loadTitle() {
		this.http.get('/shortprofile').subscribe({
			next: (res: any) => {
				this.title.set(res.name ?? 'Guest');
				this.loggedOn.set(!!res.loggedIn);
				this.shortProfileResult.set(res);
				this.roles.set(res.roles ?? []);
				this.scopes.set(res.scopes ?? []);
				
			},
			error: (err) => {
				this.shortProfileResult.set({
					unusable: true,
					status: err.status,
					message: err.message
				});
				this.loggedOn.set(false);           // safe default
				//this.title.set('Guest');            // fallback
				this.roles.set([]);           // safe default
			}
		});
	}
	
	loadCsrf()
	{
		this.http.get('/csrf').subscribe(		{
					next: (res: any) => {
						
						this.loadTitle();
					},
					error: (err) => {
						this.shortProfileResult.set({
							unusable: true,
							status: err.status,
							message: err.message+'unable to load CSRF'
						});
						
					}
				});
	}

	signIn() {
		//window.location.href = 'http://localhost:9080/oauth2/authorization/okta';
		window.location.href = MySingleton.getInstance().resolveUrl( '/oauth2/authorization/okta?source=frontend');
	}
	
	logout() {
        //this.signOut();//OR
		
		this.apiSignOutGet();//OR
		
    }

	signOut() {
		window.location.assign(MySingleton.getInstance().resolveUrl( '/logout'));
	}
	
	
	
	apiSignOutGet() {
	  window.location.href = MySingleton.getInstance().resolveUrl( '/apilogout?source=frontend');
	}

	invokeShortProfile() {
		this.http.get('/shortprofile')
			.subscribe({
				next: (res: any) => {
					this.shortProfileResult.set(res);
				},
				error: (err) => {
					this.shortProfileResult.set({ error: err.status });
				}
			});
	}
	
	
	
	invokeCheckPost() {
			  this.http.post('/checkpost', {abc:'def'}).subscribe({
			    next: (res: any) => {
			      this.checkPostResult.set(res.message ?? 'no message found');
			    },
			    error: (err) => {
					this.checkPostResult.set('err.status='+err.status+'err.message='+ err.message);
			    }
			  });
			}
			
			





	invokeUser() {
		this.http.get('/secured/user', { responseType: 'text' })
			.subscribe({
				next: (res) => this.userResult.set(res),
				error: (err) => {
					if (err.status === 403) {
						this.userResult.set('Access denied (403)');
					} else if (err.status === 401 && !this.loggedOn()) {
						this.userResult.set('Not logged on ');
					}
					else {
						this.userResult.set('Error: ' + err.message + " ,status=" + err.status)
					}
				}
			});
	}

	invokeAdmin() {
		this.http.get('/secured/admin', { responseType: 'text' })
			.subscribe({
				next: (res) => this.adminResult.set(res),
				error: (err) => {
					if (err.status === 403) {
						this.adminResult.set('Access denied (403)');
					} else if (err.status === 401  && !this.loggedOn()) {
						this.adminResult.set('Not logged on ');
					}
					else {
						this.adminResult.set('Error: ' + err.message + " ,status=" + err.status)
					}
				}
			});
	}
	
	invokeFoo() {
			this.http.get('/secured/foo', { responseType: 'text' })
				.subscribe({
					next: (res) => this.fooResult.set(res),
					error: (err) => {
						if (err.status === 403) {
							this.fooResult.set('Access denied (403)');
						} else if (err.status === 401 && !this.loggedOn()) {
							this.fooResult.set('Not logged on ');
						}
						else {
							this.fooResult.set('Error: ' + err.message + " ,status=" + err.status)
						}
					}
				});
		}
		
		invokeBar() {
					this.http.get('/secured/bar', { responseType: 'text' })
						.subscribe({
							next: (res) => this.barResult.set(res),
							error: (err) => {
								if (err.status === 403) {
									this.barResult.set('Access denied (403)');
								} else if (err.status === 401 && !this.loggedOn()) {
									this.barResult.set('Not logged on ');
								}
								else {
									this.barResult.set('Error: ' + err.message + " ,status=" + err.status)
								}
							}
						});
				}
	
	


}
