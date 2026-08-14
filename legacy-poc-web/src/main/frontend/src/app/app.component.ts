import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Employee, ApiError } from './employee';
import { EmployeeService } from './employee.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  employees: Employee[] = [];
  loading = true;
  saving = false;
  editingId: number | null = null;
  successMessage = '';
  errorMessage = '';
  serverFieldErrors: Record<string, string> = {};
  username = '';
  canEdit = false;

  readonly form = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(200)]],
    department: ['', Validators.maxLength(100)]
  });

  ngOnInit(): void {
    this.employeeService.getSession().subscribe({
      next: session => {
        this.username = session.username;
        this.canEdit = session.roles.includes('ROLE_EMPLOYEE_ADMIN');
        this.loadEmployees();
      },
      error: () => window.location.assign(this.employeeService.loginUrl())
    });
  }

  loadEmployees(): void {
    this.loading = true;
    this.errorMessage = '';
    this.employeeService.getEmployees().subscribe({
      next: employees => {
        this.employees = employees;
        this.loading = false;
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.errorMessage = 'Employees could not be loaded. Verify the WebSphere DataSource.';
        this.loading = false;
        this.changeDetectorRef.markForCheck();
      }
    });
  }

  edit(employee: Employee): void {
    this.editingId = employee.employeeId;
    this.clearMessages();
    this.form.setValue({
      firstName: employee.firstName,
      lastName: employee.lastName,
      email: employee.email,
      department: employee.department || ''
    });
    window.scrollTo({top: 0, behavior: 'smooth'});
  }

  cancelEdit(): void {
    this.editingId = null;
    this.form.reset();
    this.clearMessages();
  }

  save(): void {
    this.clearMessages();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    const value = this.form.getRawValue();
    const employee: Employee = {
      employeeId: this.editingId,
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      email: value.email.trim(),
      department: value.department.trim(),
      createdAt: null
    };
    const operation = this.editingId === null
      ? this.employeeService.createEmployee(employee)
      : this.employeeService.updateEmployee(employee);

    operation.subscribe({
      next: () => {
        this.successMessage = this.editingId === null
          ? 'Employee created successfully.'
          : 'Employee updated successfully.';
        this.editingId = null;
        this.form.reset();
        this.saving = false;
        this.loadEmployees();
      },
      error: error => {
        this.applyApiError(error);
        this.saving = false;
        this.changeDetectorRef.markForCheck();
      }
    });
  }

  remove(employee: Employee): void {
    if (employee.employeeId === null || !window.confirm(`Delete ${employee.firstName} ${employee.lastName}?`)) {
      return;
    }
    this.clearMessages();
    this.employeeService.deleteEmployee(employee.employeeId).subscribe({
      next: () => {
        this.successMessage = 'Employee deleted successfully.';
        this.loadEmployees();
      },
      error: error => {
        this.applyApiError(error);
        this.changeDetectorRef.markForCheck();
      }
    });
  }

  logout(): void {
    this.employeeService.logout().subscribe({
      next: () => window.location.assign(`${this.employeeService.loginUrl()}?logout`),
      error: () => window.location.assign(this.employeeService.loginUrl())
    });
  }

  fieldError(field: 'firstName' | 'lastName' | 'email' | 'department'): string {
    if (this.serverFieldErrors[field]) {
      return this.serverFieldErrors[field];
    }
    const control = this.form.controls[field];
    if (!control.touched || !control.errors) {
      return '';
    }
    if (control.errors['required']) {
      return `${this.label(field)} is required`;
    }
    if (control.errors['email']) {
      return 'Enter a valid email address';
    }
    return `${this.label(field)} is too long`;
  }

  private applyApiError(error: HttpErrorResponse): void {
    const response = error.error as ApiError | undefined;
    this.errorMessage = response?.message || 'The request could not be completed.';
    this.serverFieldErrors = response?.fieldErrors || {};
  }

  private clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.serverFieldErrors = {};
  }

  private label(field: string): string {
    return field.replace(/([A-Z])/g, ' $1').replace(/^./, value => value.toUpperCase());
  }
}
