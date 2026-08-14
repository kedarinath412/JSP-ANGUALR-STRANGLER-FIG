import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { EmployeeService } from './employee.service';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let http: HttpTestingController;

  beforeEach(() => {
    window.history.replaceState({}, '', '/legacy-poc/app/');
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    service = TestBed.inject(EmployeeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads employees from the context-relative REST API', () => {
    service.getEmployees().subscribe(employees => expect(employees.length).toBe(1));
    const request = http.expectOne('/legacy-poc/api/employees');
    expect(request.request.method).toBe('GET');
    request.flush([{employeeId: 1, firstName: 'John', lastName: 'Smith', email: 'john@example.com', department: 'Engineering', createdAt: null}]);
  });
});
