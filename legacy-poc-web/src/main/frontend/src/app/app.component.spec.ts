import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let http: HttpTestingController;

  beforeEach(() => {
    window.history.replaceState({}, '', '/legacy-poc/app/');
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    fixture = TestBed.createComponent(AppComponent);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('renders employees as soon as the initial API request completes', async () => {
    fixture.detectChanges();

    const sessionRequest = http.expectOne('/legacy-poc/api/session');
    sessionRequest.flush({
      username: 'employee-admin',
      roles: ['ROLE_EMPLOYEE_ADMIN', 'ROLE_EMPLOYEE_VIEWER'],
      csrfHeaderName: 'X-CSRF-TOKEN',
      csrfToken: 'test-token'
    });

    const request = http.expectOne('/legacy-poc/api/employees');
    request.flush([{
      employeeId: 1,
      firstName: 'John',
      lastName: 'Smith',
      email: 'john.smith@example.com',
      department: 'Engineering',
      createdAt: null
    }]);

    await fixture.whenStable();

    const pageText = fixture.nativeElement.textContent as string;
    expect(pageText).toContain('John Smith');
    expect(pageText).toContain('john.smith@example.com');
    expect(pageText).not.toContain('Loading employees');
  });
});
