import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Employee } from './employee';

@Injectable({providedIn: 'root'})
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${this.contextPath()}/api/employees`;

  getEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(this.apiUrl);
  }

  createEmployee(employee: Employee): Observable<void> {
    return this.http.post<void>(this.apiUrl, employee);
  }

  updateEmployee(employee: Employee): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${employee.employeeId}`, employee);
  }

  deleteEmployee(employeeId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${employeeId}`);
  }

  private contextPath(): string {
    const appMarker = '/app';
    const markerIndex = window.location.pathname.indexOf(appMarker);
    return markerIndex >= 0 ? window.location.pathname.substring(0, markerIndex) : '';
  }
}
