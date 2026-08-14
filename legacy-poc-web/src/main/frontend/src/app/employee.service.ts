import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Employee, UserSession } from './employee';

@Injectable({providedIn: 'root'})
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${this.contextPath()}/api/employees`;
  private userSession: UserSession | null = null;

  getSession(): Observable<UserSession> {
    return this.http.get<UserSession>(`${this.contextPath()}/api/session`).pipe(
      tap(session => this.userSession = session)
    );
  }

  getEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(this.apiUrl);
  }

  createEmployee(employee: Employee): Observable<void> {
    return this.http.post<void>(this.apiUrl, employee, {headers: this.csrfHeaders()});
  }

  updateEmployee(employee: Employee): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${employee.employeeId}`, employee, {headers: this.csrfHeaders()});
  }

  deleteEmployee(employeeId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${employeeId}`, {headers: this.csrfHeaders()});
  }

  logout(): Observable<string> {
    return this.http.post(`${this.contextPath()}/logout`, null, {
      headers: this.csrfHeaders(), responseType: 'text'
    });
  }

  loginUrl(): string { return `${this.contextPath()}/login`; }

  private csrfHeaders(): HttpHeaders {
    if (!this.userSession) {
      throw new Error('The authenticated session and CSRF token have not been loaded.');
    }
    return new HttpHeaders().set(this.userSession.csrfHeaderName, this.userSession.csrfToken);
  }

  private contextPath(): string {
    const appMarker = '/app';
    const markerIndex = window.location.pathname.indexOf(appMarker);
    return markerIndex >= 0 ? window.location.pathname.substring(0, markerIndex) : '';
  }
}
