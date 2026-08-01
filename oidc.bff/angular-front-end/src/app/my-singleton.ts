export class MySingleton {
  private static instance = new MySingleton();
  private serverbaseurl: string = '';

  setServerBaseUrl(url: string) {
    this.serverbaseurl = url;
  }

  getServerBaseUrl(): string {
    return this.serverbaseurl;
  }
  
  
  resolveUrl(relativeUrl: string): string {
    if (this.serverbaseurl && relativeUrl.startsWith('/')) {
      return this.serverbaseurl.replace(/\/$/, '') + relativeUrl;
    }
    return relativeUrl;
  }
  private constructor() {}
  static getInstance() { return MySingleton.instance; }
}
