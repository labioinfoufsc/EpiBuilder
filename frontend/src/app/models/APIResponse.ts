export interface APIResponse<T> {
  success: boolean;
  message: T | string;
  data?: T;
}
