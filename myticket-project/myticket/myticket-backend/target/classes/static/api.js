/**
 * Thin wrapper for fetch() calls to the backend API.
 * Handles JWT token injection and standard response formatting.
 */
class ApiClient {
    constructor() {
        this.baseUrl = window.location.origin; // e.g. http://localhost:8080 or same domain
    }

    getToken() {
        return localStorage.getItem('myticket_token');
    }

    getAuthHeaders() {
        const headers = { 'Content-Type': 'application/json' };
        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    }

    async _fetch(endpoint, options = {}) {
        const url = endpoint.startsWith('http') ? endpoint : `${this.baseUrl}${endpoint}`;
        
        try {
            const response = await fetch(url, options);
            
            // Handle unauthorized (session expired)
            if (response.status === 401) {
                localStorage.removeItem('myticket_token');
                localStorage.removeItem('myticket_user');
                alert("Session expired. Please log in again.");
                if (window.location.pathname !== '/login.html') {
                    window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`;
                }
                throw new Error("Unauthorized");
            }
            
            // Fast return for empty responses (e.g., 204 No Content or success void)
            if (response.status === 204 || response.headers.get('content-length') === '0') {
                return null;
            }

            const isJson = response.headers.get('content-type')?.includes('application/json');
            
            if (!response.ok) {
                let errorMsg = `HTTP Error ${response.status}`;
                if (isJson) {
                    const errorObj = await response.json();
                    errorMsg = errorObj.error || errorObj.message || errorMsg;
                } else {
                    const text = await response.text();
                    errorMsg = text || errorMsg;
                }
                throw new Error(errorMsg);
            }

            if (isJson) {
                return await response.json();
            } else {
                return await response.text();
            }

        } catch (error) {
            console.error(`API Error on ${endpoint}:`, error);
            throw error;
        }
    }

    async get(endpoint) {
        return this._fetch(endpoint, {
            method: 'GET',
            headers: this.getAuthHeaders()
        });
    }

    async post(endpoint, body) {
        return this._fetch(endpoint, {
            method: 'POST',
            headers: this.getAuthHeaders(),
            body: body ? JSON.stringify(body) : undefined
        });
    }

    async put(endpoint, body) {
        return this._fetch(endpoint, {
            method: 'PUT',
            headers: this.getAuthHeaders(),
            body: body ? JSON.stringify(body) : undefined
        });
    }

    async delete(endpoint) {
        return this._fetch(endpoint, {
            method: 'DELETE',
            headers: this.getAuthHeaders()
        });
    }

    // Auth utility
    isAuthenticated() {
        return !!this.getToken();
    }
    
    logout() {
        localStorage.removeItem('myticket_token');
        localStorage.removeItem('myticket_user');
        window.location.href = '/';
    }
}

// Global instance
const api = new ApiClient();

// Initialize appMode globally
window.appMode = 'online'; // default
api.get('/api/health').then(health => {
    if (health && health.mode) {
        window.appMode = health.mode;
    }
}).catch(e => console.warn('Health check failed, assuming online mode'));
