export interface Employee {
  employeeId: number | null;
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  createdAt: string | null;
}

export interface ApiError {
  code: string;
  message: string;
  fieldErrors: Record<string, string>;
}
