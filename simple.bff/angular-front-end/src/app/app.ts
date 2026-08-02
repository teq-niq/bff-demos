import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';

import { JsonViewerComponent } from './json-viewer/json-viewer.component';

import { MySingleton } from './my-singleton';
import { FormsModule } from '@angular/forms';


@Component({
	selector: 'fe-root',
	imports: [RouterOutlet, JsonViewerComponent, FormsModule],
	templateUrl: './app.html',
	styleUrl: './app.css'
})
export class App {
	
	protected readonly showInaccessible = signal(false);
	
	protected readonly title = signal('loading...');
	protected readonly loggedOn = signal(false);
	protected readonly shortProfileResult = signal<any>(null);  // Holds result/error
	protected readonly roles = signal<string[]>([]);
	protected readonly showLoginForm = signal(false);
	
	
	
	
	protected readonly userResult = signal('');  // Holds result/error
	protected readonly adminResult = signal('');  // Holds result/error
	username = '';
	password = '';

	
	
	
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
		//window.location.href = MySingleton.getInstance().resolveUrl( '/oauth2/authorization/okta?source=frontend');
		
		// hide sign-in button, show login form
		this.showLoginForm.set(true);
	}
	login() {
	  const body = new HttpParams()
	    .set('username', this.username)
	    .set('password', this.password);

	  this.http.post('/login', body, {
	    headers: {
	      'Content-Type': 'application/x-www-form-urlencoded'
	    }
	  }).subscribe({
	    next: () => {
		  this.loadTitle();
	      this.loggedOn.set(true);
	      this.showLoginForm.set(false);
	      this.password = ''; // clear sensitive data
	    },
	    error: () => {
	      alert('Invalid username or password');
	    }
	  });
	}

	login1() {
	    // placeholder for real auth call
	    this.loggedOn.set(true);
	    this.showLoginForm.set(false);
	  }

	  cancelLogin() {
	    this.showLoginForm.set(false);
	  }
	
	  logout() {
	    this.http.post('/logout', {}).subscribe(() => {
	      this.loadTitle(); // will now return loggedIn=false
	    });
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
			
			invokeCheckPostWithCreds() {
						  this.http.post('/checkpost', {abc:'def'}, { withCredentials: true }).subscribe({
						    next: (res: any) => {
						      this.checkPostResult.set(res.message ?? 'xno message found');
						    },
						    error: (err) => {
								this.checkPostResult.set('withCreds-err.status='+err.status+'err.message='+ err.message);
						    }
						  });
						}		





	invokeUser() {
		this.http.get('/secured/user', { responseType: 'text' })
			.subscribe({
				next: (res) => this.userResult.set(res),
				error: (err) => {
					if (err.status === 401) {
											this.userResult.set('Please login (401)');
										}
					else if (err.status === 403) {
						this.userResult.set('Access denied (403)');
					}/* else if (err.status === 0 && !this.loggedOn()) {
						this.userResult.set('Not logged on ');
					}*/
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
					if (err.status === 401) {
						this.adminResult.set('Please login (401)');
															}
					else if (err.status === 403) {
						this.adminResult.set('Access denied (403)');
					} /*else if (err.status === 0 && !this.loggedOn()) {
						this.adminResult.set('Not logged on ');
					}*/
					else {
						this.adminResult.set('Error: ' + err.message + " ,status=" + err.status)
					}
				}
			});
	}
	
		


}
